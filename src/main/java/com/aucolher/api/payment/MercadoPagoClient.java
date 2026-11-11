package com.aucolher.api.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Chamadas ao Mercado Pago. Nunca loga token, code nem client_secret.
 * Os caminhos dos campos da Orders API e os nomes dos status devem ser conferidos no sandbox
 * (seção 6 do contrato).
 */
@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    public enum OrderPaymentState { PAID, WAITING, EXPIRED, CANCELLED, UNKNOWN }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("user_id") Long userId,
            @JsonProperty("live_mode") Boolean liveMode) {
    }

    public record PixOrder(String orderId, String qrCode, String qrCodeBase64, String ticketUrl) {
    }

    /** Pagador de teste do sandbox: "APRO" aprova o PIX sozinho. */
    static final String SANDBOX_PAYER_EMAIL = "test@testuser.com";
    static final String SANDBOX_PAYER_NAME = "APRO";
    /** O PIX vence junto com a doação (30 minutos). */
    static final String PIX_EXPIRATION = "PT30M";

    private final RestClient rest;
    private final MercadoPagoProperties props;
    private final ObjectMapper mapper;

    public MercadoPagoClient(MercadoPagoProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);
        this.rest = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(factory).build();
    }

    /** Troca o code pelos tokens (redirect_uri idêntica à do início da conexão). */
    public TokenResponse exchangeCode(String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_id", props.clientId());
        body.put("client_secret", props.clientSecret());
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", props.redirectUri());
        body.put("test_token", props.testToken());
        return requestToken(body);
    }

    /** O refresh_token muda a cada renovação: quem chama tem de salvar o novo. */
    public TokenResponse refresh(String refreshToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("grant_type", "refresh_token");
        body.put("client_id", props.clientId());
        body.put("client_secret", props.clientSecret());
        body.put("refresh_token", refreshToken);
        return requestToken(body);
    }

    private TokenResponse requestToken(Map<String, Object> body) {
        try {
            TokenResponse response = rest.post().uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new MercadoPagoApiException(502, "Resposta de token vazia.");
            }
            return response;
        } catch (RestClientResponseException e) {
            log.warn("Mercado Pago /oauth/token respondeu {}", e.getStatusCode().value());
            throw new MercadoPagoApiException(e.getStatusCode().value(), "Mercado Pago recusou a requisição de token.");
        } catch (RestClientException e) {
            log.warn("Falha de rede em /oauth/token: {}", e.getClass().getSimpleName());
            throw new MercadoPagoApiException(0, "Falha ao falar com o Mercado Pago.");
        }
    }

    /**
     * Cria a order PIX com o token da ONG. Sem marketplace_fee: a plataforma não cobra comissão.
     * idempotencyKey é única por doação (UUID): ids de bancos diferentes usando a mesma conta se repetem.
     * Order sem o "copia e cola" é recusada (502): o doador não teria como pagar.
     */
    public PixOrder createPixOrder(String accessToken, String idempotencyKey, String externalReference,
                                   BigDecimal amount, String payerEmail, String payerName) {
        String value = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("amount", value);
        payment.put("payment_method", Map.of("id", "pix", "type", "bank_transfer"));
        payment.put("expiration_time", PIX_EXPIRATION);
        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", props.sandbox() ? SANDBOX_PAYER_EMAIL : payerEmail);
        payer.put("first_name", props.sandbox() ? SANDBOX_PAYER_NAME : firstName(payerName));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "online");
        body.put("processing_mode", "automatic");
        body.put("total_amount", value);
        body.put("external_reference", externalReference);
        body.put("payer", payer);
        body.put("transactions", Map.of("payments", List.of(payment)));
        JsonNode root;
        try {
            root = rest.post().uri("/v1/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            log.warn("Mercado Pago POST /v1/orders respondeu {} {}", e.getStatusCode().value(), errorCodes(e));
            throw new MercadoPagoApiException(e.getStatusCode().value(), "Mercado Pago recusou a criação da order.");
        } catch (RestClientException e) {
            log.warn("Falha de rede em POST /v1/orders: {}", e.getClass().getSimpleName());
            throw new MercadoPagoApiException(0, "Falha ao falar com o Mercado Pago.");
        }
        if (root == null || text(root.path("id")) == null) {
            throw new MercadoPagoApiException(502, "Resposta da order vazia.");
        }
        JsonNode method = root.path("transactions").path("payments").path(0).path("payment_method");
        String qrCode = text(method.path("qr_code"));
        if (qrCode == null || qrCode.isBlank()) {
            log.warn("Order {} criada sem o código PIX", text(root.path("id")));
            throw new MercadoPagoApiException(502, "O Mercado Pago não devolveu o código PIX.");
        }
        return new PixOrder(
                text(root.path("id")),
                qrCode,
                text(method.path("qr_code_base64")),
                text(method.path("ticket_url")));
    }

    public OrderPaymentState getOrderState(String accessToken, String orderId) {
        try {
            JsonNode root = rest.get().uri("/v1/orders/{id}", orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null) {
                return OrderPaymentState.UNKNOWN;
            }
            String status = text(root.path("status"));
            String detail = text(root.path("status_detail"));
            if (status == null) {
                JsonNode payment = root.path("transactions").path("payments").path(0);
                status = text(payment.path("status"));
                detail = text(payment.path("status_detail"));
            }
            return mapOrderState(status, detail);
        } catch (RestClientResponseException e) {
            log.warn("Mercado Pago GET /v1/orders respondeu {} {}", e.getStatusCode().value(), errorCodes(e));
            throw new MercadoPagoApiException(e.getStatusCode().value(), "Mercado Pago recusou a consulta da order.");
        } catch (RestClientException e) {
            log.warn("Falha de rede em GET /v1/orders: {}", e.getClass().getSimpleName());
            throw new MercadoPagoApiException(0, "Falha ao falar com o Mercado Pago.");
        }
    }

    /** processed/accredited = paga; action_required/waiting_transfer = aguardando; expired; canceled. */
    public static OrderPaymentState mapOrderState(String status, String statusDetail) {
        String s = status == null ? "" : status.toLowerCase(Locale.ROOT);
        String d = statusDetail == null ? "" : statusDetail.toLowerCase(Locale.ROOT);
        if (s.equals("processed") || d.equals("accredited")) {
            return OrderPaymentState.PAID;
        }
        if (s.equals("expired") || d.equals("expired")) {
            return OrderPaymentState.EXPIRED;
        }
        if (s.equals("canceled") || s.equals("cancelled") || d.equals("canceled") || d.equals("cancelled")) {
            return OrderPaymentState.CANCELLED;
        }
        if (s.equals("action_required") || s.equals("created") || d.equals("waiting_transfer")
                || d.equals("waiting_payment")) {
            return OrderPaymentState.WAITING;
        }
        return OrderPaymentState.UNKNOWN;
    }

    /** Só os códigos de erro da resposta (ex.: [invalid_total_amount]): o corpo inteiro pode trazer dados do pagador. */
    private String errorCodes(RestClientResponseException e) {
        try {
            JsonNode errors = mapper.readTree(e.getResponseBodyAsString()).path("errors");
            List<String> codes = new ArrayList<>();
            errors.forEach(err -> {
                String code = text(err.path("code"));
                if (code != null) {
                    codes.add(code);
                }
            });
            return codes.toString();
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "Doador";
        }
        return name.trim().split("\\s+")[0];
    }

    private static String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
