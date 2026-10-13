package com.aucolher.api.adoption.entity;

import com.aucolher.api.animal.entity.Animal;
import com.aucolher.api.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pedido de adoção: quem quer adotar (adopter) pede um animal ao dono dele.
 * O dono não é guardado aqui — é sempre o dono do animal.
 *
 * Um pedido "vivo" por pessoa e animal (índice uq_adoption_requests_open):
 * recusado é definitivo, e só um pedido cancelado libera um novo.
 */
@Entity
@Table(name = "adoption_requests")
@Getter
@Setter
@NoArgsConstructor
public class AdoptionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adopter_id", nullable = false)
    private User adopter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdoptionStatus status = AdoptionStatus.PENDING;

    /** Só em CANCELLED. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    private CancelReason cancelReason;

    /** Status em que o pedido estava quando foi cancelado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_from_status", length = 20)
    private AdoptionStatus cancelledFromStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "concluded_at")
    private LocalDateTime concludedAt;

    /** Quando foi recusado ou cancelado. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public AdoptionRequest(Animal animal, User adopter) {
        this.animal = animal;
        this.adopter = adopter;
    }

    @PrePersist
    protected void onPersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** O dono do animal — compara pelo e-mail, que é o que vem no token. */
    public boolean isOwnedBy(String email) {
        return animal.isOwnedBy(email);
    }

    public boolean isAdopter(String email) {
        return email != null && email.equals(adopter.getEmail());
    }
}
