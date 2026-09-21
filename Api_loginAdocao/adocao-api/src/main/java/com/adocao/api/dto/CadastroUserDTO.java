package com.adocao.api.dto;

import jakarta.validation.constraints.*;

public record CadastroUserDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail em formato inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String senha,

        @Size(max = FotoUrl.TAMANHO_MAXIMO, message = "A foto é grande demais")
        @Pattern(regexp = FotoUrl.FORMATO, message = "Foto em formato inválido")
        String fotoUrl
) {

    public CadastroUserDTO {
        nome = Sanitizador.texto(nome);
        email = Sanitizador.texto(email);
        fotoUrl = Sanitizador.texto(fotoUrl);
    }
}
