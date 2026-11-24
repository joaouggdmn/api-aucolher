package com.aucolher.api.support.dto;

import java.util.List;

/**
 * Pergunta ao assistente. A conversa não fica no banco: o navegador guarda o histórico
 * e manda as últimas falas junto, para o assistente entender o contexto.
 */
public record SupportChatRequestDTO(String message, List<SupportMessageDTO> history) {
}
