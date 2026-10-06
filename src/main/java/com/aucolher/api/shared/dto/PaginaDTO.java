package com.aucolher.api.shared.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Formato padrão das respostas paginadas da API.
 *
 * Devolver o Page do Spring direto expõe um JSON grande, em inglês e que o
 * próprio Spring Data avisa que pode mudar entre versões — este record fixa
 * o contrato com o frontend. `pagina` começa em 0.
 */
public record PaginaDTO<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static <T> PaginaDTO<T> from(Page<T> page) {
        return new PaginaDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
