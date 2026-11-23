package com.aucolher.api.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Não loga a chave nem o texto das mensagens: só o código HTTP quando dá erro.
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
    }

    private final RestClient rest;
    private final GeminiProperties props;
    private final ObjectMapper mapper;

    public GeminiClient(GeminiProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.rest = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(factory).build();
    }

    /**
     * Manda a instrução de sistema e a conversa e devolve o texto da resposta,
     * ou null se o Gemini não devolveu texto (resposta bloqueada ou cortada).
     */
    public String generate(String systemInstruction, List<Turn> turns) {
        Map<String, Object> body = buildBody(systemInstruction, turns);
        try {
            String json = rest.post()
                    .uri("/v1beta/models/{model}:generateContent", props.model())
                    .header("x-goog-api-key", props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractText(json == null ? null : mapper.readTree(json));
        } catch (RestClientResponseException e) {
            log.warn("Gemini respondeu HTTP {}", e.getStatusCode().value());
            throw new GeminiException(e.getStatusCode().value());
        } catch (RestClientException | JsonProcessingException e) {
            log.warn("Falha ao chamar o Gemini: {}", e.getClass().getSimpleName());
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
