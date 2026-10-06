package com.aucolher.api.animal;

import com.aucolher.api.animal.entity.Animal;
import com.aucolher.api.animal.entity.StatusAnimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * As listagens carregam o dono junto (@EntityGraph) — sem isso, montar cada
 * card dispararia uma consulta extra por animal para buscar nome e cidade do dono.
 */
public interface AnimalRepository extends JpaRepository<Animal, Long>, JpaSpecificationExecutor<Animal> {

    /** Listagem pública com filtros (ver AnimalSpecifications). */
    @Override
    @EntityGraph(attributePaths = "dono")
    Page<Animal> findAll(Specification<Animal> spec, Pageable pageable);

    /** "Meus animais": todos os anúncios da conta, em qualquer status. */
    @EntityGraph(attributePaths = "dono")
    List<Animal> findByDonoEmailOrderByDataCriacaoDesc(String email);

    /** Animais de um perfil público (só os disponíveis). */
    @EntityGraph(attributePaths = "dono")
    List<Animal> findByDonoIdAndStatusOrderByDataCriacaoDesc(Long donoId, StatusAnimal status);

    /**
     * Só a foto de capa (ordem 0) de cada animal, numa consulta única. Os
     * cards não precisam das outras fotos, que podem ter centenas de KB cada.
     */
    @Query(value = "SELECT animal_id AS animalId, url FROM animal_fotos WHERE ordem = 0 AND animal_id IN (:ids)",
            nativeQuery = true)
    List<FotoCapa> buscarCapas(Collection<Long> ids);

    interface FotoCapa {
        Long getAnimalId();
        String getUrl();
    }
}
