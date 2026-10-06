package com.aucolher.api.animal.entity;

/**
 * Faixa etária usada no filtro da listagem. Não vem do cliente: é calculada
 * a partir da idade sempre que o animal é salvo (ver {@link Animal}).
 */
public enum FaixaEtaria {
    FILHOTE,
    ADULTO,
    IDOSO;

    private static final int MESES_ATE_ADULTO = 12;
    private static final int MESES_ATE_IDOSO = 96; // 8 anos

    /** Mesma regra do deriveAgeGroup do frontend: menos de 1 ano é filhote, a partir de 8 anos é idoso. */
    public static FaixaEtaria de(int idadeValor, UnidadeIdade unidade) {
        int meses = unidade == UnidadeIdade.MESES ? idadeValor : idadeValor * 12;
        if (meses < MESES_ATE_ADULTO) return FILHOTE;
        if (meses < MESES_ATE_IDOSO) return ADULTO;
        return IDOSO;
    }
}
