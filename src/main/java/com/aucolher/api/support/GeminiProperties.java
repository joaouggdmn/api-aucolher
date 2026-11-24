package com.aucolher.api.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.gemini.*: a chave, os modelos e o raciocínio do Google Gemini, usados pelo assistente de suporte.
 * Os valores vêm do .env (GEMINI_*), e os padrões ficam num lugar só: o application.properties.
 * Esta classe não escolhe valor nenhum: só trata texto em branco como "não informado".
 * <ul>
 *   <li>Sem chave ou sem modelo, a API sobe normalmente e o assistente responde 503.</li>
 *   <li>baseUrl troca o Google por um servidor falso nos testes.</li>
 *   <li>thinkingLevel limita o "raciocínio" do modelo (os tokens dele contam como saída e deixam a
 *   resposta lenta); vazio = padrão do modelo.</li>
 *   <li>fallbackModel é tentado quando o principal está sobrecarregado ou não responde a tempo;
 *   vazio ou igual ao principal = sem reserva.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        String thinkingLevel,
        Integer maxOutputTokens,
        String fallbackModel) {

    public GeminiProperties {
        apiKey = blankToNull(apiKey);
        model = blankToNull(model);
        baseUrl = blankToNull(baseUrl);
        thinkingLevel = blankToNull(thinkingLevel);
        fallbackModel = blankToNull(fallbackModel);
        if (fallbackModel != null && fallbackModel.equals(model)) {
            fallbackModel = null;
        }
        if (maxOutputTokens != null && maxOutputTokens <= 0) {
            maxOutputTokens = null;
        }
    }

    public boolean configured() {
        return apiKey != null && model != null && baseUrl != null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
