package com.tickethub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body de POST /api/v1/auth/register. Misma regla de contraseña que registro.html. */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 128)
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).+$", message = "Debe tener al menos una mayúscula y un número")
        String password
) {
}
