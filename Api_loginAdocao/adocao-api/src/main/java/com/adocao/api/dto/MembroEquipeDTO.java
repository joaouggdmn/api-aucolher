package com.adocao.api.dto;

import com.adocao.api.entity.MembroEquipe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Integrante da equipe da ONG — colunas de ong_equipe. Usado na edição do perfil e nas respostas. */
public record MembroEquipeDTO(

        @NotBlank(message = "Informe o nome de cada integrante da equipe")
        @Size(max = 150, message = "O nome do integrante deve ter no máximo 150 caracteres")
        String nome,

        @Size(max = 100, message = "A função do integrante deve ter no máximo 100 caracteres")
        String funcao
) {

    public MembroEquipeDTO {
        nome = Sanitizador.texto(nome);
        funcao = Sanitizador.texto(funcao);
    }

    public static MembroEquipeDTO from(MembroEquipe membro) {
        return new MembroEquipeDTO(membro.getNome(), membro.getFuncao());
    }
}
