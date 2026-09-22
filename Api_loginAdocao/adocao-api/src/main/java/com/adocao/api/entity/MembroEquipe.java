package com.adocao.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Integrante da equipe exibida no perfil público da ONG (ex: "Marina Costa", "Presidente"). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MembroEquipe {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 100)
    private String funcao;
}
