package com.adocao.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Edição do perfil pela própria conta ("Minha conta"). Serve aos dois tipos:
 * os campos de ONG (e o endereço completo) só são gravados em contas ONG —
 * a Service ignora o que um usuário comum mandar neles. E-mail, senha e CNPJ
 * não mudam por aqui.
 *
 * É uma substituição completa (PUT): campo opcional que não vier é apagado.
 * Mesma normalização do cadastro (ver {@link Sanitizador}).
 */
public record AtualizacaoPerfilDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
        String nome,

        @Size(max = FotoUrl.TAMANHO_MAXIMO, message = "A foto é grande demais")
        @Pattern(regexp = FotoUrl.FORMATO, message = "Foto em formato inválido")
        String fotoUrl,

        @Size(max = 500, message = "A bio deve ter no máximo 500 caracteres")
        String bio,

        // ===================== Perfil de ONG =====================

        @Email(message = "E-mail institucional em formato inválido")
        @Size(max = 150, message = "O e-mail institucional deve ter no máximo 150 caracteres")
        String emailInstitucional,

        @Pattern(regexp = "^[A-Za-z0-9._]{1,30}$", message = "Usuário do Instagram inválido")
        String instagram,

        @Pattern(regexp = "^[A-Za-z0-9_]{1,15}$", message = "Usuário do X (Twitter) inválido")
        String twitter,

        @Size(max = 255, message = "O link do Facebook deve ter no máximo 255 caracteres")
        @Pattern(
                regexp = "^https?://([\\w-]+\\.)*(facebook|fb)\\.com/.+$",
                message = "Informe o link da página no Facebook"
        )
        String facebook,

        @AnoFundacao
        Integer anoFundacao,

        @Valid
        @Size(max = 20, message = "A equipe pode ter no máximo 20 integrantes")
        List<MembroEquipeDTO> equipe,

        @Valid
        @Size(max = 20, message = "Cadastre no máximo 20 faixas de horário de visita")
        List<HorarioVisitaDTO> horariosVisita,

        // ===================== Endereço =====================
        // Obrigatório para ONG (conferido na Service); o usuário comum usa só CEP, cidade e UF

        @Pattern(regexp = "^\\d{8}$", message = "CEP em formato inválido")
        String cep,

        @Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres")
        String logradouro,

        @Size(max = 20, message = "O número deve ter no máximo 20 caracteres")
        String numero,

        @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres")
        String complemento,

        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres")
        String bairro,

        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String cidade,

        @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ser a sigla da UF (ex: SC)")
        String estado
) {

    public AtualizacaoPerfilDTO {
        nome = Sanitizador.texto(nome);
        fotoUrl = Sanitizador.texto(fotoUrl);
        bio = Sanitizador.texto(bio);
        emailInstitucional = Sanitizador.texto(emailInstitucional);
        instagram = Sanitizador.semArroba(instagram);
        twitter = Sanitizador.semArroba(twitter);
        facebook = Sanitizador.comProtocolo(facebook);
        equipe = equipe == null ? List.of() : equipe;
        horariosVisita = horariosVisita == null ? List.of() : horariosVisita;
        cep = Sanitizador.apenasDigitos(cep);
        logradouro = Sanitizador.texto(logradouro);
        numero = Sanitizador.texto(numero);
        complemento = Sanitizador.texto(complemento);
        bairro = Sanitizador.texto(bairro);
        cidade = Sanitizador.texto(cidade);
        estado = Sanitizador.sigla(estado);
    }
}
