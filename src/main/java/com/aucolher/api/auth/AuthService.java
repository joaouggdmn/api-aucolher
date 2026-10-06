package com.aucolher.api.auth;

import com.aucolher.api.auth.dto.AuthResponseDTO;
import com.aucolher.api.auth.dto.CadastroOngDTO;
import com.aucolher.api.auth.dto.CadastroUserDTO;
import com.aucolher.api.auth.dto.LoginDTO;
import com.aucolher.api.security.JwtService;
import com.aucolher.api.shared.exception.BusinessException;
import com.aucolher.api.shared.exception.InvalidCredentialsException;
import com.aucolher.api.usuario.UsuarioRepository;
import com.aucolher.api.usuario.dto.UsuarioResponseDTO;
import com.aucolher.api.usuario.entity.AuthProvider;
import com.aucolher.api.usuario.entity.TipoUsuario;
import com.aucolher.api.usuario.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de cadastro e autenticação. Os DTOs chegam aqui já validados
 * (Bean Validation) e normalizados (construtores compactos dos records),
 * então esta classe cuida só das regras de negócio.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDTO registrarOng(CadastroOngDTO dto) {
        garantirEmailDisponivel(dto.email());

        if (usuarioRepository.existsByCnpj(dto.cnpj())) {
            throw new BusinessException("Já existe uma ONG cadastrada com este CNPJ");
        }

        // Fluxo temporário: sem etapa PENDENTE de aprovação (seção 7 das regras
        // de negócio) — a ONG já nasce ativa e sem o selo de verificada
        Usuario usuario = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .cnpj(dto.cnpj())
                .tipoUsuario(TipoUsuario.ONG)
                .provider(AuthProvider.LOCAL)
                .ativo(true)
                .fotoUrl(dto.fotoUrl())
                .bio(dto.bio())
                .emailInstitucional(dto.emailInstitucional())
                .instagram(dto.instagram())
                .twitter(dto.twitter())
                .facebook(dto.facebook())
                .anoFundacao(dto.anoFundacao())
                .cep(dto.cep())
                .logradouro(dto.logradouro())
                .numero(dto.numero())
                .complemento(dto.complemento())
                .bairro(dto.bairro())
                .cidade(dto.cidade())
                .estado(dto.estado())
                .build();

        return gerarResposta(usuarioRepository.save(usuario));
    }

    @Transactional
    public AuthResponseDTO registrarUsuarioComum(CadastroUserDTO dto) {
        garantirEmailDisponivel(dto.email());

        Usuario usuario = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .tipoUsuario(TipoUsuario.USUARIO_COMUM)
                .provider(AuthProvider.LOCAL)
                .ativo(true)
                .fotoUrl(dto.fotoUrl())
                .build();

        return gerarResposta(usuarioRepository.save(usuario));
    }

    /**
     * Login único para qualquer tipo de conta: valida só e-mail e senha.
     * O papel (tipoUsuario) vai no payload da resposta, e é o frontend que
     * decide o que mostrar a partir dele.
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginDTO dto) {
        return gerarResposta(autenticar(dto));
    }

    private void garantirEmailDisponivel(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail");
        }
    }

    private Usuario autenticar(LoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new InvalidCredentialsException("E-mail ou senha inválidos"));

        if (usuario.getProvider() != AuthProvider.LOCAL || usuario.getSenha() == null) {
            throw new InvalidCredentialsException("Esta conta utiliza login via Google. Use a autenticação OAuth2");
        }

        if (!usuario.getAtivo()) {
            throw new InvalidCredentialsException("Usuário inativo");
        }

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }

        return usuario;
    }

    private AuthResponseDTO gerarResposta(Usuario usuario) {
        String token = jwtService.gerarToken(usuario.getEmail());
        return new AuthResponseDTO(token, UsuarioResponseDTO.from(usuario));
    }
}
