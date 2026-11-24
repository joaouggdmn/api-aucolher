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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aucolher.api.support.GeminiClient.GeminiException;
import com.aucolher.api.support.GeminiClient.Turn;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/**
 * Corpo, cabeçalho, leitura da resposta e as novas tentativas, contra um servidor falso no lugar
 * do Google (GEMINI_BASE_URL). As pausas entre tentativas são registradas, não esperadas.
 */
class GeminiClientTest {

    private static final String REPLY_OK = """
            {"candidates":[{"content":{"role":"model","parts":[
              {"text":"pensando...","thought":true},{"text":"Para adotar, "},{"text":"use o AUmatch."}]}}]}""";
    private static final String OVERLOADED = "{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\"}}";
    private static final List<Turn> ONE_QUESTION = List.of(new Turn("user", "Como adotar?"));

    private record Reply(int status, String body) {
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Deque<Reply> queued = new ArrayDeque<>();
    private final List<String> paths = new ArrayList<>();
    private final List<Long> pauses = new ArrayList<>();
    private Reply fallbackReply;
    private HttpServer server;
    private String lastQuery;
    private String lastBody;
    private String lastApiKey;

    @BeforeEach
    void startFakeGemini() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            lastQuery = exchange.getRequestURI().getQuery();
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastApiKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            Reply reply = queued.isEmpty() ? fallbackReply : queued.poll();
            byte[] out = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), out.length);
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

    /** Resposta de todas as chamadas, menos as que estiverem na fila. */
    private void respond(int status, String body) {
        fallbackReply = new Reply(status, body);
    }

    /** Respostas das próximas chamadas, na ordem. */
    private void queue(int status, String body) {
        queued.add(new Reply(status, body));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private GeminiClient client(String thinkingLevel, String fallbackModel) {
        GeminiProperties props = new GeminiProperties("chave-teste", "modelo-leve", baseUrl(), thinkingLevel, 256,
                fallbackModel);
        return new GeminiClient(props, mapper, pauses::add);
    }

    private GeminiClient client() {
        return client("MINIMAL", "modelo-reserva");
    }

    private static final String LEVE = "/v1beta/models/modelo-leve:generateContent";
    private static final String RESERVA = "/v1beta/models/modelo-reserva:generateContent";

    @Test
    void sendsTheKeyInTheHeaderAndTheConversationInTheBody() throws IOException {
        client().generate("Você é o assistente.", List.of(
                new Turn("user", "Oi"), new Turn("model", "Olá!"), new Turn("user", "Como adotar?")));

        assertEquals(List.of(LEVE), paths);
        assertEquals("chave-teste", lastApiKey);
        assertNull(lastQuery, "a chave não pode ir na URL");

        JsonNode body = mapper.readTree(lastBody);
        assertEquals("Você é o assistente.", body.at("/systemInstruction/parts/0/text").asText());
        assertEquals(3, body.get("contents").size());
        assertEquals("user", body.at("/contents/0/role").asText());
        assertEquals("model", body.at("/contents/1/role").asText());
        assertEquals("Como adotar?", body.at("/contents/2/parts/0/text").asText());
        assertEquals(256, body.at("/generationConfig/maxOutputTokens").asInt());
        assertEquals("MINIMAL", body.at("/generationConfig/thinkingConfig/thinkingLevel").asText());
    }

    @Test
    void unsetThinkingLevelAndOutputLimitAreLeftOut() throws IOException {
        client(" ", null).generate("instrução", ONE_QUESTION);
        assertFalse(mapper.readTree(lastBody).at("/generationConfig").has("thinkingConfig"));

        new GeminiClient(new GeminiProperties("chave-teste", "modelo-leve", baseUrl(), null, null, null), mapper,
                pauses::add).generate("instrução", ONE_QUESTION);
        assertFalse(mapper.readTree(lastBody).at("/generationConfig").has("maxOutputTokens"));
    }

    @Test
    void joinsTheTextPartsAndSkipsTheThoughts() {
        assertEquals("Para adotar, use o AUmatch.", client().generate("instrução", ONE_QUESTION));
    }

    @Test
    void blockedOrEmptyAnswerReturnsNull() {
        respond(200, "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}");
        assertNull(client().generate("instrução", ONE_QUESTION));

        respond(200, "{}");
        assertNull(client().generate("instrução", ONE_QUESTION));
    }

    @Test
    void overloadedOnceTriesTheSameModelAgain() {
        queue(503, OVERLOADED);

        assertEquals("Para adotar, use o AUmatch.", client().generate("instrução", ONE_QUESTION));
        assertEquals(List.of(LEVE, LEVE), paths);
        assertEquals(List.of(GeminiClient.BACKOFF_MS[0]), pauses);
    }

    @Test
    void stillOverloadedUsesTheFallbackModel() {
        queue(503, OVERLOADED);
        queue(429, "{\"error\":{\"code\":429}}");

        assertEquals("Para adotar, use o AUmatch.", client().generate("instrução", ONE_QUESTION));
        assertEquals(List.of(LEVE, LEVE, RESERVA), paths);
        assertEquals(List.of(GeminiClient.BACKOFF_MS[0], GeminiClient.BACKOFF_MS[1]), pauses);
    }

    @Test
    void whenEveryAttemptFailsTheLastErrorIsThrown() {
        respond(503, OVERLOADED);

        GeminiException ex = assertThrows(GeminiException.class, () -> client().generate("instrução", ONE_QUESTION));
        assertEquals(503, ex.getStatus());
        assertEquals(List.of(LEVE, LEVE, RESERVA), paths);
    }

    @Test
    void withoutFallbackItStopsAfterTwoAttempts() {
        respond(503, OVERLOADED);

        assertThrows(GeminiException.class, () -> client("MINIMAL", null).generate("instrução", ONE_QUESTION));
        assertEquals(List.of(LEVE, LEVE), paths);
    }

    @Test
    void configurationErrorIsNotRetried() {
        respond(400, "{\"error\":{\"code\":400,\"status\":\"INVALID_ARGUMENT\"}}");

        GeminiException ex = assertThrows(GeminiException.class, () -> client().generate("instrução", ONE_QUESTION));
        assertEquals(400, ex.getStatus());
        assertFalse(ex.isTemporary());
        assertEquals(List.of(LEVE), paths);
        assertTrue(pauses.isEmpty());
    }

    @Test
    void noAnswerSkipsTheSecondTryOfTheSameModel() {
        GeminiClient client = client();
        server.stop(0);

        GeminiException ex = assertThrows(GeminiException.class, () -> client.generate("instrução", ONE_QUESTION));
        assertEquals(0, ex.getStatus());
        assertTrue(ex.isTemporary());
        // modelo leve sem resposta → direto para o reserva: uma pausa só, e não duas
        assertEquals(List.of(GeminiClient.BACKOFF_MS[0]), pauses);
    }

    @Test
    void blankValuesCountAsNotInformed() {
        // os padrões ficam só no application.properties: em branco aqui é "não informado"
        assertNull(new GeminiProperties("k", "m", "u", null, null, "m").fallbackModel(), "reserva igual ao principal");
        GeminiProperties blank = new GeminiProperties("k", " ", "u", " ", 0, " ");
        assertNull(blank.model());
        assertNull(blank.thinkingLevel());
        assertNull(blank.maxOutputTokens());
        assertNull(blank.fallbackModel());
        assertFalse(blank.configured());
        assertTrue(new GeminiProperties("k", "m", "u", null, null, null).configured());
    }
}
