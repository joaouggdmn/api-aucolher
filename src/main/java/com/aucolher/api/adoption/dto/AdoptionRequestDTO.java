package com.aucolher.api.adoption.dto;

import com.aucolher.api.adoption.entity.AdoptionRequest;
import com.aucolher.api.adoption.entity.AdoptionStatus;
import com.aucolher.api.adoption.entity.CancelReason;
import com.aucolher.api.animal.dto.AnimalSummaryDTO;

import java.time.LocalDateTime;

/**
 * Um pedido de adoção, igual para as duas partes. O dono do animal vem em
 * `animal.owner`. `matchScore` é a compatibilidade (0 a 100) entre as
 * respostas atuais do quiz do adotante e o animal, calculada na hora.
 * `reviews` traz as avaliações das duas partes (só existem depois de CONCLUDED).
 */
public record AdoptionRequestDTO(
        Long id,
        AdoptionStatus status,
        CancelReason cancelReason,
        AdoptionStatus cancelledFromStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime concludedAt,
        LocalDateTime closedAt,
        AnimalSummaryDTO animal,
        AdopterDTO adopter,
        int matchScore,
        RequestReviewsDTO reviews
) {

    public static AdoptionRequestDTO of(AdoptionRequest request, AnimalSummaryDTO animal, AdopterDTO adopter,
                                        int matchScore, RequestReviewsDTO reviews) {
        return new AdoptionRequestDTO(
                request.getId(),
                request.getStatus(),
                request.getCancelReason(),
                request.getCancelledFromStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getConcludedAt(),
                request.getClosedAt(),
                animal,
                adopter,
                matchScore,
                reviews);
    }
}
