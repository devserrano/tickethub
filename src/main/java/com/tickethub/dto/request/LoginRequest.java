package com.tickethub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de POST /api/v1/auth/login. */
public record LoginRequest(
        @NotBlank @Email @Size(max = 150) String email,
        // El límite evita textos enormes que saturen el hashing (DoS)
        @NotBlank @Size(max = 128) String password
) {
}
