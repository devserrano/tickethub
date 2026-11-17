package com.tickethub.dto.request;

import com.tickethub.util.Emails;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password) {

    /** Se limpia antes de validar: un espacio al final del correo no debe hacer fallar el login. */
    public LoginRequest {
        email = Emails.normalize(email);
    }
}
