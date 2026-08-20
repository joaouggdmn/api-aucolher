package com.adocao.api.exception;

import com.adocao.api.dto.ErroResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroResponseDTO> handleBusiness(BusinessException ex) {
        return build(HttpStatus.BAD_REQUEST, "Erro de validação", ex.getMessage(), null);
    }

    @ExceptionHandler({CredenciaisInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<ErroResponseDTO> handleCredenciais(RuntimeException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Não autorizado", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos enviados", detalhes);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> handleGeneric(Exception ex) {
        // Essa linha vai imprimir a stack trace completa no seu console
        ex.printStackTrace();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado", null);
    }

    private ResponseEntity<ErroResponseDTO> build(HttpStatus status, String erro, String mensagem, List<String> detalhes) {
        ErroResponseDTO body = new ErroResponseDTO(LocalDateTime.now(), status.value(), erro, mensagem, detalhes);
        return ResponseEntity.status(status).body(body);
    }
}
