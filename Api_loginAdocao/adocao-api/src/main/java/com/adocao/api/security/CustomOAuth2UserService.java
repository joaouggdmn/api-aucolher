package com.adocao.api.security;

import com.adocao.api.entity.AuthProvider;
import com.adocao.api.entity.TipoUsuario;
import com.adocao.api.entity.Usuario;
import com.adocao.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Executado a cada login OAuth2 (Google). Se for o primeiro acesso do
 * e-mail, cria automaticamente um registro em `usuarios` com perfil
 * USUARIO_COMUM, como exigido pelo requisito 3.3.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String nome = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Não foi possível obter o e-mail da conta Google");
        }

        usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario novoUsuario = Usuario.builder()
                    .nome(nome != null ? nome : email)
                    .email(email)
                    .senha(null)
                    .tipoUsuario(TipoUsuario.USUARIO_COMUM)
                    .provider(AuthProvider.GOOGLE)
                    .ativo(true)
                    .build();
            return usuarioRepository.save(novoUsuario);
        });

        return oAuth2User;
    }
}
