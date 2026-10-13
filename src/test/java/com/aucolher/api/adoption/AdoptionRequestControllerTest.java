package com.aucolher.api.adoption;

import com.aucolher.api.config.SecurityConfig;
import com.aucolher.api.security.CustomOAuth2UserService;
import com.aucolher.api.security.CustomUserDetailsService;
import com.aucolher.api.security.JwtService;
import com.aucolher.api.security.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Camada HTTP dos pedidos de adoção: quem pode enviar e como a validação
 * responde. A Service é simulada — as regras estão em AdoptionRequestServiceTest.
 */
@WebMvcTest(AdoptionRequestController.class)
@Import(SecurityConfig.class)
class AdoptionRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdoptionRequestService requestService;

    // Dependências da configuração de segurança real, importada acima
    @MockBean
    private JwtService jwtService;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Test
    void sendingRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/adoption-requests").contentType(MediaType.APPLICATION_JSON).content("{\"animalId\":10}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/adoption-requests/sent")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@email.com", roles = "ADMIN")
    void adminCannotSendRequests() throws Exception {
        mockMvc.perform(post("/api/adoption-requests").contentType(MediaType.APPLICATION_JSON).content("{\"animalId\":10}"))
                .andExpect(status().isForbidden());
        verify(requestService, never()).create(any(), anyLong());
    }

    @Test
    @WithMockUser(username = "person@email.com", roles = "PERSON")
    void personSendsARequest() throws Exception {
        mockMvc.perform(post("/api/adoption-requests").contentType(MediaType.APPLICATION_JSON).content("{\"animalId\":10}"))
                .andExpect(status().isCreated());
        verify(requestService).create("person@email.com", 10L);
    }

    @Test
    @WithMockUser(username = "ngo@email.com", roles = "NGO")
    void ngoSendsARequest() throws Exception {
        mockMvc.perform(post("/api/adoption-requests").contentType(MediaType.APPLICATION_JSON).content("{\"animalId\":10}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "person@email.com", roles = "PERSON")
    void requestWithoutAnimalReturns400() throws Exception {
        mockMvc.perform(post("/api/adoption-requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.animalId").exists());
    }

    @Test
    @WithMockUser(username = "admin@email.com", roles = "ADMIN")
    void listsOnlyNeedLogin() throws Exception {
        mockMvc.perform(get("/api/adoption-requests/received")).andExpect(status().isOk());
    }
}
