package com.aucolher.api.admin;

import com.aucolher.api.admin.dto.AdminStatsDTO.*;
import com.aucolher.api.adoption.entity.AdoptionRequest;
import com.aucolher.api.adoption.entity.AdoptionStatus;
import com.aucolher.api.adoption.entity.ReturnReason;
import com.aucolher.api.animal.entity.Animal;
import com.aucolher.api.animal.entity.AnimalStatus;
import com.aucolher.api.animal.entity.Species;
import com.aucolher.api.user.entity.User;
import com.aucolher.api.user.entity.UserType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** As contas da Visão geral do admin — em especial a taxa de reabandono, a métrica do TCC. */
class AdminStatsServiceTest {

    private final User donor = account(2L, UserType.NGO, true);
    private final User person = account(3L, UserType.PERSON, true);
    private final User rescue = account(4L, UserType.NGO, true);

    @Test
    void returnRateSplitsByTheFrozenScoreAndLeavesNgoAdoptionsOut() {
        List<AdoptionRequest> requests = new ArrayList<>();
        // Faixa alta: 4 adoções, 1 devolvida
        requests.add(happened(AdoptionStatus.RETURNED, person, 90, ReturnReason.OTHER_PETS));
        requests.add(happened(AdoptionStatus.CONCLUDED, person, 85, null));
        requests.add(happened(AdoptionStatus.CONCLUDED, person, 80, null));
        requests.add(happened(AdoptionStatus.CONCLUDED, person, 100, null));
        // Faixa média: 1 adoção, repassada
        requests.add(happened(AdoptionStatus.REHOMED, person, 79, ReturnReason.COSTS));
        // Faixa baixa: nenhuma. Sem score: 1 adoção, devolvida
        requests.add(happened(AdoptionStatus.RETURNED, person, null, ReturnReason.COSTS));
        // ONG adotando (resgate): fica fora de tudo
        requests.add(happened(AdoptionStatus.CONCLUDED, rescue, null, null));
        // Pedido que não virou adoção: fora
        requests.add(request(AdoptionStatus.PENDING, person));

        Returns returns = AdminStatsService.returns(requests);

        assertThat(returns.adoptions()).isEqualTo(6);
        assertThat(returns.returned()).isEqualTo(2);
        assertThat(returns.rehomed()).isEqualTo(1);
        assertThat(returns.rate()).isEqualTo(50.0);
        assertThat(returns.byRange()).extracting(ScoreRange::range, ScoreRange::adoptions, ScoreRange::returns, ScoreRange::rate)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("HIGH", 4L, 1L, 25.0),
                        org.assertj.core.groups.Tuple.tuple("MEDIUM", 1L, 1L, 100.0),
                        org.assertj.core.groups.Tuple.tuple("LOW", 0L, 0L, null));
        assertThat(returns.noScore()).isEqualTo(new NoScore(1, 1));
        assertThat(returns.byReason()).hasSize(7)
                .containsEntry(ReturnReason.COSTS, 2L)
                .containsEntry(ReturnReason.OTHER_PETS, 1L)
                .containsEntry(ReturnReason.BEHAVIOR, 0L);
    }

    @Test
    void rateHasOneDecimalAndIsNullWithoutAdoptions() {
        assertThat(AdminStatsService.rateOf(1, 6)).isEqualTo(16.7);
        assertThat(AdminStatsService.rateOf(0, 0)).isNull();
        assertThat(AdminStatsService.returns(List.of()).rate()).isNull();
    }

    @Test
    void monthlySeriesCoverTwelveMonthsOldestFirst() {
        List<String> months = AdminStatsService.lastMonths(YearMonth.of(2026, 10));

        assertThat(months).hasSize(12).startsWith("2025-11").endsWith("2026-10");
    }

    @Test
    void funnelCountsEveryStepReachedOrPassed() {
        List<AdoptionRequest> requests = List.of(
                request(AdoptionStatus.PENDING, person),
                request(AdoptionStatus.ACCEPTED, person),
                request(AdoptionStatus.AWAITING_DELIVERY, person),
                happened(AdoptionStatus.CONCLUDED, person, null, null),
                happened(AdoptionStatus.RETURNED, person, null, ReturnReason.BEHAVIOR),
                request(AdoptionStatus.CANCELLED, person));

        Adoptions adoptions = AdminStatsService.adoptions(requests, AdminStatsService.lastMonths(YearMonth.now()));

        assertThat(adoptions.funnel()).isEqualTo(new Funnel(6, 4, 3, 2));
        assertThat(adoptions.concludedByMonth().get(11).count()).isEqualTo(2);
    }

    @Test
    void bannedOwnersLeaveTheShowcaseNumbersButAdoptedAnimalsStay() {
        User banned = account(5L, UserType.PERSON, false);
        Animal inProgress = animal(1L, AnimalStatus.AVAILABLE, donor);
        List<Animal> animals = List.of(
                animal(2L, AnimalStatus.AVAILABLE, donor),
                inProgress,
                animal(3L, AnimalStatus.AVAILABLE, banned),
                animal(4L, AnimalStatus.ADOPTED, banned),
                animal(5L, AnimalStatus.REMOVED, donor),
                animal(6L, AnimalStatus.INACTIVE, donor));
        AdoptionRequest accepted = new AdoptionRequest(inProgress, person);
        accepted.setStatus(AdoptionStatus.ACCEPTED);

        Animals stats = AdminStatsService.animals(animals, List.of(accepted));

        assertThat(stats.available()).isEqualTo(1);
        assertThat(stats.inProgress()).isEqualTo(1);
        assertThat(stats.adopted()).isEqualTo(1);
        assertThat(stats.removed()).isEqualTo(1);
        assertThat(stats.bySpecies()).containsEntry(Species.DOG, 2L).containsEntry(Species.CAT, 0L);
    }

    @Test
    void rankingCountsAdoptionsOfNgoAnimals() {
        List<AdoptionRequest> requests = List.of(
                happened(AdoptionStatus.CONCLUDED, person, null, null),
                happened(AdoptionStatus.REHOMED, person, null, ReturnReason.COSTS));

        assertThat(AdminStatsService.ngoRankingByAdoptions(requests))
                .containsExactly(new RankedNgo(2L, "Conta 2", 0, 2));
    }

    // ===================== Fixtures =====================

    private AdoptionRequest happened(AdoptionStatus status, User adopter, Integer score, ReturnReason reason) {
        AdoptionRequest request = request(status, adopter);
        request.setConcludedAt(LocalDateTime.now());
        request.setMatchScoreAtAdoption(score);
        if (reason != null) {
            request.setReturnReason(reason);
            request.setReturnDate(LocalDate.now());
        }
        return request;
    }

    private AdoptionRequest request(AdoptionStatus status, User adopter) {
        AdoptionRequest request = new AdoptionRequest(animal(1L, AnimalStatus.ADOPTED, donor), adopter);
        request.setStatus(status);
        return request;
    }

    private static Animal animal(Long id, AnimalStatus status, User owner) {
        Animal animal = new Animal();
        animal.setId(id);
        animal.setName("Animal " + id);
        animal.setSpecies(Species.DOG);
        animal.setStatus(status);
        animal.setOwner(owner);
        return animal;
    }

    private static User account(Long id, UserType type, boolean active) {
        User user = User.builder().id(id).name("Conta " + id).userType(type).build();
        user.setActive(active);
        return user;
    }
}
