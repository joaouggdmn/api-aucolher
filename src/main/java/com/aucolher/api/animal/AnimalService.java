package com.aucolher.api.animal;

import com.aucolher.api.animal.dto.AnimalDetalheDTO;
import com.aucolher.api.animal.dto.AnimalFiltroDTO;
import com.aucolher.api.animal.dto.AnimalRequestDTO;
import com.aucolher.api.animal.dto.AnimalResumoDTO;
import com.aucolher.api.animal.entity.Animal;
import com.aucolher.api.animal.entity.StatusAnimal;
import com.aucolher.api.shared.dto.PageDTO;
import com.aucolher.api.shared.exception.ForbiddenException;
import com.aucolher.api.shared.exception.BusinessException;
import com.aucolher.api.shared.exception.ResourceNotFoundException;
import com.aucolher.api.usuario.UsuarioRepository;
import com.aucolher.api.usuario.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regras dos anúncios de animais. Como nas outras Services, o DTO chega já
 * validado e normalizado; aqui ficam só as regras de negócio.
 */
@Service
@RequiredArgsConstructor
public class AnimalService {

    static final int TAMANHO_PAGINA_MAXIMO = 50;

    private final AnimalRepository animalRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Qualquer conta (ONG ou usuário comum) pode anunciar, desde que tenha
     * cidade e UF no perfil — é de lá que sai a localização do animal.
     */
    @Transactional
    public AnimalDetalheDTO cadastrar(String emailDono, AnimalRequestDTO dto) {
        Usuario dono = usuarioRepository.findByEmail(emailDono)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (dono.getCidade() == null || dono.getEstado() == null) {
            throw new BusinessException("Complete a cidade e a UF do seu perfil antes de anunciar um animal");
        }

        Animal animal = new Animal();
        animal.setDono(dono);
        preencher(animal, dto);

        return AnimalDetalheDTO.from(animalRepository.save(animal));
    }

    /**
     * Detalhes públicos do animal. Anúncio inativo só aparece para o próprio
     * dono; para os outros ele "não existe" (404), como se tivesse sido apagado.
     *
     * @param emailVisitante e-mail de quem está vendo, ou null se não estiver logado
     */
    @Transactional(readOnly = true)
    public AnimalDetalheDTO buscarDetalhe(Long id, String emailVisitante) {
        Animal animal = buscarAnimal(id);

        if (animal.getStatus() == StatusAnimal.INATIVO && !animal.pertenceA(emailVisitante)) {
            throw animalNaoEncontrado();
        }

        return AnimalDetalheDTO.from(animal);
    }

    /** Vitrine pública: só disponíveis, com filtros, do anúncio mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public PageDTO<AnimalResumoDTO> listarDisponiveis(AnimalFiltroDTO filtro, int pagina, int tamanho) {
        PageRequest paginacao = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamanho, 1), TAMANHO_PAGINA_MAXIMO),
                Sort.by(Sort.Direction.DESC, "dataCriacao"));

        Page<Animal> resultado = animalRepository.findAll(AnimalSpecifications.disponiveis(filtro), paginacao);
        Map<Long, AnimalResumoDTO> resumos = resumosPorId(resultado.getContent());
        return PageDTO.from(resultado.map(animal -> resumos.get(animal.getId())));
    }

    /** "Meus animais": todos os anúncios da conta logada, inclusive adotados e inativos. */
    @Transactional(readOnly = true)
    public List<AnimalResumoDTO> listarMeus(String emailDono) {
        return resumir(animalRepository.findByDonoEmailOrderByDataCriacaoDesc(emailDono));
    }

    /** Animais disponíveis de um perfil público. */
    @Transactional(readOnly = true)
    public List<AnimalResumoDTO> listarDoPerfil(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
        return resumir(animalRepository.findByDonoIdAndStatusOrderByDataCriacaoDesc(usuarioId, StatusAnimal.DISPONIVEL));
    }

    /**
     * Monta os cards de uma lista de animais, buscando as fotos de capa numa
     * consulta só. Público porque os favoritos usam os mesmos cards.
     */
    @Transactional(readOnly = true)
    public List<AnimalResumoDTO> resumir(List<Animal> animais) {
        Map<Long, AnimalResumoDTO> resumos = resumosPorId(animais);
        return animais.stream().map(animal -> resumos.get(animal.getId())).toList();
    }

    private Map<Long, AnimalResumoDTO> resumosPorId(List<Animal> animais) {
        if (animais.isEmpty()) return Map.of();

        Map<Long, String> capas = animalRepository.buscarCapas(animais.stream().map(Animal::getId).toList()).stream()
                .collect(Collectors.toMap(AnimalRepository.FotoCapa::getAnimalId, AnimalRepository.FotoCapa::getUrl));

        return animais.stream().collect(Collectors.toMap(
                Animal::getId,
                animal -> AnimalResumoDTO.from(animal, capas.get(animal.getId()))));
    }

    /**
     * Substitui o anúncio inteiro (PUT). Animal adotado fica congelado; um
     * inativo pode ser editado antes de voltar ao ar.
     */
    @Transactional
    public AnimalDetalheDTO editar(Long id, String emailDono, AnimalRequestDTO dto) {
        Animal animal = buscarDoDono(id, emailDono);

        if (animal.getStatus() == StatusAnimal.ADOTADO) {
            throw new BusinessException("Um animal já adotado não pode mais ser editado");
        }

        preencher(animal, dto);
        return salvarEResponder(animal);
    }

    /**
     * Muda o status do anúncio: marcar como adotado, tirar do ar (INATIVO) ou
     * voltar a DISPONIVEL. ADOTADO é definitivo.
     */
    @Transactional
    public AnimalDetalheDTO alterarStatus(Long id, String emailDono, StatusAnimal novoStatus) {
        Animal animal = buscarDoDono(id, emailDono);

        if (animal.getStatus() == novoStatus) {
            return AnimalDetalheDTO.from(animal);
        }
        if (animal.getStatus() == StatusAnimal.ADOTADO) {
            throw new BusinessException("Um animal já adotado não muda mais de status");
        }

        animal.setStatus(novoStatus);
        return salvarEResponder(animal);
    }

    /** "Excluir" o anúncio: exclusão lógica, o animal vira INATIVO. */
    @Transactional
    public void inativar(Long id, String emailDono) {
        alterarStatus(id, emailDono, StatusAnimal.INATIVO);
    }

    /** Animal que o usuário logado quer alterar: precisa existir e ser dele. */
    private Animal buscarDoDono(Long id, String emailDono) {
        Animal animal = buscarAnimal(id);
        if (!animal.pertenceA(emailDono)) {
            throw new ForbiddenException("Só quem anunciou pode alterar este animal");
        }
        return animal;
    }

    /**
     * O flush força o UPDATE agora — é nele que rodam o @PreUpdate (faixa
     * etária e data de atualização) — para a resposta já sair com os valores novos.
     */
    private AnimalDetalheDTO salvarEResponder(Animal animal) {
        return AnimalDetalheDTO.from(animalRepository.saveAndFlush(animal));
    }

    private Animal buscarAnimal(Long id) {
        return animalRepository.findById(id).orElseThrow(this::animalNaoEncontrado);
    }

    private ResourceNotFoundException animalNaoEncontrado() {
        return new ResourceNotFoundException("Animal não encontrado");
    }

    /** Copia o anúncio do DTO para a entidade — usado no cadastro e na edição. */
    private void preencher(Animal animal, AnimalRequestDTO dto) {
        animal.setNome(dto.nome());
        animal.setEspecie(dto.especie());
        animal.setRaca(dto.raca());
        animal.setSexo(dto.sexo());
        animal.setIdadeValor(dto.idadeValor());
        animal.setIdadeUnidade(dto.idadeUnidade());
        animal.setPorte(dto.porte());
        animal.setVacinado(dto.vacinado());
        animal.setCastrado(dto.castrado());
        animal.setVermifugado(dto.vermifugado());
        animal.setNecessidadesEspeciais(dto.necessidadesEspeciais());
        animal.setNivelEnergia(dto.nivelEnergia());
        animal.setTemperamento(dto.temperamento());
        animal.setNivelIndependencia(dto.nivelIndependencia());
        animal.setNivelVocalizacao(dto.nivelVocalizacao());
        animal.setBomComCriancas(dto.bomComCriancas());
        animal.setBomComCaes(dto.bomComCaes());
        animal.setBomComGatos(dto.bomComGatos());
        animal.setAdaptadoApartamento(dto.adaptadoApartamento());
        animal.setResumo(dto.resumo());
        animal.setHistoria(dto.historia());

        // A ordem enviada é a ordem de exibição; a primeira foto é a capa
        animal.getFotos().clear();
        animal.getFotos().addAll(dto.fotos());
    }
}
