package com.tickethub.dto.response;

import com.tickethub.enums.Role;

/** Lo que la API muestra de un usuario. Nunca incluye el hash de la contraseña. */
public record UserResponse(Long id, String name, String email, Role role) {
}
