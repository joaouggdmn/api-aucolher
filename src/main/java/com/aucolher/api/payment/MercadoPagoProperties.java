package com.aucolher.api.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.mercadopago.*. baseUrl troca o Mercado Pago por um servidor falso nos testes.
 * sandbox=true manda o pagador de teste ("APRO", aprova sozinho): só com credenciais de teste.
 */
@ConfigurationProperties(prefix = "app.mercadopago")
public record MercadoPagoProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String webhookSecret,
        String tokenEncryptionKey,
        boolean testToken,
        String stateSecret,
        String baseUrl,
        boolean sandbox) {

    public static final String DEFAULT_BASE_URL = "https://api.mercadopago.com";

    public MercadoPagoProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
    }

    /** Sem estes valores a API sobe, mas a conexão responde 503. */
    public boolean oauthConfigured() {
        return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri)
                && notBlank(tokenEncryptionKey) && notBlank(stateSecret);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
