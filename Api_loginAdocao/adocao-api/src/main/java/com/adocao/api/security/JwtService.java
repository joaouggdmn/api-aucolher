package com.adocao.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Geração e validação dos tokens JWT usados pelos logins (tradicional e OAuth2).
 *
 * O token carrega apenas o e-mail (subject) e a validade. Papel e id não entram
 * como claims: o token é assinado, mas não é revalidado contra o banco a cada
 * uso, então um papel copiado para dentro dele ficaria desatualizado depois de
 * qualquer mudança na conta. Quem responde por isso é o CustomUserDetailsService,
 * que relê o usuário do banco em cada requisição autenticada.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** HS256 exige pelo menos 256 bits de chave. */
    private static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    private final SecretKey chave;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.chave = construirChave(secret);
        this.expirationMs = expirationMs;
    }

    private static SecretKey construirChave(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET não definido: gerando uma chave aleatória válida só para esta execução. " +
                    "Os tokens emitidos param de valer quando a API reinicia — defina JWT_SECRET no ambiente.");
            return Jwts.SIG.HS256.key().build();
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < TAMANHO_MINIMO_DO_SEGREDO) {
            throw new IllegalStateException(
                    "app.jwt.secret precisa ter no mínimo " + TAMANHO_MINIMO_DO_SEGREDO + " caracteres");
        }

        return Keys.hmacShaKeyFor(bytes);
    }

    public String gerarToken(String email) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean tokenValido(String token, String email) {
        String emailToken = extrairEmail(token);
        return emailToken.equals(email) && !tokenExpirado(token);
    }

    private boolean tokenExpirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extrairTodasClaims(token);
        return resolver.apply(claims);
    }

    private Claims extrairTodasClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
