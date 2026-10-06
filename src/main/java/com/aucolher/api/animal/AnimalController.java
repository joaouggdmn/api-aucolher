package com.aucolher.api.animal;

import com.aucolher.api.animal.dto.AlterarStatusDTO;
import com.aucolher.api.animal.dto.AnimalDetalheDTO;
import com.aucolher.api.animal.dto.AnimalFiltroDTO;
import com.aucolher.api.animal.dto.AnimalRequestDTO;
import com.aucolher.api.animal.dto.AnimalResumoDTO;
import com.aucolher.api.shared.dto.PageDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rotas dos anúncios de animais. Mapeado em /api (e não em /api/animais)
 * porque também responde pelos animais de um perfil, em /api/usuarios/{id}/animais.
 *
 * Nas rotas públicas, `usuarioLogado` vem null quando a requisição não traz token.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;

    /**
     * Público: vitrine de animais disponíveis, com filtros (ver AnimalFiltroDTO)
     * e paginação — ?page=0&size=12 (página começa em 0; no máximo 50 por página).
     */
    @GetMapping("/animais")
    public ResponseEntity<PageDTO<AnimalResumoDTO>> listar(@ModelAttribute AnimalFiltroDTO filtro,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(animalService.listarDisponiveis(filtro, page, size));
    }

    /** Todos os anúncios do usuário logado, em qualquer status ("Minha conta"). */
    @GetMapping("/animais/meus")
    public ResponseEntity<List<AnimalResumoDTO>> listarMeus(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(animalService.listarMeus(usuarioLogado.getUsername()));
    }

    /** Público: animais disponíveis de um perfil (página de perfil público). */
    @GetMapping("/usuarios/{usuarioId}/animais")
    public ResponseEntity<List<AnimalResumoDTO>> listarDoPerfil(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(animalService.listarDoPerfil(usuarioId));
    }

    @PostMapping("/animais")
    public ResponseEntity<AnimalDetalheDTO> cadastrar(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                      @Valid @RequestBody AnimalRequestDTO dto) {
        AnimalDetalheDTO resposta = animalService.cadastrar(usuarioLogado.getUsername(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /** Público: qualquer visitante vê os detalhes de um animal. */
    @GetMapping("/animais/{id}")
    public ResponseEntity<AnimalDetalheDTO> buscar(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(animalService.buscarDetalhe(id, emailDe(usuarioLogado)));
    }

    /** Edição completa do anúncio — só o dono. */
    @PutMapping("/animais/{id}")
    public ResponseEntity<AnimalDetalheDTO> editar(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails usuarioLogado,
                                                   @Valid @RequestBody AnimalRequestDTO dto) {
        return ResponseEntity.ok(animalService.editar(id, usuarioLogado.getUsername(), dto));
    }

    /** Marcar como adotado, tirar do ar ou reativar — só o dono. */
    @PatchMapping("/animais/{id}/status")
    public ResponseEntity<AnimalDetalheDTO> alterarStatus(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserDetails usuarioLogado,
                                                          @Valid @RequestBody AlterarStatusDTO dto) {
        return ResponseEntity.ok(animalService.alterarStatus(id, usuarioLogado.getUsername(), dto.status()));
    }

    /** Tira o anúncio do ar (exclusão lógica: vira INATIVO) — só o dono. */
    @DeleteMapping("/animais/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UserDetails usuarioLogado) {
        animalService.inativar(id, usuarioLogado.getUsername());
        return ResponseEntity.noContent().build();
    }

    private static String emailDe(UserDetails usuarioLogado) {
        return usuarioLogado == null ? null : usuarioLogado.getUsername();
    }
}
