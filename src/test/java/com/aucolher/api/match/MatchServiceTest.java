package com.aucolher.api.match;

import com.aucolher.api.animal.entity.AnimalSize;
import com.aucolher.api.match.dto.MatchProfileDTO;
import com.aucolher.api.match.dto.MatchProfileRequestDTO;
import com.aucolher.api.match.entity.*;
import com.aucolher.api.user.UserRepository;
import com.aucolher.api.user.entity.User;
import com.aucolher.api.user.entity.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras do perfil AUmatch, com o banco simulado (Mockito). */
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    private static final String EMAIL = "person@email.com";
    private static final Long USER_ID = 7L;

    @Mock
    private MatchProfileRepository matchProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MatchService matchService;

    @Test
    void personWithoutProfileGetsEmptyAnswers() {
        givenUser();
        when(matchProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        MatchProfileDTO profile = matchService.getProfile(EMAIL);

        assertThat(profile.complete()).isFalse();
        assertThat(profile.housing()).isNull();
        assertThat(profile.hasChildren()).isNull();
    }

    @Test
    void firstAnswersCreateTheProfile() {
        givenUser();
        when(matchProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(matchProfileRepository.saveAndFlush(any(MatchProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchProfileDTO saved = matchService.saveProfile(EMAIL, completeRequest());

        ArgumentCaptor<MatchProfile> captor = ArgumentCaptor.forClass(MatchProfile.class);
        verify(matchProfileRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.complete()).isTrue();
        assertThat(saved.preferredSize()).isEqualTo(AnimalSize.SMALL);
    }

    @Test
    void savingReplacesEveryAnswer() {
        givenUser();
        MatchProfile existing = new MatchProfile(USER_ID);
        existing.setHousing(Housing.APARTMENT);
        existing.setHasChildren(true);
        existing.setHasDogs(true);
        existing.setHasCats(true);
        when(matchProfileRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(matchProfileRepository.saveAndFlush(existing)).thenReturn(existing);

        // Só o porte: as outras respostas ficam vazias (substituição completa)
        MatchProfileDTO saved = matchService.saveProfile(EMAIL,
                new MatchProfileRequestDTO(null, null, null, null, null, null, null, null, AnimalSize.LARGE));

        assertThat(saved.housing()).isNull();
        assertThat(saved.hasChildren()).isNull();
        assertThat(saved.preferredSize()).isEqualTo(AnimalSize.LARGE);
        assertThat(saved.complete()).isFalse();
    }

    @Test
    void householdAnsweredAsNobodyStillCountsAsAnswered() {
        MatchProfile profile = new MatchProfile(USER_ID);
        profile.setHousing(Housing.FARM);
        profile.setExerciseRoutine(ExerciseRoutine.ACTIVE);
        profile.setTimeAway(TimeAway.LONG);
        profile.setHasChildren(false);
        profile.setHasDogs(false);
        profile.setHasCats(false);
        profile.setSpeciesPreference(SpeciesPreference.BOTH);
        profile.setIdealPetProfile(IdealPetProfile.CALM_COMPANION);
        profile.setPreferredSize(AnimalSize.MEDIUM);

        assertThat(profile.isComplete()).isTrue();
    }

    // ===================== Fixtures =====================

    private void givenUser() {
        User user = User.builder().id(USER_ID).email(EMAIL).name("Pessoa").userType(UserType.PERSON).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private static MatchProfileRequestDTO completeRequest() {
        return new MatchProfileRequestDTO(
                Housing.APARTMENT,
                ExerciseRoutine.SEDENTARY,
                TimeAway.SHORT,
                true, false, true,
                SpeciesPreference.DOG,
                IdealPetProfile.CALM_COMPANION,
                AnimalSize.SMALL);
    }
}
