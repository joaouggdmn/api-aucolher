package com.aucolher.api.animal.dto;

import com.aucolher.api.animal.entity.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Animal completo, com todas as fotos — página de detalhes e resposta do
 * cadastro/edição. Cidade e UF são as do perfil do dono.
 *
 * Dono e fotos são carregados sob demanda (lazy): monte este DTO dentro de
 * uma transação (open-in-view está desligado).
 */
public record AnimalDetalheDTO(
        Long id,
        String nome,
        Especie especie,
        String raca,
        Sexo sexo,
        Integer idadeValor,
        UnidadeIdade idadeUnidade,
        FaixaEtaria faixaEtaria,
        Porte porte,

        // Saúde
        Boolean vacinado,
        Boolean castrado,
        Boolean vermifugado,
        Boolean necessidadesEspeciais,

        // Comportamento e compatibilidade
        Nivel nivelEnergia,
        Temperamento temperamento,
        Nivel nivelIndependencia,
        Nivel nivelVocalizacao,
        Boolean bomComCriancas,
        Boolean bomComCaes,
        Boolean bomComGatos,
        Boolean adaptadoApartamento,

        // Anúncio
        String resumo,
        String historia,
        StatusAnimal status,
        List<String> fotos,
        String cidade,
        String estado,
        DonoAnimalDTO dono,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {

    public static AnimalDetalheDTO from(Animal animal) {
        return new AnimalDetalheDTO(
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
                animal.getNivelIndependencia(),
                animal.getNivelVocalizacao(),
                animal.getBomComCriancas(),
                animal.getBomComCaes(),
                animal.getBomComGatos(),
                animal.getAdaptadoApartamento(),
                animal.getResumo(),
                animal.getHistoria(),
                animal.getStatus(),
                List.copyOf(animal.getFotos()),
                animal.getDono().getCity(),
                animal.getDono().getState(),
                DonoAnimalDTO.from(animal.getDono()),
                animal.getDataCriacao(),
                animal.getDataAtualizacao()
        );
    }
}
