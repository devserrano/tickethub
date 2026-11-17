package com.tickethub.service;

import com.tickethub.dto.request.LoginRequest;
import com.tickethub.dto.request.RegisterRequest;
import com.tickethub.dto.response.AuthResponse;
import com.tickethub.dto.response.UserResponse;

public interface AuthService {

    /** Valida correo y contraseña y devuelve un token JWT. */
    AuthResponse login(LoginRequest request);

    /** Crea una cuenta nueva. Todo registro nace con rol USER. */
    UserResponse register(RegisterRequest request);
}
