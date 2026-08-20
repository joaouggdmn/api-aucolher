package com.adocao.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa tanto ONGs quanto Usuários Comuns.
 * O campo `tipoUsuario` define o perfil, e `provider` define a
 * origem da autenticação (cadastro tradicional ou OAuth2/Google).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Hash BCrypt da senha. Fica nulo para usuários criados
     * automaticamente via login OAuth2 (Google), pois esses
     * usuários nunca informam senha própria.
     */
    @Column(length = 255)
    private String senha;

    /**
     * Obrigatório apenas para tipoUsuario = ONG.
     * Armazenado apenas com dígitos (sem máscara).
     */
    @Column(unique = true, length = 18)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private TipoUsuario tipoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void aoPersistir() {
        this.dataCriacao = LocalDateTime.now();
    }
}
