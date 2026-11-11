package com.aucolher.api.payment;

import static com.aucolher.api.payment.MercadoPagoClient.OrderPaymentState.CANCELLED;
import static com.aucolher.api.payment.MercadoPagoClient.OrderPaymentState.EXPIRED;
import static com.aucolher.api.payment.MercadoPagoClient.OrderPaymentState.PAID;
import static com.aucolher.api.payment.MercadoPagoClient.OrderPaymentState.UNKNOWN;
import static com.aucolher.api.payment.MercadoPagoClient.OrderPaymentState.WAITING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/** Corpo e cabeçalhos da order PIX, contra um servidor falso no lugar do Mercado Pago (MP_BASE_URL). */
class MercadoPagoClientTest {

    private static final String ORDER_OK = """
            {"id":"ORD123","transactions":{"payments":[{"payment_method":
              {"qr_code":"00020126pix","qr_code_base64":"iVBOR","ticket_url":"https://mp/ticket"}}]}}""";

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private int responseStatus;
    private String responseBody;
    private String lastBody;
    private String lastIdempotencyKey;
    private String lastAuthorization;

    @BeforeEach
    void startFakeMercadoPago() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/orders", exchange -> {
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastIdempotencyKey = exchange.getRequestHeaders().getFirst("X-Idempotency-Key");
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
        respond(201, ORDER_OK);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void respond(int status, String body) {
        responseStatus = status;
        responseBody = body;
    }

    private MercadoPagoClient client(boolean sandbox) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        var props = new MercadoPagoProperties("id", "secret", "http://x", "wh", "k", false, "state", baseUrl, sandbox);
        return new MercadoPagoClient(props, mapper);
    }

    private MercadoPagoClient.PixOrder create(MercadoPagoClient client) {
        return client.createPixOrder("token-da-ong", "chave-uuid", "donation-259",
                new BigDecimal("50"), "maria@email.com", "Maria Souza");
    }

    @Test
    void sandboxOrderUsesTheApprovingTestPayer() throws Exception {
        MercadoPagoClient.PixOrder order = create(client(true));

        JsonNode body = mapper.readTree(lastBody);
        assertEquals("APRO", body.path("payer").path("first_name").asText());
        assertEquals("test@testuser.com", body.path("payer").path("email").asText());
        assertEquals("ORD123", order.orderId());
        assertEquals("00020126pix", order.qrCode());
        assertEquals("iVBOR", order.qrCodeBase64());
        assertEquals("https://mp/ticket", order.ticketUrl());
    }

    @Test
    void realOrderSendsTheDonorTheKeyAndTheThirtyMinuteExpiration() throws Exception {
        create(client(false));

        JsonNode body = mapper.readTree(lastBody);
        assertEquals("Bearer token-da-ong", lastAuthorization);
        assertEquals("chave-uuid", lastIdempotencyKey);
        assertEquals("donation-259", body.path("external_reference").asText());
        assertEquals("50.00", body.path("total_amount").asText());
        assertEquals("maria@email.com", body.path("payer").path("email").asText());
        assertEquals("Maria", body.path("payer").path("first_name").asText());
        JsonNode payment = body.path("transactions").path("payments").path(0);
        assertEquals("PT30M", payment.path("expiration_time").asText());
        assertEquals("pix", payment.path("payment_method").path("id").asText());
    }

    @Test
    void orderWithoutThePixCodeIsRefused() {
        respond(201, "{\"id\":\"ORD123\",\"transactions\":{\"payments\":[{\"payment_method\":{}}]}}");

        MercadoPagoApiException e = assertThrows(MercadoPagoApiException.class, () -> create(client(false)));
        assertEquals(502, e.getHttpStatus());
    }

    @Test
    void refusalKeepsTheHttpStatus() {
        respond(401, "{\"errors\":[{\"code\":\"unauthorized\",\"message\":\"invalid token\"}]}");

        MercadoPagoApiException e = assertThrows(MercadoPagoApiException.class, () -> create(client(false)));
        assertEquals(401, e.getHttpStatus());
    }

    @Test
    void readsTheOrderState() {
        respond(200, "{\"id\":\"ORD123\",\"status\":\"processed\",\"status_detail\":\"accredited\"}");

        assertEquals(PAID, client(false).getOrderState("token-da-ong", "ORD123"));
    }

    @Test
    void mapsOrderStates() {
        assertEquals(PAID, MercadoPagoClient.mapOrderState("processed", "accredited"));
        assertEquals(WAITING, MercadoPagoClient.mapOrderState("action_required", "waiting_transfer"));
        assertEquals(EXPIRED, MercadoPagoClient.mapOrderState("expired", null));
        assertEquals(CANCELLED, MercadoPagoClient.mapOrderState("canceled", null));
        assertEquals(UNKNOWN, MercadoPagoClient.mapOrderState(null, null));
    }
}
