package com.aucolher.api.adoption;

import com.aucolher.api.adoption.dto.AdopterDTO;
import com.aucolher.api.adoption.dto.AdoptionRequestDTO;
import com.aucolher.api.adoption.dto.RequestReviewsDTO;
import com.aucolher.api.adoption.dto.ReviewDTO;
import com.aucolher.api.adoption.entity.AdoptionReview;
import com.aucolher.api.adoption.entity.AdoptionRequest;
import com.aucolher.api.adoption.entity.AdoptionStatus;
import com.aucolher.api.adoption.entity.CancelReason;
import com.aucolher.api.adoption.entity.ReviewRole;
import com.aucolher.api.animal.AnimalLeftListingEvent;
import com.aucolher.api.animal.AnimalRepository;
import com.aucolher.api.animal.AnimalService;
import com.aucolher.api.animal.dto.AnimalSummaryDTO;
import com.aucolher.api.animal.entity.Animal;
import com.aucolher.api.animal.entity.AnimalStatus;
import com.aucolher.api.match.MatchProfileRepository;
import com.aucolher.api.match.MatchScoreCalculator;
import com.aucolher.api.match.entity.MatchProfile;
import com.aucolher.api.shared.exception.BusinessException;
import com.aucolher.api.shared.exception.ForbiddenException;
import com.aucolher.api.shared.exception.ResourceNotFoundException;
import com.aucolher.api.user.UserRepository;
import com.aucolher.api.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pedidos de adoção. Quem pede é pessoa ou ONG (a SecurityConfig barra o
 * admin); o dono é sempre o dono do animal. Só as duas partes veem um pedido.
 */
@Service
@RequiredArgsConstructor
public class AdoptionRequestService {

    private final AdoptionRequestRepository requestRepository;
    private final AdoptionReviewRepository reviewRepository;
    private final AnimalRepository animalRepository;
    private final AnimalService animalService;
    private final MatchProfileRepository matchProfileRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /**
     * Novo pedido (PENDING). O animal precisa estar disponível e não ser de
     * quem pede, e não pode haver outro pedido não cancelado da mesma pessoa
     * para ele: recusado é definitivo, só um cancelado libera um pedido novo.
     */
    @Transactional
    public AdoptionRequestDTO create(String email, Long animalId) {
        User adopter = findUser(email);
        Animal animal = animalRepository.findById(animalId)
                .filter(found -> found.getStatus() != AnimalStatus.INACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Animal não encontrado"));

        if (animal.isOwnedBy(email)) {
            throw new ForbiddenException("Você não pode pedir para adotar o seu próprio animal");
        }
        if (animal.getStatus() != AnimalStatus.AVAILABLE) {
            throw new BusinessException("Este animal já foi adotado");
        }
        requestRepository.findFirstByAdopterIdAndAnimalIdAndStatusNot(adopter.getId(), animalId, AdoptionStatus.CANCELLED)
                .ifPresent(existing -> {
                    throw new BusinessException(existing.getStatus() == AdoptionStatus.REJECTED
                            ? "O dono já recusou o seu pedido para este animal"
                            : "Você já enviou um pedido para este animal");
                });

        try {
            return toDtos(List.of(requestRepository.saveAndFlush(new AdoptionRequest(animal, adopter)))).get(0);
        } catch (DataIntegrityViolationException e) {
            // Dois cliques ao mesmo tempo: o índice único do banco segura o segundo
            throw new BusinessException("Você já enviou um pedido para este animal");
        }
    }

    /** "Meus interesses": os pedidos que a pessoa enviou. */
    @Transactional(readOnly = true)
    public List<AdoptionRequestDTO> listSent(String email) {
        return toDtos(requestRepository.findByAdopterIdOrderByCreatedAtDescIdDesc(findUser(email).getId()));
    }

    /** Os pedidos dos animais da conta, em qualquer status. */
    @Transactional(readOnly = true)
    public List<AdoptionRequestDTO> listReceived(String email) {
        return toDtos(requestRepository.findByAnimalOwnerIdOrderByCreatedAtDescIdDesc(findUser(email).getId()));
    }

    @Transactional(readOnly = true)
    public AdoptionRequestDTO get(Long id, String email) {
        AdoptionRequest request = findRequest(id);
        if (!request.isAdopter(email) && !request.isOwnedBy(email)) {
            throw new ForbiddenException("Só quem participa do pedido pode vê-lo");
        }
        return toDtos(List.of(request)).get(0);
    }

    /**
     * Muda o status de um pedido. Cada transição tem dono certo e status de
     * partida certo — papel errado responde 403, status errado responde 400:
     * - dono: PENDING → ACCEPTED ou REJECTED; ACCEPTED → AWAITING_DELIVERY;
     *   ACCEPTED/AWAITING_DELIVERY → CANCELLED (desistiu);
     * - adotante: AWAITING_DELIVERY → CONCLUDED; qualquer ativo → CANCELLED (desistiu).
     * Repetir o status atual não muda nada (clique duplo).
     */
    @Transactional
    public AdoptionRequestDTO changeStatus(Long id, String email, AdoptionStatus target) {
        AdoptionRequest request = findRequest(id);
        boolean isOwner = request.isOwnedBy(email);
        boolean isAdopter = request.isAdopter(email);
        if (!isOwner && !isAdopter) {
            throw new ForbiddenException("Só quem participa do pedido pode alterá-lo");
        }
        if (request.getStatus() == target) {
            return toDtos(List.of(request)).get(0);
        }

        switch (target) {
            case ACCEPTED -> {
                requireRole(isOwner, "Só o dono do animal pode aceitar o pedido");
                requireStatus(request, "Só um pedido pendente pode ser aceito", AdoptionStatus.PENDING);
                request.accept();
            }
            case REJECTED -> {
                requireRole(isOwner, "Só o dono do animal pode recusar o pedido");
                requireStatus(request, "Só um pedido pendente pode ser recusado", AdoptionStatus.PENDING);
                request.reject();
            }
            case AWAITING_DELIVERY -> {
                requireRole(isOwner, "Só o dono do animal confirma a entrega");
                requireStatus(request, "Só um pedido aceito pode ir para a entrega", AdoptionStatus.ACCEPTED);
                request.requestDelivery();
            }
            case CONCLUDED -> {
                requireRole(isAdopter, "Só quem adotou confirma a chegada do pet");
                requireStatus(request, "A chegada só é confirmada depois que o dono confirmar a entrega",
                        AdoptionStatus.AWAITING_DELIVERY);
                conclude(request);
            }
            case CANCELLED -> {
                if (isAdopter) {
                    requireStatus(request, "Este pedido já foi encerrado", AdoptionStatus.PENDING,
                            AdoptionStatus.ACCEPTED, AdoptionStatus.AWAITING_DELIVERY);
                    request.cancel(CancelReason.ADOPTER_WITHDREW);
                } else {
                    // Pendente o dono não cancela: recusa
                    requireStatus(request, "Para um pedido pendente, use Recusar",
                            AdoptionStatus.ACCEPTED, AdoptionStatus.AWAITING_DELIVERY);
                    request.cancel(CancelReason.OWNER_WITHDREW);
                }
            }
            case PENDING -> throw new BusinessException("Um pedido não volta a ficar pendente");
        }

        return toDtos(List.of(requestRepository.saveAndFlush(request))).get(0);
    }

    /**
     * Fecha a adoção numa transação só: o pedido é concluído, o animal vira
     * ADOPTED e os outros pedidos ativos dele são cancelados (ANIMAL_ADOPTED).
     * A linha do animal fica travada (FOR UPDATE) até o fim — se dois
     * adotantes confirmarem ao mesmo tempo, o segundo encontra o animal já
     * adotado e o próprio pedido já cancelado.
     */
    private void conclude(AdoptionRequest request) {
        Animal animal = request.getAnimal();
        entityManager.refresh(animal, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(request);
        if (animal.getStatus() != AnimalStatus.AVAILABLE || request.getStatus() != AdoptionStatus.AWAITING_DELIVERY) {
            throw new BusinessException("Este animal já foi adotado");
        }

        request.conclude();
        requestRepository.findByAnimalIdAndStatusIn(animal.getId(), AdoptionStatus.ACTIVE).stream()
                .filter(other -> !other.getId().equals(request.getId()))
                .forEach(other -> other.cancel(CancelReason.ANIMAL_ADOPTED));
        animal.setStatus(AnimalStatus.ADOPTED);
    }

    /**
     * O dono tirou o animal da vitrine por conta própria: os pedidos ativos
     * dele são cancelados (ANIMAL_UNAVAILABLE). Roda na mesma transação.
     */
    @EventListener
    public void onAnimalLeftListing(AnimalLeftListingEvent event) {
        requestRepository.findByAnimalIdAndStatusIn(event.animalId(), AdoptionStatus.ACTIVE)
                .forEach(request -> request.cancel(CancelReason.ANIMAL_UNAVAILABLE));
    }

    private static void requireRole(boolean hasRole, String message) {
        if (!hasRole) throw new ForbiddenException(message);
    }

    private static void requireStatus(AdoptionRequest request, String message, AdoptionStatus... allowed) {
        if (!Arrays.asList(allowed).contains(request.getStatus())) throw new BusinessException(message);
    }

    AdoptionRequest findRequest(Long id) {
        return requestRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
    }

    /**
     * Monta os pedidos com o card do animal (as capas numa consulta só), as
     * respostas do quiz de cada adotante, o score de compatibilidade atual e
     * as avaliações.
     */
    List<AdoptionRequestDTO> toDtos(List<AdoptionRequest> requests) {
        if (requests.isEmpty()) return List.of();

        // Um animal pode ter vários pedidos: cada card é montado uma vez só
        Map<Long, Animal> animalsById = new LinkedHashMap<>();
        requests.forEach(request -> animalsById.putIfAbsent(request.getAnimal().getId(), request.getAnimal()));
        Map<Long, AnimalSummaryDTO> cards = animalService.summarize(List.copyOf(animalsById.values())).stream()
                .collect(Collectors.toMap(AnimalSummaryDTO::id, Function.identity()));

        List<Long> adopterIds = requests.stream().map(request -> request.getAdopter().getId()).distinct().toList();
        Map<Long, MatchProfile> profiles = matchProfileRepository.findAllById(adopterIds).stream()
                .collect(Collectors.toMap(MatchProfile::getUserId, Function.identity()));

        Map<Long, List<AdoptionReview>> reviews = reviewRepository
                .findByRequestIdIn(requests.stream().map(AdoptionRequest::getId).toList()).stream()
                .collect(Collectors.groupingBy(review -> review.getRequest().getId()));

        return requests.stream().map(request -> {
            MatchProfile profile = profiles.get(request.getAdopter().getId());
            return AdoptionRequestDTO.of(
                    request,
                    cards.get(request.getAnimal().getId()),
                    AdopterDTO.from(request.getAdopter(), profile),
                    MatchScoreCalculator.score(profile, request.getAnimal()).score(),
                    reviewsOf(reviews.getOrDefault(request.getId(), List.of())));
        }).toList();
    }

    private static RequestReviewsDTO reviewsOf(List<AdoptionReview> reviews) {
        if (reviews.isEmpty()) return RequestReviewsDTO.NONE;
        return new RequestReviewsDTO(
                reviews.stream().filter(review -> review.getAuthorRole() == ReviewRole.ADOPTER).findFirst().map(ReviewDTO::from).orElse(null),
                reviews.stream().filter(review -> review.getAuthorRole() == ReviewRole.OWNER).findFirst().map(ReviewDTO::from).orElse(null));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }
}
