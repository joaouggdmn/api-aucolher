package com.adocao.api.dto;

import com.adocao.api.entity.AuthProvider;
import com.adocao.api.entity.TipoUsuario;
import com.adocao.api.entity.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Usuário devolvido em todas as respostas de autenticação (login,
 * cadastro e OAuth2) e na edição do perfil. Campos de ONG/endereço vêm
 * nulos (e as listas vazias) quando não se aplicam.
 *
 * Equipe e horários são coleções lazy: monte este DTO dentro de uma
 * transação (open-in-view está desligado).
 */
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        TipoUsuario tipoUsuario,
        AuthProvider provider,
        LocalDateTime dataCriacao, // base do "Membro desde [ano]" no perfil
        String fotoUrl,
        String bio,

        // Perfil de ONG
        String cnpj,
        String emailInstitucional,
        @JsonProperty("isVerificado") Boolean verificado,
        String instagram,
        String twitter,
        String facebook,
        Integer anoFundacao,
        List<MembroEquipeDTO> equipe,
        List<HorarioVisitaDTO> horariosVisita,

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
                usuario.getDataCriacao(),
                usuario.getFotoUrl(),
                usuario.getBio(),
                usuario.getCnpj(),
                usuario.getEmailInstitucional(),
                usuario.getVerificado(),
                usuario.getInstagram(),
                usuario.getTwitter(),
                usuario.getFacebook(),
                usuario.getAnoFundacao(),
                usuario.getEquipe().stream().map(MembroEquipeDTO::from).toList(),
                usuario.getHorariosVisita().stream().map(HorarioVisitaDTO::from).toList(),
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
