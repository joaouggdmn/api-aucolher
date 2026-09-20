package com.adocao.api.exception;

import com.adocao.api.dto.ErroResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        // A mensagem do primeiro campo inválido já é legível para o usuário final
        // (o frontend exibe `mensagem` direto na tela); a lista completa segue em `detalhes`
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("Verifique os campos enviados");
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", mensagem, detalhes);
    }

    // Rota inexistente (ex: as antigas /login/user e /login/ong) — sem isto,
    // caía no handler genérico abaixo e virava 500
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponseDTO> handleRotaInexistente(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Não encontrado", "Rota não encontrada: /" + ex.getResourcePath(), null);
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
