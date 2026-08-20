package com.adocao.api.controller;

import com.adocao.api.dto.*;
import com.adocao.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173/")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/ong")
    public ResponseEntity<AuthResponseDTO> cadastrarOng(@Valid @RequestBody CadastroOngDTO dto) {
        AuthResponseDTO resposta = authService.registrarOng(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/register/user")
    public ResponseEntity<AuthResponseDTO> cadastrarUsuario(@Valid @RequestBody CadastroUserDTO dto) {
        AuthResponseDTO resposta = authService.registrarUsuarioComum(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/login/ong")
    public ResponseEntity<AuthResponseDTO> loginOng(@Valid @RequestBody LoginDTO dto) {
        AuthResponseDTO resposta = authService.loginOng(dto);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/login/user")
    public ResponseEntity<AuthResponseDTO> loginUsuario(@Valid @RequestBody LoginDTO dto) {
        AuthResponseDTO resposta = authService.loginUsuarioComum(dto);
        return ResponseEntity.ok(resposta);
    }
}
