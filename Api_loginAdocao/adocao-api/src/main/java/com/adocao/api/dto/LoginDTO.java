package com.adocao.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDTO(

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail em formato inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String senha
) {

    public LoginDTO {
        // Autocompletar do navegador costuma trazer espaço no fim do e-mail
        email = Sanitizador.texto(email);
    }
}
