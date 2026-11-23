package com.aucolher.api.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aucolher.api.support.GeminiClient.GeminiException;
import com.aucolher.api.support.GeminiClient.Turn;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/** Corpo, cabeçalho e leitura da resposta, contra um servidor falso no lugar do Google (GEMINI_BASE_URL). */
class GeminiClientTest {

    private static final String REPLY_OK = """
            {"candidates":[{"content":{"role":"model","parts":[
              {"text":"pensando...","thought":true},{"text":"Para adotar, "},{"text":"use o AUmatch."}]}}]}""";

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private int responseStatus;
    private String responseBody;
    private String lastPath;
    private String lastQuery;
    private String lastBody;
    private String lastApiKey;

    @BeforeEach
    void startFakeGemini() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/", exchange -> {
            lastPath = exchange.getRequestURI().getPath();
            lastQuery = exchange.getRequestURI().getQuery();
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastApiKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
        respond(200, REPLY_OK);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void respond(int status, String body) {
        this.responseStatus = status;
        this.responseBody = body;
    }

    private GeminiClient client(String thinkingLevel) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new GeminiClient(new GeminiProperties("chave-teste", "modelo-teste", baseUrl, thinkingLevel, 256), mapper);
    }

    @Test
    void sendsTheKeyInTheHeaderAndTheConversationInTheBody() throws IOException {
        client("LOW").generate("Você é o assistente.", List.of(
                new Turn("user", "Oi"), new Turn("model", "Olá!"), new Turn("user", "Como adotar?")));

        assertEquals("/v1beta/models/modelo-teste:generateContent", lastPath);
        assertEquals("chave-teste", lastApiKey);
        assertNull(lastQuery, "a chave não pode ir na URL");

        JsonNode body = mapper.readTree(lastBody);
        assertEquals("Você é o assistente.", body.at("/systemInstruction/parts/0/text").asText());
        assertEquals(3, body.get("contents").size());
        assertEquals("user", body.at("/contents/0/role").asText());
        assertEquals("model", body.at("/contents/1/role").asText());
        assertEquals("Como adotar?", body.at("/contents/2/parts/0/text").asText());
        assertEquals(256, body.at("/generationConfig/maxOutputTokens").asInt());
        assertEquals("LOW", body.at("/generationConfig/thinkingConfig/thinkingLevel").asText());
    }

    @Test
    void withoutThinkingLevelTheModelDefaultIsKept() throws IOException {
        client(" ").generate("instrução", List.of(new Turn("user", "Oi")));

        assertFalse(mapper.readTree(lastBody).at("/generationConfig").has("thinkingConfig"));
    }

    @Test
    void joinsTheTextPartsAndSkipsTheThoughts() {
        String reply = client("LOW").generate("instrução", List.of(new Turn("user", "Como adotar?")));

        assertEquals("Para adotar, use o AUmatch.", reply);
    }

    @Test
    void blockedOrEmptyAnswerReturnsNull() {
        respond(200, "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}");
        assertNull(client("LOW").generate("instrução", List.of(new Turn("user", "Oi"))));

        respond(200, "{}");
        assertNull(client("LOW").generate("instrução", List.of(new Turn("user", "Oi"))));
    }

    @Test
    void errorFromGoogleKeepsTheHttpStatus() {
        respond(429, "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}");

        GeminiException ex = assertThrows(GeminiException.class,
                () -> client("LOW").generate("instrução", List.of(new Turn("user", "Oi"))));
        assertEquals(429, ex.getStatus());
    }

    @Test
    void noAnswerFromTheServerBecomesStatusZero() {
        GeminiClient client = client("LOW");
        server.stop(0);

        GeminiException ex = assertThrows(GeminiException.class,
                () -> client.generate("instrução", List.of(new Turn("user", "Oi"))));
        assertEquals(0, ex.getStatus());
        assertTrue(ex.getMessage().contains("0"));
    }
}
