package com.tickethub.dto.request;

import com.tickethub.util.Emails;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String name,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo admite máximo 150 caracteres")
        String email,

        // BCrypt solo lee los primeros 72 bytes, por eso ese es el máximo
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password) {

    /** Se limpian antes de validar: los espacios sobrantes no deben hacer fallar el registro. */
    public RegisterRequest {
        name = name == null ? null : name.trim();
        email = Emails.normalize(email);
    }
}
