package com.aucolher.api.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.aucolher.api.common.ApiException;
import com.aucolher.api.common.security.CurrentUserProvider;
import com.aucolher.api.config.SecurityConfig;
import com.aucolher.api.security.CustomOAuth2UserService;
import com.aucolher.api.security.CustomUserDetailsService;
import com.aucolher.api.security.JwtService;
import com.aucolher.api.security.OAuth2AuthenticationFailureHandler;
import com.aucolher.api.security.OAuth2AuthenticationSuccessHandler;
import com.aucolher.api.support.dto.SupportReplyDTO;

/**
 * Camada HTTP do assistente: rota pública, chave do limite (conta ou IP) e formato do erro.
 * A Service é simulada — as regras estão em SupportChatServiceTest.
 */
@WebMvcTest(SupportChatController.class)
@Import(SecurityConfig.class)
class SupportChatControllerTest {

    private static final String BODY = """
            {"message":"Como adotar?","history":[{"role":"user","text":"Oi"},{"role":"model","text":"Olá!"}]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupportChatService service;
    @MockBean
    private CurrentUserProvider currentUser;

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

    @Test
    void visitorCanAskAndIsLimitedByIp() throws Exception {
        when(currentUser.optionalUserId()).thenReturn(Optional.empty());
        when(service.chat(anyString(), any(), any())).thenReturn(new SupportReplyDTO("Use o AUmatch."));

        mockMvc.perform(post("/api/support/chat").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Use o AUmatch."));

        verify(service).chat(eq("ip:127.0.0.1"), eq("Como adotar?"), any());
    }

    @Test
    @WithMockUser(username = "mariana@teste.com", roles = "PERSON")
    void loggedInPersonIsLimitedByAccount() throws Exception {
        when(currentUser.optionalUserId()).thenReturn(Optional.of(8L));
        when(service.chat(anyString(), any(), any())).thenReturn(new SupportReplyDTO("Oi!"));

        mockMvc.perform(post("/api/support/chat").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        verify(service).chat(eq("user:8"), eq("Como adotar?"), any());
    }

    @Test
    void withoutBodyTheServiceDecides() throws Exception {
        when(currentUser.optionalUserId()).thenReturn(Optional.empty());
        when(service.chat(anyString(), isNull(), isNull())).thenThrow(ApiException.badRequest("Escreva uma pergunta."));

        mockMvc.perform(post("/api/support/chat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Escreva uma pergunta."));
    }

    @Test
    void limitAndUnavailableKeepTheErrorFormat() throws Exception {
        when(currentUser.optionalUserId()).thenReturn(Optional.empty());
        when(service.chat(anyString(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.TOO_MANY_REQUESTS, SupportChatService.TOO_MANY))
                .thenThrow(ApiException.unavailable(SupportChatService.UNAVAILABLE));

        mockMvc.perform(post("/api/support/chat").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(SupportChatService.TOO_MANY));

        mockMvc.perform(post("/api/support/chat").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(SupportChatService.UNAVAILABLE));
    }

    @Test
    void onlyPostIsPublic() throws Exception {
        mockMvc.perform(get("/api/support/chat")).andExpect(status().isUnauthorized());
    }
}
