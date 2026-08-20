package com.adocao.api.service;

import com.adocao.api.dto.*;
import com.adocao.api.entity.AuthProvider;
import com.adocao.api.entity.TipoUsuario;
import com.adocao.api.entity.Usuario;
import com.adocao.api.exception.BusinessException;
import com.adocao.api.exception.CredenciaisInvalidasException;
import com.adocao.api.repository.UsuarioRepository;
import com.adocao.api.security.JwtService;
import com.adocao.api.util.CnpjValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDTO registrarOng(CadastroOngDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail");
        }

        String cnpjLimpo = CnpjValidator.limpar(dto.cnpj());

        if (!CnpjValidator.isValid(cnpjLimpo)) {
            throw new BusinessException("CNPJ inválido");
        }

        if (usuarioRepository.existsByCnpj(cnpjLimpo)) {
            throw new BusinessException("Já existe uma ONG cadastrada com este CNPJ");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .cnpj(cnpjLimpo)
                .tipoUsuario(TipoUsuario.ONG)
                .provider(AuthProvider.LOCAL)
                .ativo(true)
                .build();

        usuario = usuarioRepository.save(usuario);

        return gerarResposta(usuario);
    }

    @Transactional
    public AuthResponseDTO registrarUsuarioComum(CadastroUserDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .tipoUsuario(TipoUsuario.USUARIO_COMUM)
                .provider(AuthProvider.LOCAL)
                .ativo(true)
                .build();

        usuario = usuarioRepository.save(usuario);

        return gerarResposta(usuario);
    }

    public AuthResponseDTO loginOng(LoginDTO dto) {
        Usuario usuario = autenticar(dto);

        if (usuario.getTipoUsuario() != TipoUsuario.ONG) {
            throw new CredenciaisInvalidasException("Esta conta não está cadastrada como ONG");
        }

        return gerarResposta(usuario);
    }

    public AuthResponseDTO loginUsuarioComum(LoginDTO dto) {
        Usuario usuario = autenticar(dto);

        if (usuario.getTipoUsuario() != TipoUsuario.USUARIO_COMUM) {
            throw new CredenciaisInvalidasException("Esta conta não está cadastrada como usuário comum");
        }

        return gerarResposta(usuario);
    }

    private Usuario autenticar(LoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha inválidos"));

        if (usuario.getProvider() != AuthProvider.LOCAL || usuario.getSenha() == null) {
            throw new CredenciaisInvalidasException("Esta conta utiliza login via Google. Use a autenticação OAuth2");
        }

        if (!usuario.getAtivo()) {
            throw new CredenciaisInvalidasException("Usuário inativo");
        }

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos");
        }

        return usuario;
    }

    private AuthResponseDTO gerarResposta(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", usuario.getId());
        claims.put("tipoUsuario", usuario.getTipoUsuario().name());

        String token = jwtService.gerarToken(usuario.getEmail(), claims);

        UsuarioResponseDTO usuarioResponse = new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCnpj(),
                usuario.getTipoUsuario(),
                usuario.getProvider()
        );

        return new AuthResponseDTO(token, usuarioResponse);
    }
}
