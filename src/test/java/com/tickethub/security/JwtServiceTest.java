package com.tickethub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tickethub.config.JwtProperties;
import com.tickethub.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "un-secreto-de-prueba-con-mas-de-32-caracteres";

    private final AuthenticatedUser user =
            new AuthenticatedUser(7L, "Ana", "ana@tickethub.local", "hash", Role.AGENT, true);

    private JwtService service(String secret, long minutes) {
        return new JwtService(new JwtProperties(secret, minutes));
    }

    @Test
    @DisplayName("un token recién creado se valida y trae el correo, el id y el rol")
    void generatesAndParsesToken() {
        JwtService jwt = service(SECRET, 60);

        Claims claims = jwt.parse(jwt.generateToken(user));

        assertThat(claims.getSubject()).isEqualTo("ana@tickethub.local");
        assertThat(claims.get("uid", Long.class)).isEqualTo(7L);
        assertThat(claims.get("role", String.class)).isEqualTo("AGENT");
    }

    @Test
    @DisplayName("un token vencido se rechaza")
    void rejectsExpiredToken() {
        JwtService jwt = service(SECRET, -1); // venció hace un minuto

        assertThatThrownBy(() -> jwt.parse(jwt.generateToken(user)))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("un token firmado con otro secreto se rechaza")
    void rejectsTokenSignedWithAnotherSecret() {
        String token = service("otro-secreto-distinto-con-mas-de-32-caracteres", 60).generateToken(user);

        assertThatThrownBy(() -> service(SECRET, 60).parse(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("un token alterado se rechaza")
    void rejectsTamperedToken() {
        JwtService jwt = service(SECRET, 60);
        String[] parts = jwt.generateToken(user).split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x." + parts[2]; // se modifica el contenido

        assertThatThrownBy(() -> jwt.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("un texto que no es un token se rechaza")
    void rejectsGarbage() {
        assertThatThrownBy(() -> service(SECRET, 60).parse("esto-no-es-un-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("un secreto de menos de 32 caracteres impide arrancar")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> service("corto", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("expiresInSeconds refleja los minutos configurados")
    void reportsExpirationInSeconds() {
        assertThat(service(SECRET, 60).expiresInSeconds()).isEqualTo(3600);
    }
}
