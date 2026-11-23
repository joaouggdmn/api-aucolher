package com.aucolher.api.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.gemini.*: a chave e o modelo do Google Gemini usados pelo assistente de suporte.
 * Sem a chave, a API sobe normalmente e o assistente responde 503.
 * baseUrl troca o Google por um servidor falso nos testes.
 * thinkingLevel limita o "raciocínio" do modelo (os tokens dele contam como saída); vazio = padrão do modelo.
 */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        String thinkingLevel,
        Integer maxOutputTokens) {

    public static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    public static final String DEFAULT_MODEL = "gemini-3.6-flash";
    public static final int DEFAULT_MAX_OUTPUT_TOKENS = 1024;

    public GeminiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }
        if (maxOutputTokens == null || maxOutputTokens <= 0) {
            maxOutputTokens = DEFAULT_MAX_OUTPUT_TOKENS;
        }
        if (thinkingLevel != null && thinkingLevel.isBlank()) {
            thinkingLevel = null;
        }
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
