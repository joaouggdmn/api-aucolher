package com.aucolher.api.favorito.entity;

import com.aucolher.api.animal.entity.Animal;
import com.aucolher.api.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Um animal salvo por um usuário — ligação usuário ↔ animal, única por par
 * (constraint uq_favoritos_usuario_animal).
 *
 * Só é lida pelo JPA: a gravação é um INSERT ... ON CONFLICT DO NOTHING no
 * FavoritoRepository, então a data vem do DEFAULT do banco.
 */
@Entity
@Table(name = "favoritos")
@Getter
@NoArgsConstructor
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(name = "data_criacao", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dataCriacao;
}
