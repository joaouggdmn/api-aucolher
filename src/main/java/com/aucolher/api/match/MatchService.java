package com.aucolher.api.match;

import com.aucolher.api.match.dto.MatchProfileDTO;
import com.aucolher.api.match.dto.MatchProfileRequestDTO;
import com.aucolher.api.match.entity.MatchProfile;
import com.aucolher.api.shared.exception.BusinessException;
import com.aucolher.api.user.UserRepository;
import com.aucolher.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUmatch da pessoa logada: as respostas do quiz. Só pessoas físicas chegam
 * aqui — a SecurityConfig barra ONG e admin em /api/match.
 */
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchProfileRepository matchProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MatchProfileDTO getProfile(String email) {
        return matchProfileRepository.findById(findUser(email).getId())
                .map(MatchProfileDTO::from)
                .orElseGet(MatchProfileDTO::empty);
    }

    /** Cria o perfil na primeira resposta; depois, substitui as respostas inteiras. */
    @Transactional
    public MatchProfileDTO saveProfile(String email, MatchProfileRequestDTO request) {
        Long userId = findUser(email).getId();
        MatchProfile profile = matchProfileRepository.findById(userId).orElseGet(() -> new MatchProfile(userId));
        fill(profile, request);
        // saveAndFlush: o @PreUpdate roda antes de a resposta ser montada
        return MatchProfileDTO.from(matchProfileRepository.saveAndFlush(profile));
    }

    private void fill(MatchProfile profile, MatchProfileRequestDTO request) {
        profile.setHousing(request.housing());
        profile.setExerciseRoutine(request.exerciseRoutine());
        profile.setTimeAway(request.timeAway());
        profile.setHasChildren(request.hasChildren());
        profile.setHasDogs(request.hasDogs());
        profile.setHasCats(request.hasCats());
        profile.setSpeciesPreference(request.speciesPreference());
        profile.setIdealPetProfile(request.idealPetProfile());
        profile.setPreferredSize(request.preferredSize());
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }
}
