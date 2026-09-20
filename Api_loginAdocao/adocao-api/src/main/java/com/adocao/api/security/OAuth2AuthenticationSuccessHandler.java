package com.adocao.api.security;

import com.adocao.api.dto.AuthResponseDTO;
import com.adocao.api.dto.UsuarioResponseDTO;
import com.adocao.api.entity.Usuario;
import com.adocao.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Após o login Google ser concluído, gera um JWT próprio da API
 * (mesmo formato usado pelo login tradicional) e devolve em JSON,
 * mantendo a API stateless mesmo para o fluxo OAuth2.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário OAuth2 não encontrado após login"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", usuario.getId());
        claims.put("tipoUsuario", usuario.getTipoUsuario().name());

        String token = jwtService.gerarToken(usuario.getEmail(), claims);

        // Mesmo payload do login tradicional (POST /api/auth/login)
        AuthResponseDTO body = new AuthResponseDTO(token, UsuarioResponseDTO.from(usuario));

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
