package com.aucolher.api.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.aucolher.api.config.SecurityConfig;
import com.aucolher.api.security.CustomOAuth2UserService;
import com.aucolher.api.security.CustomUserDetailsService;
import com.aucolher.api.security.JwtService;
import com.aucolher.api.security.OAuth2AuthenticationFailureHandler;
import com.aucolher.api.security.OAuth2AuthenticationSuccessHandler;

/** Webhook do Mercado Pago (contrato api-mercado-pago-ongs §5.2): rota pública, protegida pela assinatura. */
@WebMvcTest(MercadoPagoWebhookController.class)
@Import(SecurityConfig.class)
class MercadoPagoWebhookControllerTest {

    static final String SECRET = "segredo-do-webhook";
    static final String TS = "1742505638683";

    @TestConfiguration
    static class Props {
        @Bean
        MercadoPagoProperties mercadoPagoProperties() {
            return new MercadoPagoProperties("id", "secret", "http://x", SECRET, "k", false, "state", null, false);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MercadoPagoWebhookService service;

    // Dependências da configuração de segurança real, importada acima
    @MockBean
    private JwtService jwtService;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private static String signature(String dataId, String requestId) {
        return "ts=" + TS + ",v1=" + MercadoPagoSignatureVerifier.computeSignature(SECRET, dataId, requestId, TS);
    }

    @Test
    void withoutSignatureIs401() throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago").param("data.id", "ORD123").param("type", "order"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).processOrderNotification(any());
    }

    @Test
    void signatureOfAnotherOrderIs401() throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago").param("data.id", "ORD999")
                        .header("x-signature", signature("ORD123", "req-1")).header("x-request-id", "req-1"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).processOrderNotification(any());
    }

    @Test
    void validSignatureProcessesTheOrderWithoutFilteringByType() throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago").param("data.id", "ORD123").param("type", "topic_orders")
                        .header("x-signature", signature("ORD123", "req-1")).header("x-request-id", "req-1"))
                .andExpect(status().isOk());
        verify(service).processOrderNotification("ORD123");
    }

    @Test
    void withoutIdInTheUrlTheIdComesFromTheBody() throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order\",\"data\":{\"id\":\"ORD777\"}}")
                        .header("x-signature", signature(null, "req-1")).header("x-request-id", "req-1"))
                .andExpect(status().isOk());
        verify(service).processOrderNotification("ORD777");
    }
}
