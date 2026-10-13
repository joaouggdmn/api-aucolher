package com.aucolher.api.adoption;

import com.aucolher.api.adoption.entity.AdoptionRequest;
import com.aucolher.api.adoption.entity.AdoptionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Pedidos de adoção. As listagens carregam animal, dono e adotante juntos
 * (@EntityGraph) — sem isso, montar cada pedido dispararia consultas extras.
 */
public interface AdoptionRequestRepository extends JpaRepository<AdoptionRequest, Long> {

    /** "Meus interesses": os pedidos que a pessoa enviou, do mais recente para o mais antigo. */
    @EntityGraph(attributePaths = {"animal", "animal.owner", "adopter"})
    List<AdoptionRequest> findByAdopterIdOrderByCreatedAtDescIdDesc(Long adopterId);

    /** Pedidos recebidos: os dos animais da conta. */
    @EntityGraph(attributePaths = {"animal", "animal.owner", "adopter"})
    List<AdoptionRequest> findByAnimalOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    @EntityGraph(attributePaths = {"animal", "animal.owner", "adopter"})
    Optional<AdoptionRequest> findWithDetailsById(Long id);

    /** O pedido que impede um novo: qualquer um que não esteja cancelado. */
    Optional<AdoptionRequest> findFirstByAdopterIdAndAnimalIdAndStatusNot(Long adopterId, Long animalId, AdoptionStatus status);

    /** Pedidos de um animal nos status dados — usado para cancelar os ativos. */
    List<AdoptionRequest> findByAnimalIdAndStatusIn(Long animalId, Collection<AdoptionStatus> statuses);
}
