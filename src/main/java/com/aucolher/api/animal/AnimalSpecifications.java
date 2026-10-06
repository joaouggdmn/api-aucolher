package com.aucolher.api.animal;

import com.aucolher.api.animal.dto.AnimalFiltroDTO;
import com.aucolher.api.animal.entity.Animal;
import com.aucolher.api.animal.entity.StatusAnimal;
import com.aucolher.api.user.entity.User;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Monta o WHERE da listagem pública a partir dos filtros recebidos. Cada
 * filtro só entra na consulta se veio na requisição, e todos se somam (AND):
 * ?especie=GATO&porte=PEQUENO vira "especie IN ('GATO') AND porte IN ('PEQUENO')".
 */
final class AnimalSpecifications {

    private AnimalSpecifications() {}

    static Specification<Animal> disponiveis(AnimalFiltroDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();

            // A vitrine só mostra quem está disponível — adotados e inativos nunca aparecem
            condicoes.add(cb.equal(root.get("status"), StatusAnimal.DISPONIVEL));

            adicionarIn(condicoes, root.get("especie"), filtro.especie());
            adicionarIn(condicoes, root.get("porte"), filtro.porte());
            adicionarIn(condicoes, root.get("sexo"), filtro.sexo());
            adicionarIn(condicoes, root.get("faixaEtaria"), filtro.faixaEtaria());
            adicionarIn(condicoes, root.get("nivelEnergia"), filtro.nivelEnergia());
            adicionarIn(condicoes, root.get("temperamento"), filtro.temperamento());

            if (Boolean.TRUE.equals(filtro.necessidadesEspeciais())) {
                condicoes.add(cb.isTrue(root.get("necessidadesEspeciais")));
            }

            // Cidade e UF são as do dono (o animal está onde o dono está)
            Join<Animal, User> dono = root.join("dono");

            if (filtro.cidade() != null) {
                condicoes.add(cb.equal(cb.lower(dono.get("city")), filtro.cidade().toLowerCase(Locale.ROOT)));
            }
            if (filtro.estado() != null) {
                condicoes.add(cb.equal(dono.get("state"), filtro.estado()));
            }

            if (filtro.busca() != null) {
                String termo = "%" + escaparLike(filtro.busca().toLowerCase(Locale.ROOT)) + "%";
                condicoes.add(cb.or(
                        cb.like(cb.lower(root.get("nome")), termo, '\\'),
                        cb.like(cb.lower(root.get("raca")), termo, '\\'),
                        cb.like(cb.lower(dono.get("city")), termo, '\\')
                ));
            }

            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }

    private static void adicionarIn(List<Predicate> condicoes, Expression<?> campo, Collection<?> valores) {
        if (valores != null && !valores.isEmpty()) {
            condicoes.add(campo.in(valores));
        }
    }

    /** % e _ digitados na busca são texto, não curingas do LIKE. */
    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
