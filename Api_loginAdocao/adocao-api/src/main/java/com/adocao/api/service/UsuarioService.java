package com.adocao.api.service;

import com.adocao.api.dto.AtualizacaoPerfilDTO;
import com.adocao.api.dto.UsuarioResponseDTO;
import com.adocao.api.entity.HorarioVisita;
import com.adocao.api.entity.MembroEquipe;
import com.adocao.api.entity.TipoUsuario;
import com.adocao.api.entity.Usuario;
import com.adocao.api.exception.BusinessException;
import com.adocao.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Regras do perfil da própria conta ("Minha conta"). Assim como na
 * AuthService, o DTO chega já validado e normalizado.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Perfil completo da conta, com equipe e horários. Transacional porque as
     * coleções são lazy — quem chama de fora de uma transação (o handler do
     * login Google) não conseguiria carregá-las.
     */
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPerfil(String email) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioResponseDTO::from)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }

    /**
     * Substitui o perfil editável da conta autenticada. Usuário comum grava só
     * nome, foto, bio e CEP/cidade/UF; ONG grava também o perfil institucional
     * e o endereço completo, que continua obrigatório.
     */
    @Transactional
    public UsuarioResponseDTO atualizarPerfil(String email, AtualizacaoPerfilDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        usuario.setNome(dto.nome());
        usuario.setFotoUrl(dto.fotoUrl());
        usuario.setBio(dto.bio());
        usuario.setCep(dto.cep());
        usuario.setCidade(dto.cidade());
        usuario.setEstado(dto.estado());

        if (usuario.getTipoUsuario() == TipoUsuario.ONG) {
            atualizarPerfilOng(usuario, dto);
        }

        return UsuarioResponseDTO.from(usuario);
    }

    private void atualizarPerfilOng(Usuario usuario, AtualizacaoPerfilDTO dto) {
        boolean enderecoIncompleto = Stream.of(dto.cep(), dto.logradouro(), dto.numero(), dto.bairro(), dto.cidade(), dto.estado())
                .anyMatch(Objects::isNull);
        if (enderecoIncompleto) {
            throw new BusinessException("O endereço da ONG é obrigatório: informe CEP, logradouro, número, bairro, cidade e UF");
        }

        usuario.setEmailInstitucional(dto.emailInstitucional());
        usuario.setInstagram(dto.instagram());
        usuario.setTwitter(dto.twitter());
        usuario.setFacebook(dto.facebook());
        usuario.setAnoFundacao(dto.anoFundacao());
        usuario.setLogradouro(dto.logradouro());
        usuario.setNumero(dto.numero());
        usuario.setComplemento(dto.complemento());
        usuario.setBairro(dto.bairro());

        // A ordem da lista vira a coluna "ordem" (@OrderColumn)
        usuario.getEquipe().clear();
        dto.equipe().forEach(membro -> usuario.getEquipe().add(new MembroEquipe(membro.nome(), membro.funcao())));

        usuario.getHorariosVisita().clear();
        dto.horariosVisita().forEach(faixa -> usuario.getHorariosVisita().add(new HorarioVisita(faixa.dias(), faixa.horario())));
    }
}
