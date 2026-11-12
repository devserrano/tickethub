package com.tickethub.config;

import com.tickethub.enums.Role;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Crea los 4 usuarios semilla al arrancar. Flyway no lee variables de entorno, por eso
 * esto va aquí y no en un .sql: así la contraseña nunca queda escrita en el repo.
 * Es idempotente: si el correo ya existe no hace nada (no pisa contraseñas cambiadas).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final SeedProperties seed;

    @Override
    public void run(String... args) {
        // Encoder propio y no el bean de seguridad: así no dependemos del trabajo de auth y el hash es BCrypt estándar.
        var encoder = new BCryptPasswordEncoder();
        createIfMissing(encoder, "admin@tickethub.local", "Administrador", Role.ADMIN, seed.adminPassword());
        createIfMissing(encoder, "liberador@tickethub.local", "Liberador Demo", Role.LIBERADOR, seed.demoPassword());
        createIfMissing(encoder, "agente@tickethub.local", "Agente Demo", Role.AGENT, seed.demoPassword());
        createIfMissing(encoder, "usuario@tickethub.local", "Usuario Demo", Role.USER, seed.demoPassword());
    }

    private void createIfMissing(BCryptPasswordEncoder encoder, String email, String name, Role role, String password) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        if (exists != null && exists > 0) {
            return;
        }
        if (!StringUtils.hasText(password)) {
            log.warn("Usuario semilla {} omitido: falta ADMIN_PASSWORD / DEMO_PASSWORD en el .env", email);
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        jdbc.update(
                "INSERT INTO users (name, email, password_hash, role, active, created_at, updated_at) VALUES (?, ?, ?, ?, TRUE, ?, ?)",
                name, email, encoder.encode(password), role.name(), now, now);
        log.info("Usuario semilla creado: {} ({})", email, role);
    }
}
