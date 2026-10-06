package com.aucolher.api.animal.dto;

import com.aucolher.api.usuario.entity.TipoUsuario;
import com.aucolher.api.usuario.entity.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Quem anunciou o animal, como aparece no card e na página de detalhes.
 * Só dados públicos: nada de e-mail ou endereço completo.
 * `tipoUsuario` diz se o anúncio é de uma ONG ou de uma pessoa.
 */
public record DonoAnimalDTO(
        Long id,
        String nome,
        String fotoUrl,
        TipoUsuario tipoUsuario,
        @JsonProperty("isVerificado") Boolean verificado
) {

    public static DonoAnimalDTO from(Usuario dono) {
        return new DonoAnimalDTO(dono.getId(), dono.getNome(), dono.getFotoUrl(), dono.getTipoUsuario(), dono.getVerificado());
    }
}
