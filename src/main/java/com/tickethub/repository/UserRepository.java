package com.tickethub.repository;

import com.tickethub.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla `users`. Spring Data genera la implementación sola:
 * el nombre del método ya describe la consulta (findByEmail → WHERE email = ?).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
