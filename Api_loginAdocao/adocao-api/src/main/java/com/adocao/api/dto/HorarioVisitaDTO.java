package com.adocao.api.dto;

import com.adocao.api.entity.HorarioVisita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Faixa de horário de visita da ONG — colunas de ong_horarios_visita. Usado na edição do perfil e nas respostas. */
public record HorarioVisitaDTO(

        @NotBlank(message = "Informe os dias de cada horário de visita")
        @Size(max = 80, message = "Os dias do horário de visita devem ter no máximo 80 caracteres")
        String dias,

        @NotBlank(message = "Informe o horário de cada faixa de visita")
        @Size(max = 80, message = "O horário de visita deve ter no máximo 80 caracteres")
        String horario
) {

    public HorarioVisitaDTO {
        dias = Sanitizador.texto(dias);
        horario = Sanitizador.texto(horario);
    }

    public static HorarioVisitaDTO from(HorarioVisita faixa) {
        return new HorarioVisitaDTO(faixa.getDias(), faixa.getHorario());
    }
}
