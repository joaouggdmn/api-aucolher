package com.aucolher.api.animal.dto;

import com.aucolher.api.animal.entity.*;
import com.aucolher.api.shared.validation.PhotoUrl;
import com.aucolher.api.shared.validation.Sanitizer;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Cadastro (POST) e edição (PUT) de um animal — os dois recebem o anúncio
 * inteiro. Os campos seguem as etapas do formulário do frontend: dados
 * básicos, saúde, comportamento/compatibilidade e anúncio.
 *
 * Cidade e UF não vêm aqui: são as do perfil do dono.
 */
public record AnimalRequestDTO(

        // ===================== Dados básicos =====================

        @NotBlank(message = "O nome do animal é obrigatório")
        @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres")
        String nome,

        @NotNull(message = "Informe a espécie")
        Especie especie,

        @NotBlank(message = "A raça é obrigatória (use \"SRD\" ou \"Vira-lata\" se não souber)")
        @Size(max = 60, message = "A raça deve ter no máximo 60 caracteres")
        String raca,

        @NotNull(message = "Informe o sexo")
        Sexo sexo,

        @NotNull(message = "Informe a idade")
        Integer idadeValor,

        @NotNull(message = "Informe se a idade está em anos ou meses")
        UnidadeIdade idadeUnidade,

        @NotNull(message = "Informe o porte")
        Porte porte,

        // ===================== Saúde (não informado = não) =====================

        Boolean vacinado,
        Boolean castrado,
        Boolean vermifugado,
        Boolean necessidadesEspeciais,

        // ===================== Comportamento e compatibilidade =====================

        @NotNull(message = "Informe o nível de energia")
        Nivel nivelEnergia,

        @NotNull(message = "Informe o temperamento")
        Temperamento temperamento,

        @NotNull(message = "Informe o nível de independência")
        Nivel nivelIndependencia,

        @NotNull(message = "Informe o nível de vocalização")
        Nivel nivelVocalizacao,

        @NotNull(message = "Informe se o animal é bom com crianças")
        Boolean bomComCriancas,

        @NotNull(message = "Informe se o animal é bom com outros cães")
        Boolean bomComCaes,

        @NotNull(message = "Informe se o animal é bom com gatos")
        Boolean bomComGatos,

        @NotNull(message = "Informe se o animal vive bem em apartamento")
        Boolean adaptadoApartamento,

        // ===================== Anúncio =====================

        @NotBlank(message = "O resumo é obrigatório")
        @Size(max = 200, message = "O resumo deve ter no máximo 200 caracteres")
        String resumo,

        @NotBlank(message = "A história é obrigatória")
        @Size(max = 3000, message = "A história deve ter no máximo 3000 caracteres")
        String historia,

        @Size(min = 1, max = 4, message = "Envie de 1 a 4 fotos")
        List<
                @NotBlank(message = "Foto vazia")
                @Size(max = PhotoUrl.MAX_LENGTH, message = "Uma das fotos é grande demais")
                @Pattern(regexp = PhotoUrl.FORMAT, message = "Foto em formato inválido")
                String> fotos
) {

    public AnimalRequestDTO {
        nome = Sanitizer.text(nome);
        raca = Sanitizer.text(raca);
        resumo = Sanitizer.text(resumo);
        historia = Sanitizer.text(historia);
        vacinado = Boolean.TRUE.equals(vacinado);
        castrado = Boolean.TRUE.equals(castrado);
        vermifugado = Boolean.TRUE.equals(vermifugado);
        necessidadesEspeciais = Boolean.TRUE.equals(necessidadesEspeciais);
        fotos = fotos == null ? List.of() : fotos.stream().map(Sanitizer::text).toList();
    }

    /**
     * Filhotes são informados em meses (0 a 11) e os demais em anos (1 a 30),
     * como no formulário. É um método de validação, não um campo do JSON.
     */
    @AssertTrue(message = "Idade inválida: informe de 0 a 11 meses ou de 1 a 30 anos")
    public boolean isIdadeValida() {
        if (idadeValor == null || idadeUnidade == null) return true; // o @NotNull de cada campo já acusa
        return idadeUnidade == UnidadeIdade.MESES
                ? idadeValor >= 0 && idadeValor <= 11
                : idadeValor >= 1 && idadeValor <= 30;
    }
}
