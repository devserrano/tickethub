package com.tickethub.service;

import com.tickethub.dto.request.LoginRequest;
import com.tickethub.dto.request.RegisterRequest;
import com.tickethub.dto.response.AuthResponse;
import java.time.Instant;
import java.util.Optional;

public interface AuthService {

    /** Resultado interno del login: el token va a la cookie y el body al JSON. */
    record LoginResult(String token, AuthResponse body) {
    }

    LoginResult login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    Optional<AuthResponse> currentUser(String email, Instant expiresAt);
}
