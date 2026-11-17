package com.tickethub.dto.response;

/** Respuesta del login: el token, cuántos segundos dura y quién es el usuario. */
public record AuthResponse(String token, String tokenType, long expiresIn, UserResponse user) {
}
