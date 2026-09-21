package com.adocao.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de erro da API.
 *
 * `mensagem` é o texto pronto para exibir ao usuário; `erros` traz o detalhe
 * campo a campo (nome do campo do DTO -> motivo) para o frontend destacar os
 * inputs inválidos, e só aparece no JSON quando existe.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponseDTO(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        Map<String, String> erros
) {}
