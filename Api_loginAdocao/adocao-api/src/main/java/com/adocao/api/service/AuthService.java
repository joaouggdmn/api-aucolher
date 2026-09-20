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
import java.util.Locale;
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

        // Fluxo temporário: sem etapa PENDENTE de aprovação (seção 7 das regras
        // de negócio) — a ONG já nasce ativa e sem o selo de verificada
        Usuario usuario = Usuario.builder()
                .nome(dto.nome().trim())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .cnpj(cnpjLimpo)
                .tipoUsuario(TipoUsuario.ONG)
                .provider(AuthProvider.LOCAL)
                .ativo(true)
                .fotoUrl(textoOuNulo(dto.fotoUrl()))
                .bio(textoOuNulo(dto.bio()))
                .emailInstitucional(textoOuNulo(dto.emailInstitucional()))
                .instagram(semArroba(dto.instagram()))
                .twitter(semArroba(dto.twitter()))
                .facebook(comProtocolo(dto.facebook()))
                .cep(apenasDigitos(dto.cep()))
                .logradouro(textoOuNulo(dto.logradouro()))
                .numero(textoOuNulo(dto.numero()))
                .complemento(textoOuNulo(dto.complemento()))
                .bairro(textoOuNulo(dto.bairro()))
                .cidade(textoOuNulo(dto.cidade()))
                .estado(dto.estado().trim().toUpperCase(Locale.ROOT))
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
                .nome(dto.nome().trim())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .tipoUsuario(TipoUsuario.USUARIO_COMUM)
                .provider(AuthProvider.LOCAL)
                .ativo(true)
                .fotoUrl(textoOuNulo(dto.fotoUrl()))
                .build();

        usuario = usuarioRepository.save(usuario);

        return gerarResposta(usuario);
    }

    /**
     * Login único para qualquer tipo de conta: valida só e-mail e senha.
     * O papel (tipoUsuario) segue no payload e no JWT, e é o frontend que
     * decide o que mostrar a partir dele.
     */
    public AuthResponseDTO login(LoginDTO dto) {
        return gerarResposta(autenticar(dto));
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

        return new AuthResponseDTO(token, UsuarioResponseDTO.from(usuario));
    }

    // ===================== Normalização dos campos opcionais =====================

    private static String textoOuNulo(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private static String semArroba(String usuarioRedeSocial) {
        String limpo = textoOuNulo(usuarioRedeSocial);
        if (limpo == null) return null;
        return textoOuNulo(limpo.replaceFirst("^@+", ""));
    }

    private static String comProtocolo(String url) {
        String limpo = textoOuNulo(url);
        if (limpo == null) return null;
        return limpo.matches("(?i)^https?://.*") ? limpo : "https://" + limpo;
    }

    private static String apenasDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }
}
