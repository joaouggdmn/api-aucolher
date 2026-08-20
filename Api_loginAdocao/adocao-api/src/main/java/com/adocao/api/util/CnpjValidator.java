package com.adocao.api.util;

/**
 * Validação de CNPJ: formato + dígitos verificadores (algoritmo oficial da Receita Federal).
 */
public final class CnpjValidator {

    private CnpjValidator() {}

    public static boolean isValid(String cnpj) {
        if (cnpj == null) return false;
        String limpo = cnpj.replaceAll("[^0-9]", "");

        if (limpo.length() != 14) return false;
        if (limpo.chars().distinct().count() == 1) return false; // todos os dígitos iguais

        return digitoVerificador(limpo, 12) && digitoVerificador(limpo, 13);
    }

    private static boolean digitoVerificador(String cnpj, int posicao) {
        int[] pesos = posicao == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        int digitoEsperado = resto < 2 ? 0 : 11 - resto;

        return digitoEsperado == Character.getNumericValue(cnpj.charAt(posicao));
    }

    /** Remove máscara, retornando apenas os 14 dígitos numéricos. */
    public static String limpar(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("[^0-9]", "");
    }
}
