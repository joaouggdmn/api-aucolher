package com.aucolher.api.shared.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroResponseDTO> handleBusiness(BusinessException ex) {
        return build(HttpStatus.BAD_REQUEST, "Erro de validação", ex.getMessage(), null);
    }

    @ExceptionHandler({CredenciaisInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<ErroResponseDTO> handleCredenciais(RuntimeException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Não autorizado", ex.getMessage(), null);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponseDTO> handleAcessoNegado(AcessoNegadoException ex) {
        return build(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponseDTO> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage(), null);
    }

    /**
     * Falhas do Bean Validation nos DTOs: devolve o motivo de cada campo em
     * `erros` e usa a primeira mensagem como texto principal.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.isBindingFailure()
                        // Valor que nem chegou a ser convertido (ex: ?especie=PASSARO) — a mensagem padrão do Spring é técnica
                        ? "Valor inválido para o campo " + erro.getField()
                        : erro.getDefaultMessage()));

        String mensagem = erros.values().stream().findFirst().orElse("Verifique os campos enviados");

        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", mensagem, erros);
    }

    /**
     * JSON malformado ou corpo ausente. Quando o problema é um valor com o
     * tipo errado (ex: "especie": "PASSARO", fora do enum), aponta o campo.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponseDTO> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException formato && !formato.getPath().isEmpty()) {
            String campo = formato.getPath().get(formato.getPath().size() - 1).getFieldName();
            if (campo != null) {
                String mensagem = "Valor inválido para o campo " + campo;
                return build(HttpStatus.BAD_REQUEST, "Dados inválidos", mensagem, Map.of(campo, mensagem));
            }
        }
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", "Corpo da requisição ausente ou malformado", null);
    }

    /** Parâmetro de rota ou de filtro com tipo errado (ex: /api/animais/abc) — sem isto, virava 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponseDTO> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", "Valor inválido para o parâmetro " + ex.getName(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponseDTO> handleMetodo(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado", ex.getMessage(), null);
    }

    /** Rota inexistente — sem isto, caía no handler genérico abaixo e virava 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponseDTO> handleRotaInexistente(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Não encontrado", "Rota não encontrada: /" + ex.getResourcePath(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> handleGeneric(Exception ex) {
        // Detalhe do erro fica no log do servidor; o cliente recebe só a mensagem genérica
        log.error("Erro não tratado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado", null);
    }

    private ResponseEntity<ErroResponseDTO> build(HttpStatus status, String erro, String mensagem,
                                                  Map<String, String> erros) {
        ErroResponseDTO body = new ErroResponseDTO(LocalDateTime.now(), status.value(), erro, mensagem, erros);
        return ResponseEntity.status(status).body(body);
    }
}
