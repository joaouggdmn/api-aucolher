package com.adocao.api.controller;

import com.adocao.api.dto.AtualizacaoPerfilDTO;
import com.adocao.api.dto.UsuarioResponseDTO;
import com.adocao.api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * "Minha conta": a conta é a do token (o e-mail vem no JWT), então não há
     * id na rota — ninguém consegue editar o perfil de outra pessoa.
     */
    @PutMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> atualizarPerfil(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                              @Valid @RequestBody AtualizacaoPerfilDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(usuarioLogado.getUsername(), dto));
    }
}
