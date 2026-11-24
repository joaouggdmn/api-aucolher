package com.aucolher.api.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.gemini.*: a chave e os modelos do Google Gemini usados pelo assistente de suporte.
 * Sem a chave, a API sobe normalmente e o assistente responde 503.
 * baseUrl troca o Google por um servidor falso nos testes.
 * thinkingLevel limita o "raciocínio" do modelo (os tokens dele contam como saída e deixam a
 * resposta lenta); vazio = padrão do modelo.
 * fallbackModel é tentado quando o modelo principal está sobrecarregado ou não responde a tempo;
 * vazio = sem reserva.
 */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        String thinkingLevel,
        Integer maxOutputTokens,
        String fallbackModel) {

    public static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    /** Modelo leve: responde em 1 a 2 segundos, o que importa num chat de suporte. */
    public static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
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
        if (fallbackModel != null && (fallbackModel.isBlank() || fallbackModel.equals(model))) {
            fallbackModel = null;
        }
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
