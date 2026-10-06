package com.aucolher.api.animal.dto;

import com.aucolher.api.user.entity.UserType;
import com.aucolher.api.user.entity.User;
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
        UserType tipoUsuario,
        @JsonProperty("isVerificado") Boolean verificado
) {

    public static DonoAnimalDTO from(User dono) {
        return new DonoAnimalDTO(dono.getId(), dono.getName(), dono.getPhotoUrl(), dono.getUserType(), dono.getVerified());
    }
}
