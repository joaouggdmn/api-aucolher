package com.aucolher.api.support.dto;

/** Uma fala anterior da conversa, guardada no navegador: role "user" (a pessoa) ou "model" (o assistente). */
public record SupportMessageDTO(String role, String text) {
}
