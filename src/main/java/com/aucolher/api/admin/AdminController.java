package com.aucolher.api.admin;

import com.aucolher.api.admin.dto.AdminUserDTO;
import com.aucolher.api.admin.dto.UserStatusDTO;
import com.aucolher.api.report.dto.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Painel do admin. Toda a /api/admin/** é só da conta ADMIN (regra na
 * SecurityConfig): sem token, 401; logado sem ser admin, 403.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ModerationService moderationService;

    // ===================== Moderação =====================

    /** Banir (`{ "active": false }`) ou reativar (`{ "active": true }`) uma conta. */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserDTO> setUserStatus(@PathVariable Long id, @RequestBody UserStatusDTO body,
                                                      @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(moderationService.setUserActive(id, body.active(), admin.getUsername()));
    }

    /** Arquiva esta denúncia e as outras pendentes do mesmo alvo; devolve as que foram arquivadas. */
    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<List<ReportDTO>> dismissReports(@PathVariable Long id, @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(moderationService.dismiss(id, admin.getUsername()));
    }

    /** Bane a conta denunciada (perfil) ou a dona do anúncio denunciado; devolve as denúncias resolvidas. */
    @PostMapping("/reports/{id}/ban")
    public ResponseEntity<List<ReportDTO>> banReportedAccount(@PathVariable Long id, @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(moderationService.banByReport(id, admin.getUsername()));
    }

    @DeleteMapping("/animals/{id}")
    public ResponseEntity<Void> removeAnimal(@PathVariable Long id, @AuthenticationPrincipal UserDetails admin) {
        moderationService.removeAnimal(id, admin.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> removeEvent(@PathVariable Long id) {
        moderationService.removeEvent(id);
        return ResponseEntity.noContent().build();
    }
}
