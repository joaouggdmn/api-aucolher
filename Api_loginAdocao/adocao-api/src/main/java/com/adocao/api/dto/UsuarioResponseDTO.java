package com.adocao.api.dto;

import com.adocao.api.entity.AuthProvider;
import com.adocao.api.entity.TipoUsuario;
import com.adocao.api.entity.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Usuário devolvido em todas as respostas de autenticação (login,
 * cadastro e OAuth2). Campos de ONG/endereço vêm nulos quando não se aplicam.
 */
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        TipoUsuario tipoUsuario,
        AuthProvider provider,
        String fotoUrl,
        String bio,

        // Perfil de ONG
        String cnpj,
        String emailInstitucional,
        @JsonProperty("isVerificado") Boolean verificado,
        String instagram,
        String twitter,
        String facebook,

        // Endereço
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado
) {

    public static UsuarioResponseDTO from(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTipoUsuario(),
                usuario.getProvider(),
                usuario.getFotoUrl(),
                usuario.getBio(),
                usuario.getCnpj(),
                usuario.getEmailInstitucional(),
                usuario.getVerificado(),
                usuario.getInstagram(),
                usuario.getTwitter(),
                usuario.getFacebook(),
                usuario.getCep(),
                usuario.getLogradouro(),
                usuario.getNumero(),
                usuario.getComplemento(),
                usuario.getBairro(),
                usuario.getCidade(),
                usuario.getEstado()
        );
    }
}
