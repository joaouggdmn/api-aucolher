package com.aucolher.api.animal.dto;

import com.aucolher.api.animal.entity.*;
import com.aucolher.api.shared.validation.Sanitizador;

import java.util.List;

/**
 * Filtros da listagem pública, lidos da query string. Todos opcionais; os de
 * múltipla escolha aceitam o parâmetro repetido, como os checkboxes do frontend:
 * /api/animais?especie=CACHORRO&especie=GATO&porte=PEQUENO&cidade=Araranguá
 *
 * `busca` procura o texto no nome, na raça e na cidade do animal.
 */
public record AnimalFiltroDTO(
        String busca,
        List<Especie> especie,
        List<Porte> porte,
        List<Sexo> sexo,
        List<FaixaEtaria> faixaEtaria,
        List<Nivel> nivelEnergia,
        List<Temperamento> temperamento,
        Boolean necessidadesEspeciais,
        String cidade,
        String estado
) {

    public AnimalFiltroDTO {
        busca = Sanitizador.texto(busca);
        cidade = Sanitizador.texto(cidade);
        estado = Sanitizador.sigla(estado);
    }
}
