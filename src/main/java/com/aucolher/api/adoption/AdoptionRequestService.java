package com.aucolher.api.adoption;

import com.aucolher.api.adoption.dto.AdopterDTO;
import com.aucolher.api.adoption.dto.AdoptionRequestDTO;
import com.aucolher.api.adoption.entity.AdoptionRequest;
import com.aucolher.api.adoption.entity.AdoptionStatus;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AnimalRepository animalRepository;
    private final AnimalService animalService;
    private final MatchProfileRepository matchProfileRepository;
    private final UserRepository userRepository;

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

    AdoptionRequest findRequest(Long id) {
        return requestRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
    }

    /**
     * Monta os pedidos com o card do animal (as capas numa consulta só), as
     * respostas do quiz de cada adotante e o score de compatibilidade atual.
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

        return requests.stream().map(request -> {
            MatchProfile profile = profiles.get(request.getAdopter().getId());
            return AdoptionRequestDTO.of(
                    request,
                    cards.get(request.getAnimal().getId()),
                    AdopterDTO.from(request.getAdopter(), profile),
                    MatchScoreCalculator.score(profile, request.getAnimal()).score());
        }).toList();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }
}
