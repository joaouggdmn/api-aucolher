package com.adocao.api.dto;

import com.adocao.api.entity.AuthProvider;
import com.adocao.api.entity.TipoUsuario;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String cnpj,
        TipoUsuario tipoUsuario,
        AuthProvider provider
) {}
