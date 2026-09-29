package com.aucolher.api.auth;

import com.aucolher.api.auth.dto.AuthResponseDTO;
import com.aucolher.api.auth.dto.CadastroOngDTO;
import com.aucolher.api.auth.dto.CadastroUserDTO;
import com.aucolher.api.auth.dto.LoginDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
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

    /** Rota única de login — vale para ONG e usuário comum; o tipo vem em usuario.tipoUsuario. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        AuthResponseDTO resposta = authService.login(dto);
        return ResponseEntity.ok(resposta);
    }
}
