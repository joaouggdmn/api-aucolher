package com.aucolher.api.animal;

import com.aucolher.api.animal.dto.AnimalRequestDTO;
import com.aucolher.api.animal.entity.*;
import com.aucolher.api.shared.exception.ForbiddenException;
import com.aucolher.api.shared.exception.BusinessException;
import com.aucolher.api.shared.exception.ResourceNotFoundException;
import com.aucolher.api.user.UserRepository;
import com.aucolher.api.user.entity.UserType;
import com.aucolher.api.user.entity.User;
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
    private UserRepository userRepository;

    @InjectMocks
    private AnimalService animalService;

    @Test
    void cadastroExigeCidadeEUfNoPerfilDoDono() {
        User semEndereco = usuario(1L, EMAIL_DONO);
        semEndereco.setCity(null);
        when(userRepository.findByEmail(EMAIL_DONO)).thenReturn(Optional.of(semEndereco));

        assertThatThrownBy(() -> animalService.cadastrar(EMAIL_DONO, requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cidade e a UF");
        verify(animalRepository, never()).save(any());
    }

    @Test
    void cadastroGravaOAnimalComODonoEAsFotosNaOrdem() {
        User dono = usuario(1L, EMAIL_DONO);
        when(userRepository.findByEmail(EMAIL_DONO)).thenReturn(Optional.of(dono));
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
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> animalService.buscarDetalhe(10L, EMAIL_OUTRO))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(animalService.buscarDetalhe(10L, EMAIL_DONO).id()).isEqualTo(10L);
    }

    @Test
    void soODonoEditaOAnimal() {
        when(animalRepository.findById(10L)).thenReturn(Optional.of(animal(StatusAnimal.DISPONIVEL)));

        assertThatThrownBy(() -> animalService.editar(10L, EMAIL_OUTRO, requestValido()))
                .isInstanceOf(ForbiddenException.class);
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
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> animalService.listarDoPerfil(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===================== Dados de teste =====================

    private static User usuario(Long id, String email) {
        return User.builder()
                .id(id)
                .name("ONG Teste")
                .email(email)
                .userType(UserType.NGO)
                .city("Araranguá")
                .state("SC")
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
