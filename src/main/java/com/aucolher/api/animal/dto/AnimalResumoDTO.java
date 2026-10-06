package com.aucolher.api.animal.dto;

import com.aucolher.api.animal.entity.*;

import java.time.LocalDateTime;

/**
 * Animal "de card": listagem, meus animais, perfil público e favoritos.
 *
 * Traz só a foto de capa e deixa a história de fora — com até 4 fotos em
 * data URL por animal, uma página de 12 animais completos passaria de 10 MB.
 * Para o anúncio inteiro, use GET /api/animais/{id} (AnimalDetalheDTO).
 */
public record AnimalResumoDTO(
        Long id,
        String nome,
        Especie especie,
        String raca,
        Sexo sexo,
        Integer idadeValor,
        UnidadeIdade idadeUnidade,
        FaixaEtaria faixaEtaria,
        Porte porte,
        Boolean vacinado,
        Boolean castrado,
        Boolean vermifugado,
        Boolean necessidadesEspeciais,
        Nivel nivelEnergia,
        Temperamento temperamento,
        Boolean bomComCriancas,
        Boolean bomComCaes,
        Boolean bomComGatos,
        Boolean adaptadoApartamento,
        String resumo,
        StatusAnimal status,
        String fotoCapa,
        String cidade,
        String estado,
        DonoAnimalDTO dono,
        LocalDateTime dataCriacao
) {

    public static AnimalResumoDTO from(Animal animal, String fotoCapa) {
        return new AnimalResumoDTO(
                animal.getId(),
                animal.getNome(),
                animal.getEspecie(),
                animal.getRaca(),
                animal.getSexo(),
                animal.getIdadeValor(),
                animal.getIdadeUnidade(),
                animal.getFaixaEtaria(),
                animal.getPorte(),
                animal.getVacinado(),
                animal.getCastrado(),
                animal.getVermifugado(),
                animal.getNecessidadesEspeciais(),
                animal.getNivelEnergia(),
                animal.getTemperamento(),
                animal.getBomComCriancas(),
                animal.getBomComCaes(),
                animal.getBomComGatos(),
                animal.getAdaptadoApartamento(),
                animal.getResumo(),
                animal.getStatus(),
                fotoCapa,
                animal.getDono().getCity(),
                animal.getDono().getState(),
                DonoAnimalDTO.from(animal.getDono()),
                animal.getDataCriacao()
        );
    }
}
