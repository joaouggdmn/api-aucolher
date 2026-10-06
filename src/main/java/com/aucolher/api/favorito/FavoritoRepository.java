package com.aucolher.api.favorito;

import com.aucolher.api.animal.entity.StatusAnimal;
import com.aucolher.api.favorito.entity.Favorito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    /**
     * Favorita sem duplicar: se o par já existe, o banco simplesmente ignora.
     * Resolve até dois cliques simultâneos, que passariam por um "existe? então
     * insere" feito em Java e estourariam a constraint única.
     */
    @Modifying
    @Query(value = """
            INSERT INTO favoritos (usuario_id, animal_id) VALUES (:usuarioId, :animalId)
            ON CONFLICT (usuario_id, animal_id) DO NOTHING
            """, nativeQuery = true)
    void favoritar(Long usuarioId, Long animalId);

    @Modifying
    @Query("DELETE FROM Favorito f WHERE f.usuario.id = :usuarioId AND f.animal.id = :animalId")
    void desfavoritar(Long usuarioId, Long animalId);

    /** Favoritos com o animal e o dono já carregados, do mais recente para o mais antigo. */
    @EntityGraph(attributePaths = {"animal", "animal.dono"})
    List<Favorito> findByUsuarioIdAndAnimalStatusNotOrderByDataCriacaoDescIdDesc(Long usuarioId, StatusAnimal status);

    @Query("""
            SELECT f.animal.id FROM Favorito f
            WHERE f.usuario.id = :usuarioId AND f.animal.status <> :statusOculto
            ORDER BY f.dataCriacao DESC, f.id DESC
            """)
    List<Long> buscarIdsDosAnimais(Long usuarioId, StatusAnimal statusOculto);
}
