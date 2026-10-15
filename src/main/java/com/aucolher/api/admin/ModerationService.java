package com.aucolher.api.admin;

import com.aucolher.api.admin.dto.AdminUserDTO;
import com.aucolher.api.adoption.AdoptionRequestService;
import com.aucolher.api.adoption.entity.CancelReason;
import com.aucolher.api.animal.AnimalRepository;
import com.aucolher.api.animal.AnimalService;
import com.aucolher.api.event.EventService;
import com.aucolher.api.report.ReportService;
import com.aucolher.api.report.dto.ReportDTO;
import com.aucolher.api.report.entity.Report;
import com.aucolher.api.report.entity.ReportResolution;
import com.aucolher.api.report.entity.ReportTargetType;
import com.aucolher.api.shared.exception.BusinessException;
import com.aucolher.api.shared.exception.ResourceNotFoundException;
import com.aucolher.api.user.UserRepository;
import com.aucolher.api.user.entity.User;
import com.aucolher.api.user.entity.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * As decisões do admin que mexem em vários módulos — banir, reativar, remover
 * anúncio ou evento, arquivar denúncia —, cada uma numa transação só.
 *
 * Banir não apaga nem altera o conteúdo da conta: os animais e eventos dela
 * somem das rotas públicas pelo filtro de conta ativa, e voltam se ela for
 * reativada. Pedidos cancelados e denúncias resolvidas não voltam.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final AnimalService animalService;
    private final EventService eventService;
    private final ReportService reportService;
    private final AdoptionRequestService requestService;

    /** Banir (`active: false`) ou reativar (`active: true`) uma conta direto da lista do admin. */
    @Transactional
    public AdminUserDTO setUserActive(Long userId, Boolean active, String adminEmail) {
        if (active == null) {
            throw new BusinessException("Informe se a conta fica ativa.");
        }
        User account = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));

        if (active) {
            account.setActive(true);
        } else {
            ban(account, findAdmin(adminEmail));
        }
        return AdminUserDTO.from(account, animalRepository.countByOwnerId(account.getId()));
    }

    /** Bane a conta por trás do caso: a do perfil denunciado ou a dona do anúncio denunciado. */
    @Transactional
    public List<ReportDTO> banByReport(Long reportId, String adminEmail) {
        Report report = findPendingReport(reportId);
        Long accountId = report.getTargetType() == ReportTargetType.USER ? report.getTargetId() : report.getTargetOwnerId();
        User account = accountId == null ? null : userRepository.findById(accountId).orElse(null);
        if (account == null) {
            throw new BusinessException("Não foi possível identificar a conta denunciada.");
        }
        return ban(account, findAdmin(adminEmail)).stream().map(ReportDTO::forAdmin).toList();
    }

    /** Arquiva o caso como improcedente: esta denúncia e as outras pendentes do mesmo alvo. */
    @Transactional
    public List<ReportDTO> dismiss(Long reportId, String adminEmail) {
        Report report = findPendingReport(reportId);
        return reportService.resolveCase(report.getTargetType(), report.getTargetId(), ReportResolution.DISMISSED,
                        findAdmin(adminEmail), LocalDateTime.now())
                .stream().map(ReportDTO::forAdmin).toList();
    }

    /** Remove o anúncio (REMOVED), cancela os pedidos ativos e resolve as denúncias contra ele. */
    @Transactional
    public void removeAnimal(Long animalId, String adminEmail) {
        animalService.removeByModeration(animalId);
        reportService.resolveCase(ReportTargetType.ANIMAL, animalId, ReportResolution.LISTING_REMOVED,
                findAdmin(adminEmail), LocalDateTime.now());
    }

    /** Remove o evento: ele vira CANCELLED, marcado como removido pela moderação. */
    @Transactional
    public void removeEvent(Long eventId) {
        eventService.removeByModeration(eventId);
    }

    /**
     * O banimento: a conta para de entrar (e o token dela deixa de valer), as
     * denúncias pendentes contra ela são resolvidas e os pedidos ativos em que
     * ela é parte são cancelados (ACCOUNT_BANNED). Devolve as denúncias resolvidas.
     */
    private List<Report> ban(User account, User admin) {
        if (account.getUserType() == UserType.ADMIN) {
            throw new BusinessException("Contas de admin não podem ser banidas.");
        }
        account.setActive(false);
        List<Report> resolved = reportService.resolveAgainstAccount(account.getId(), admin, LocalDateTime.now());
        requestService.cancelAllForAccount(account.getId(), CancelReason.ACCOUNT_BANNED);
        return resolved;
    }

    private Report findPendingReport(Long reportId) {
        Report report = reportService.findReport(reportId);
        if (!report.isPending()) {
            throw new BusinessException("Esta denúncia já foi julgada.");
        }
        return report;
    }

    private User findAdmin(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }
}
