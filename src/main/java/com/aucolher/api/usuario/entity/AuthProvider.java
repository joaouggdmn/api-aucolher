package com.aucolher.api.usuario.entity;

/**
 * Origem da autenticação do usuário: cadastro tradicional (LOCAL)
 * ou login social via Google (GOOGLE).
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
