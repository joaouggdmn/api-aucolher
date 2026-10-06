package com.aucolher.api.favorito;

import com.aucolher.api.animal.dto.AnimalResumoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Favoritos da conta logada — todas as rotas exigem token. */
@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    /** Cards dos animais favoritados, do mais recente para o mais antigo. */
    @GetMapping
    public ResponseEntity<List<AnimalResumoDTO>> listar(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(favoritoService.listar(usuarioLogado.getUsername()));
    }

    /** Só os ids dos animais favoritados. */
    @GetMapping("/ids")
    public ResponseEntity<List<Long>> listarIds(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(favoritoService.listarIds(usuarioLogado.getUsername()));
    }

    /** PUT porque é idempotente: repetir a chamada não cria um segundo favorito. */
    @PutMapping("/{animalId}")
    public ResponseEntity<Void> favoritar(@PathVariable Long animalId, @AuthenticationPrincipal UserDetails usuarioLogado) {
        favoritoService.favoritar(usuarioLogado.getUsername(), animalId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{animalId}")
    public ResponseEntity<Void> desfavoritar(@PathVariable Long animalId, @AuthenticationPrincipal UserDetails usuarioLogado) {
        favoritoService.desfavoritar(usuarioLogado.getUsername(), animalId);
        return ResponseEntity.noContent().build();
    }
}
