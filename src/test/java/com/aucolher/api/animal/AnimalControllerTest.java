package com.aucolher.api.animal;

import com.aucolher.api.config.SecurityConfig;
import com.aucolher.api.security.CustomOAuth2UserService;
import com.aucolher.api.security.CustomUserDetailsService;
import com.aucolher.api.security.JwtService;
import com.aucolher.api.security.OAuth2AuthenticationSuccessHandler;
import com.aucolher.api.shared.dto.PaginaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Camada HTTP das rotas de animais: quais são públicas, quais exigem login e
 * como a validação do corpo responde. A Service é simulada — as regras de
 * negócio estão em AnimalServiceTest.
 */
@WebMvcTest(AnimalController.class)
@Import(SecurityConfig.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnimalService animalService;

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
    void listagemEPublica() throws Exception {
        when(animalService.listarDisponiveis(any(), anyInt(), anyInt()))
                .thenReturn(new PaginaDTO<>(List.of(), 0, 12, 0, 0));

        mockMvc.perform(get("/api/animais").param("especie", "GATO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void animaisDeUmPerfilSaoPublicos() throws Exception {
        mockMvc.perform(get("/api/usuarios/1/animais")).andExpect(status().isOk());
    }

    @Test
    void filtroComValorForaDaListaDa400() throws Exception {
        mockMvc.perform(get("/api/animais").param("especie", "PASSARO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.especie").exists());
    }

    @Test
    void meusAnimaisExigeLogin() throws Exception {
        mockMvc.perform(get("/api/animais/meus")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dono@email.com")
    void meusAnimaisComLogin() throws Exception {
        mockMvc.perform(get("/api/animais/meus")).andExpect(status().isOk());
    }

    @Test
    void cadastroExigeLogin() throws Exception {
        mockMvc.perform(post("/api/animais").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dono@email.com")
    void cadastroIncompletoApontaOsCampos() throws Exception {
        mockMvc.perform(post("/api/animais").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.especie").exists())
                .andExpect(jsonPath("$.erros.fotos").value("Envie de 1 a 4 fotos"));
    }

    @Test
    @WithMockUser(username = "dono@email.com")
    void idadeForaDaFaixaDaUnidadeDa400() throws Exception {
        String corpo = """
                {"nome":"Thor","especie":"CACHORRO","raca":"SRD","sexo":"MACHO",
                 "idadeValor":14,"idadeUnidade":"MESES","porte":"GRANDE",
                 "nivelEnergia":"ALTO","temperamento":"CALMO","nivelIndependencia":"BAIXO","nivelVocalizacao":"BAIXO",
                 "bomComCriancas":true,"bomComCaes":true,"bomComGatos":true,"adaptadoApartamento":true,
                 "resumo":"Resumo","historia":"História","fotos":["https://fotos/1.jpg"]}
                """;

        mockMvc.perform(post("/api/animais").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.idadeValida").exists());
    }

    @Test
    void alterarStatusExigeLogin() throws Exception {
        mockMvc.perform(patch("/api/animais/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ADOTADO\"}"))
                .andExpect(status().isUnauthorized());
    }
}
