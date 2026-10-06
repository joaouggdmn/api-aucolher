package com.aucolher.api.animal.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FaixaEtariaTest {

    @ParameterizedTest(name = "{0} {1} = {2}")
    @CsvSource({
            "0,  MESES, FILHOTE",
            "11, MESES, FILHOTE",
            "1,  ANOS,  ADULTO",
            "7,  ANOS,  ADULTO",
            "8,  ANOS,  IDOSO",
            "15, ANOS,  IDOSO",
    })
    void calculaAFaixaPelaIdade(int valor, UnidadeIdade unidade, FaixaEtaria esperada) {
        assertThat(FaixaEtaria.de(valor, unidade)).isEqualTo(esperada);
    }
}
