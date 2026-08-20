package com.adocao.api.repository;

import com.adocao.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCnpj(String cnpj);

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);
}
