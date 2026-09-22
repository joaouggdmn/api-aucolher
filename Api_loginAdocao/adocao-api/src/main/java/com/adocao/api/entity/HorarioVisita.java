package com.adocao.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Faixa de horário de visitas da ONG (ex: "Terça a sexta", "14h às 18h"). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioVisita {

    @Column(nullable = false, length = 80)
    private String dias;

    @Column(nullable = false, length = 80)
    private String horario;
}
