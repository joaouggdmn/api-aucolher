package com.adocao.api.dto;

public record AuthResponseDTO(
        String token,
        String tipo,
        UsuarioResponseDTO usuario
) {
    public AuthResponseDTO(String token, UsuarioResponseDTO usuario) {
        this(token, "Bearer", usuario);
    }
}
