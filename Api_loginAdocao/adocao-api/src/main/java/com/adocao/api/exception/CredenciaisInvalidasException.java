package com.adocao.api.exception;

/** Exceção para falhas de autenticação (login/senha incorretos, conta incompatível, etc). */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}
