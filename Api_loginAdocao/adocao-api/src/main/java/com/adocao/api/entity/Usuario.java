package com.adocao.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa tanto ONGs quanto Usuários Comuns.
 * O campo `tipoUsuario` define o perfil, e `provider` define a
 * origem da autenticação (cadastro tradicional ou OAuth2/Google).
 *
 * Campos de perfil seguem a seção 6 de docs/regras-de-negocio.md:
 * bio e foto valem para os dois perfis; CNPJ, e-mail institucional,
 * redes sociais, equipe, horário de visitas e selo são do perfil de ONG.
 * O endereço é obrigatório para ONG e opcional para o usuário comum.
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
    @Column(unique = true, length = 14)
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

    // ===================== Perfil (comum e ONG) =====================

    /** URL do avatar (ou data URL da imagem comprimida, enquanto não há upload próprio). */
    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    // ===================== Perfil de ONG =====================

    /** Contato público da ONG — pode ser diferente do e-mail de login. */
    @Column(name = "email_institucional", length = 150)
    private String emailInstitucional;

    /** Selo de "verificada", concedido quando o admin aprova a ONG. */
    @Column(name = "is_verificado", nullable = false)
    @Builder.Default
    private Boolean verificado = false;

    /** Apenas o nome de usuário, sem @. */
    @Column(length = 30)
    private String instagram;

    /** Apenas o nome de usuário do X/Twitter, sem @. */
    @Column(length = 15)
    private String twitter;

    /** Link completo da página. */
    @Column(length = 255)
    private String facebook;

    @ElementCollection
    @CollectionTable(name = "ong_equipe", joinColumns = @JoinColumn(name = "usuario_id"))
    @OrderColumn(name = "ordem")
    @Builder.Default
    private List<MembroEquipe> equipe = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ong_horarios_visita", joinColumns = @JoinColumn(name = "usuario_id"))
    @OrderColumn(name = "ordem")
    @Builder.Default
    private List<HorarioVisita> horariosVisita = new ArrayList<>();

    // ===================== Endereço =====================

    /** Apenas dígitos (8). */
    @Column(length = 8)
    private String cep;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    /** Sigla da UF, ex: "SC". */
    @Column(length = 2)
    private String estado;

    @PrePersist
    protected void aoPersistir() {
        this.dataCriacao = LocalDateTime.now();
    }
}
