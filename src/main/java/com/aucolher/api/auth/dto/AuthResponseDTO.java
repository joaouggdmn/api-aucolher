package com.aucolher.api.auth.dto;

import com.aucolher.api.usuario.dto.UsuarioResponseDTO;

public record AuthResponseDTO(
        String token,
        String tipo,
        UsuarioResponseDTO usuario
) {
    public AuthResponseDTO(String token, UsuarioResponseDTO usuario) {
        this(token, "Bearer", usuario);
    }
}
