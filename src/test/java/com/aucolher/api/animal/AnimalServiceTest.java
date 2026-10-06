package com.aucolher.api.animal;

import com.aucolher.api.animal.dto.AnimalRequestDTO;
import com.aucolher.api.animal.entity.*;
import com.aucolher.api.shared.exception.AcessoNegadoException;
import com.aucolher.api.shared.exception.BusinessException;
import com.aucolher.api.shared.exception.RecursoNaoEncontradoException;
import com.aucolher.api.usuario.UsuarioRepository;
import com.aucolher.api.usuario.entity.TipoUsuario;
import com.aucolher.api.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Regras de negócio da AnimalService, com o banco simulado (Mockito). */
@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    private static final String EMAIL_DONO = "dono@email.com";
    private static final String EMAIL_OUTRO = "outro@email.com";

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AnimalService animalService;

    @Test
    void cadastroExigeCidadeEUfNoPerfilDoDono() {
        Usuario semEndereco = usuario(1L, EMAIL_DONO);
        semEndereco.setCidade(null);
        when(usuarioRepository.findByEmail(EMAIL_DONO)).thenReturn(Optional.of(semEndereco));

        assertThatThrownBy(() -> animalService.cadastrar(EMAIL_DONO, requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cidade e a UF");
        verify(animalRepository, never()).save(any());
    }

    @Test
    void cadastroGravaOAnimalComODonoEAsFotosNaOrdem() {
        Usuario dono = usuario(1L, EMAIL_DONO);
        when(usuarioRepository.findByEmail(EMAIL_DONO)).thenReturn(Optional.of(dono));
        when(animalRepository.save(any(Animal.class))).thenAnswer(chamada -> chamada.getArgument(0));

        var resposta = animalService.cadastrar(EMAIL_DONO, requestValido());

        assertThat(resposta.dono().id()).isEqualTo(1L);
        assertThat(resposta.cidade()).isEqualTo("Araranguá");
        assertThat(resposta.fotos()).containsExactly("https://fotos/1.jpg", "https://fotos/2.jpg");
        assertThat(resposta.status()).isEqualTo(StatusAnimal.DISPONIVEL);
    }

    @Test
    void animalInativoNaoApareceParaQuemNaoEOdono() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animal(StatusAnimal.INATIVO)));

        assertThatThrownBy(() -> animalService.buscarDetalhe(10L, null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> animalService.buscarDetalhe(10L, EMAIL_OUTRO))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(animalService.buscarDetalhe(10L, EMAIL_DONO).id()).isEqualTo(10L);
    }

    @Test
    void soODonoEditaOAnimal() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animal(StatusAnimal.DISPONIVEL)));

        assertThatThrownBy(() -> animalService.editar(10L, EMAIL_OUTRO, requestValido()))
                .isInstanceOf(AcessoNegadoException.class);
        verify(animalRepository, never()).saveAndFlush(any());
    }

    @Test
    void animalAdotadoNaoPodeSerEditadoNemMudarDeStatus() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animal(StatusAnimal.ADOTADO)));

        assertThatThrownBy(() -> animalService.editar(10L, EMAIL_DONO, requestValido()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> animalService.alterarStatus(10L, EMAIL_DONO, StatusAnimal.DISPONIVEL))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> animalService.inativar(10L, EMAIL_DONO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void excluirTiraOAnimalDoArSemApagar() {
        Animal animal = animal(StatusAnimal.DISPONIVEL);
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animal));
        when(animalRepository.saveAndFlush(animal)).thenReturn(animal);

        animalService.inativar(10L, EMAIL_DONO);

        assertThat(animal.getStatus()).isEqualTo(StatusAnimal.INATIVO);
        verify(animalRepository, never()).delete(any(Animal.class));
    }

    @Test
    void perfilInexistenteDa404() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> animalService.listarDoPerfil(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ===================== Dados de teste =====================

    private static Usuario usuario(Long id, String email) {
        return Usuario.builder()
                .id(id)
                .nome("ONG Teste")
                .email(email)
                .tipoUsuario(TipoUsuario.ONG)
                .cidade("Araranguá")
                .estado("SC")
                .build();
    }

    private static Animal animal(StatusAnimal status) {
        Animal animal = new Animal();
        animal.setId(10L);
        animal.setDono(usuario(1L, EMAIL_DONO));
        animal.setStatus(status);
        animal.getFotos().add("https://fotos/1.jpg");
        return animal;
    }

    private static AnimalRequestDTO requestValido() {
        return new AnimalRequestDTO(
                "Thor", Especie.CACHORRO, "Vira-lata", Sexo.MACHO, 3, UnidadeIdade.ANOS, Porte.GRANDE,
                true, true, true, false,
                Nivel.ALTO, Temperamento.PROTETOR, Nivel.MODERADO, Nivel.MODERADO,
                true, true, false, false,
                "Protetor e brincalhão", "Thor foi resgatado ainda filhote.",
                List.of("https://fotos/1.jpg", "https://fotos/2.jpg"));
    }
}
