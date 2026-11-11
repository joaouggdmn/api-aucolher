package com.aucolher.api.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * POST /api/webhooks/mercadopago?data.id={order}&type=order  (rota pública).
 * 1) valida a assinatura; 2) localiza a doação; 3) consulta a order com o token da ONG;
 * 4) atualiza a doação de forma atômica; 5) responde 200 rápido.
 *
 * Não filtra por "type": a documentação não confirma o valor para orders, e uma order
 * desconhecida já é ignorada. O id vem da URL ou, sem ele, do corpo ({"data":{"id":...}}).
 * O corpo nunca decide nada: a doação só muda depois de consultar a order no Mercado Pago.
 */
@RestController
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final MercadoPagoProperties props;
    private final MercadoPagoWebhookService service;
    private final ObjectMapper mapper;

    public MercadoPagoWebhookController(MercadoPagoProperties props, MercadoPagoWebhookService service,
                                        ObjectMapper mapper) {
        this.props = props;
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/api/webhooks/mercadopago")
    public ResponseEntity<Void> receive(
            @RequestParam(name = "data.id", required = false) String queryId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) String body) {

        if (props.webhookSecret() == null || props.webhookSecret().isBlank()) {
            log.warn("Webhook do Mercado Pago recebido sem MP_WEBHOOK_SECRET configurado");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        // O Mercado Pago assina o data.id da URL; quando ele não vem, a parte "id:" sai do texto assinado.
        if (!MercadoPagoSignatureVerifier.isValid(props.webhookSecret(), signature, requestId, queryId)) {
            log.warn("Webhook do Mercado Pago com assinatura inválida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String orderId = notBlank(queryId) ? queryId : bodyDataId(body);
        if (orderId != null) {
            service.processOrderNotification(orderId);
        }
        return ResponseEntity.ok().build();
    }

    private String bodyDataId(String body) {
        if (!notBlank(body)) {
            return null;
        }
        try {
            JsonNode id = mapper.readTree(body).path("data").path("id");
            return id.isMissingNode() || id.isNull() || id.asText().isBlank() ? null : id.asText();
        } catch (Exception e) {
            log.warn("Webhook do Mercado Pago com corpo ilegível");
            return null;
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
