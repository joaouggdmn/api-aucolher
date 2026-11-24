package com.aucolher.api.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Chamada ao Google Gemini (generateContent). A chave vai no cabeçalho x-goog-api-key, nunca na URL.
 * Não loga a chave nem o texto das mensagens: só o código HTTP e o modelo quando dá erro.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    /** Uma fala da conversa. role é "user" (a pessoa) ou "model" (o assistente), como o Gemini espera. */
    public record Turn(String role, String text) {
    }

    /** Falha ao falar com o Gemini. status é o código HTTP da resposta, ou 0 sem resposta (rede, timeout). */
    public static class GeminiException extends RuntimeException {

        private final int status;

        public GeminiException(int status) {
            super("Falha ao chamar o Gemini (HTTP " + status + ")");
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        /**
         * Falha passageira, que vale tentar de novo: sem resposta a tempo (0), limite de uso (429)
         * ou o Google sobrecarregado (5xx, como o 503 "high demand"). 400, 401, 403 e 404 são erro
         * de configuração (chave ou modelo) e não mudam tentando de novo.
         */
        public boolean isTemporary() {
            return status == 0 || status == 429 || status >= 500;
        }
    }

    /** Espera entre as tentativas; trocada nos testes para não deixá-los lentos. */
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    /** Pausa antes da 2ª e da 3ª tentativa. */
    static final long[] BACKOFF_MS = {700, 1500};

    private final RestClient rest;
    private final GeminiProperties props;
    private final ObjectMapper mapper;
    private final Sleeper sleeper;

    @Autowired
    public GeminiClient(GeminiProperties props, ObjectMapper mapper) {
        this(props, mapper, Thread::sleep);
    }

    GeminiClient(GeminiProperties props, ObjectMapper mapper, Sleeper sleeper) {
        this.props = props;
        this.mapper = mapper;
        this.sleeper = sleeper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(20_000);
        this.rest = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(factory).build();
    }

    /**
     * Manda a instrução de sistema e a conversa e devolve o texto da resposta,
     * ou null se o Gemini não devolveu texto (resposta bloqueada ou cortada).
     * <p>
     * Com falha passageira, tenta de novo o modelo principal e, se ainda falhar, o modelo
     * reserva (outro modelo tem outra fila no Google). Sem resposta a tempo, não repete o mesmo
     * modelo: vai direto para o reserva, para a pessoa não esperar duas vezes.
     */
    public String generate(String systemInstruction, List<Turn> turns) {
        Map<String, Object> body = buildBody(systemInstruction, turns);
        List<String> plan = attemptPlan();
        GeminiException last = null;
        String lastModel = null;
        int attempt = 0;
        for (String model : plan) {
            if (last != null && last.getStatus() == 0 && model.equals(lastModel)) {
                continue;
            }
            if (attempt > 0) {
                pause(BACKOFF_MS[Math.min(attempt - 1, BACKOFF_MS.length - 1)]);
            }
            attempt++;
            try {
                return call(model, body);
            } catch (GeminiException e) {
                log.warn("Gemini falhou (HTTP {}, modelo {}, tentativa {})", e.getStatus(), model, attempt);
                if (!e.isTemporary()) {
                    throw e;
                }
                last = e;
                lastModel = model;
            }
        }
        throw last;
    }

    /** Modelo principal duas vezes e, se houver, o reserva. */
    List<String> attemptPlan() {
        List<String> plan = new ArrayList<>(List.of(props.model(), props.model()));
        if (props.fallbackModel() != null) {
            plan.add(props.fallbackModel());
        }
        return plan;
    }

    private String call(String model, Map<String, Object> body) {
        try {
            String json = rest.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractText(json == null ? null : mapper.readTree(json));
        } catch (RestClientResponseException e) {
            throw new GeminiException(e.getStatusCode().value());
        } catch (RestClientException | JsonProcessingException e) {
            throw new GeminiException(0);
        }
    }

    private void pause(long millis) {
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiException(0);
        }
    }

    Map<String, Object> buildBody(String systemInstruction, List<Turn> turns) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));

        List<Map<String, Object>> contents = new ArrayList<>();
        for (Turn turn : turns) {
            contents.add(Map.of("role", turn.role(), "parts", List.of(Map.of("text", turn.text()))));
        }
        body.put("contents", contents);

        Map<String, Object> generation = new LinkedHashMap<>();
        generation.put("maxOutputTokens", props.maxOutputTokens());
        if (props.thinkingLevel() != null) {
            generation.put("thinkingConfig", Map.of("thinkingLevel", props.thinkingLevel()));
        }
        body.put("generationConfig", generation);
        return body;
    }

    /** Junta as partes de texto do primeiro candidato, pulando as de raciocínio ("thought"). */
    static String extractText(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            text.append(part.path("text").asText(""));
        }
        String result = text.toString().trim();
        return result.isEmpty() ? null : result;
    }
}
