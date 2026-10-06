package com.aucolher.api.animal.entity;

import com.aucolher.api.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Animal anunciado para adoção por uma ONG ou por um usuário comum.
 *
 * A localização não fica aqui: o animal está onde o dono está, então cidade
 * e UF vêm de {@link #dono}. Os campos seguem as etapas do cadastro no
 * frontend — dados básicos, saúde, comportamento/compatibilidade e anúncio.
 */
@Entity
@Table(name = "animais")
@Getter
@Setter
@NoArgsConstructor
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dono_id", nullable = false)
    private User dono;

    // ===================== Dados básicos =====================

    @Column(nullable = false, length = 60)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Especie especie;

    @Column(nullable = false, length = 60)
    private String raca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sexo sexo;

    @Column(name = "idade_valor", nullable = false)
    private Integer idadeValor;

    @Enumerated(EnumType.STRING)
    @Column(name = "idade_unidade", nullable = false, length = 10)
    private UnidadeIdade idadeUnidade;

    /** Calculada a partir da idade a cada gravação (ver calcularFaixaEtaria) — não vem do cliente. */
    @Enumerated(EnumType.STRING)
    @Column(name = "faixa_etaria", nullable = false, length = 10)
    private FaixaEtaria faixaEtaria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Porte porte;

    // ===================== Saúde =====================

    @Column(nullable = false)
    private Boolean vacinado = false;

    @Column(nullable = false)
    private Boolean castrado = false;

    @Column(nullable = false)
    private Boolean vermifugado = false;

    @Column(name = "necessidades_especiais", nullable = false)
    private Boolean necessidadesEspeciais = false;

    // ===================== Comportamento e compatibilidade =====================

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_energia", nullable = false, length = 10)
    private Nivel nivelEnergia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Temperamento temperamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_independencia", nullable = false, length = 10)
    private Nivel nivelIndependencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_vocalizacao", nullable = false, length = 10)
    private Nivel nivelVocalizacao;

    @Column(name = "bom_com_criancas", nullable = false)
    private Boolean bomComCriancas;

    @Column(name = "bom_com_caes", nullable = false)
    private Boolean bomComCaes;

    @Column(name = "bom_com_gatos", nullable = false)
    private Boolean bomComGatos;

    @Column(name = "adaptado_apartamento", nullable = false)
    private Boolean adaptadoApartamento;

    // ===================== Anúncio =====================

    /** Frase curta do card da listagem. */
    @Column(nullable = false, length = 200)
    private String resumo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String historia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAnimal status = StatusAnimal.DISPONIVEL;

    /**
     * De 1 a 4 fotos; a primeira é a capa do card. A ordem da lista vira a
     * coluna "ordem" (@OrderColumn), como a equipe no perfil da ONG.
     */
    @ElementCollection
    @CollectionTable(name = "animal_fotos", joinColumns = @JoinColumn(name = "animal_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private List<String> fotos = new ArrayList<>();

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void aoPersistir() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = dataCriacao;
        calcularFaixaEtaria();
    }

    @PreUpdate
    protected void aoAtualizar() {
        dataAtualizacao = LocalDateTime.now();
        calcularFaixaEtaria();
    }

    /** Recalculada a cada gravação, assim nunca fica em desacordo com a idade informada. */
    private void calcularFaixaEtaria() {
        faixaEtaria = FaixaEtaria.de(idadeValor, idadeUnidade);
    }

    /** Compara pelo e-mail, que é o que vem no token. Visitante sem login (null) nunca é o dono. */
    public boolean pertenceA(String email) {
        return email != null && email.equals(dono.getEmail());
    }
}
