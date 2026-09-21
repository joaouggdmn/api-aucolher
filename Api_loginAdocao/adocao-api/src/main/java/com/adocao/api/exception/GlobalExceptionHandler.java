package com.adocao.api.exception;

import com.adocao.api.dto.ErroResponseDTO;
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

    /**
     * Falhas do Bean Validation nos DTOs: devolve o motivo de cada campo em
     * `erros` e usa a primeira mensagem como texto principal.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        String mensagem = erros.values().stream().findFirst().orElse("Verifique os campos enviados");

        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", mensagem, erros);
    }

    /** JSON malformado ou corpo ausente. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponseDTO> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", "Corpo da requisição ausente ou malformado", null);
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
