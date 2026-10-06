package com.aucolher.api.shared.exception;

/** Usuário autenticado tentando alterar algo que não é dele (ex: o animal de outra conta). Vira 403. */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
