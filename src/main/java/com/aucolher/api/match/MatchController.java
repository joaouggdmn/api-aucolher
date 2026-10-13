package com.aucolher.api.match;

import com.aucolher.api.match.dto.MatchProfileDTO;
import com.aucolher.api.match.dto.MatchProfileRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * AUmatch da pessoa logada. Só pessoa física: sem token responde 401, e
 * ONG ou admin recebem 403 (regra na SecurityConfig).
 */
@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    /** Respostas do quiz. Quem ainda não respondeu recebe tudo nulo e `complete: false`. */
    @GetMapping("/profile")
    public ResponseEntity<MatchProfileDTO> getProfile(@AuthenticationPrincipal UserDetails loggedUser) {
        return ResponseEntity.ok(matchService.getProfile(loggedUser.getUsername()));
    }

    /** PUT porque substitui as respostas inteiras — o quiz e "Minha conta" mandam todas. */
    @PutMapping("/profile")
    public ResponseEntity<MatchProfileDTO> saveProfile(@Valid @RequestBody MatchProfileRequestDTO request,
                                                       @AuthenticationPrincipal UserDetails loggedUser) {
        return ResponseEntity.ok(matchService.saveProfile(loggedUser.getUsername(), request));
    }
}
