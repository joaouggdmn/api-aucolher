package com.aucolher.api.favorito;

import com.aucolher.api.animal.AnimalRepository;
import com.aucolher.api.animal.AnimalService;
import com.aucolher.api.animal.dto.AnimalResumoDTO;
import com.aucolher.api.animal.entity.Animal;
import com.aucolher.api.animal.entity.StatusAnimal;
import com.aucolher.api.favorito.entity.Favorito;
import com.aucolher.api.shared.exception.BusinessException;
import com.aucolher.api.shared.exception.ResourceNotFoundException;
import com.aucolher.api.usuario.UsuarioRepository;
import com.aucolher.api.usuario.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Favoritos do usuário logado. Animais inativos (tirados do ar pelo dono)
 * somem da lista; os adotados continuam, com o status, para o frontend
 * poder mostrar "adotado".
 */
@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final AnimalRepository animalRepository;
    private final AnimalService animalService;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<AnimalResumoDTO> listar(String email) {
        List<Animal> animais = favoritoRepository
                .findByUsuarioIdAndAnimalStatusNotOrderByDataCriacaoDescIdDesc(buscarUsuario(email).getId(), StatusAnimal.INATIVO)
                .stream()
                .map(Favorito::getAnimal)
                .toList();
        return animalService.resumir(animais);
    }

    /** Só os ids, para o frontend marcar os corações nos cards. */
    @Transactional(readOnly = true)
    public List<Long> listarIds(String email) {
        return favoritoRepository.buscarIdsDosAnimais(buscarUsuario(email).getId(), StatusAnimal.INATIVO);
    }

    /** Idempotente: favoritar de novo não duplica nem dá erro. */
    @Transactional
    public void favoritar(String email, Long animalId) {
        Animal animal = animalRepository.findById(animalId)
                .filter(encontrado -> encontrado.getStatus() != StatusAnimal.INATIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Animal não encontrado"));

        favoritoRepository.favoritar(buscarUsuario(email).getId(), animal.getId());
    }

    /** Idempotente: desfavoritar o que não está favoritado não dá erro. */
    @Transactional
    public void desfavoritar(String email, Long animalId) {
        favoritoRepository.desfavoritar(buscarUsuario(email).getId(), animalId);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }
}
