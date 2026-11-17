package com.tickethub.dto.response;

import java.time.Instant;

/**
 * Lo que el front necesita para pintar la sesión (nombre, iniciales, rol para data-roles).
 * El JWT NO viaja aquí: va en la cookie HttpOnly.
 */
public record AuthResponse(
        Long id,
        String name,
        String email,
        String role,
        Instant expiresAt
) {
}
