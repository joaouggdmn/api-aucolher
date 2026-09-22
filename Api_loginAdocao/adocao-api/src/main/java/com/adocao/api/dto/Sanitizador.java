package com.adocao.api.dto;

import java.util.Locale;

/**
 * Normalização dos textos recebidos nos cadastros, aplicada nos
 * construtores compactos dos records.
 *
 * Deixa o objeto já limpo antes da validação e antes de chegar na Service:
 * as regras de formato só precisam conhecer o formato canônico (CNPJ e CEP
 * só com dígitos, rede social sem @, link com protocolo, texto vazio como null).
 */
final class Sanitizador {

    private Sanitizador() {}

    /** Remove espaços nas pontas; texto vazio vira null. */
    static String texto(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    /** Nome de usuário de rede social sem o @ inicial. */
    static String semArroba(String valor) {
        String limpo = texto(valor);
        return limpo == null ? null : texto(limpo.replaceFirst("^@+", ""));
    }

    /** Link colado sem protocolo (ex: "facebook.com/ong") vira https://. */
    static String comProtocolo(String url) {
        String limpo = texto(url);
        if (limpo == null) return null;
        return limpo.matches("(?i)^https?://.*") ? limpo : "https://" + limpo;
    }

    /** Mantém só os dígitos — usado para CNPJ e CEP, que chegam com máscara. */
    static String apenasDigitos(String valor) {
        String limpo = texto(valor);
        return limpo == null ? null : texto(limpo.replaceAll("\\D", ""));
    }

    /** Sigla de UF em maiúsculas. */
    static String sigla(String valor) {
        String limpo = texto(valor);
        return limpo == null ? null : limpo.toUpperCase(Locale.ROOT);
    }
}
