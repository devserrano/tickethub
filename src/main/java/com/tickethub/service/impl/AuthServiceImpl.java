package com.tickethub.service.impl;

import com.tickethub.dto.request.LoginRequest;
import com.tickethub.dto.request.RegisterRequest;
import com.tickethub.dto.response.AuthResponse;
import com.tickethub.entity.User;
import com.tickethub.enums.Role;
import com.tickethub.exception.EmailAlreadyUsedException;
import com.tickethub.mapper.UserMapper;
import com.tickethub.repository.UserRepository;
import com.tickethub.security.JwtService;
import com.tickethub.service.AuthService;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    /**
     * Hash falso que se compara cuando el correo no existe, para que la respuesta tarde lo mismo
     * y no se pueda averiguar qué correos están registrados.
     */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        Optional<User> found = users.findByEmailIgnoreCase(normalize(request.email()));
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordOk = encoder.matches(request.password(), hash);

        if (found.isEmpty() || !passwordOk || !found.get().isActive()) {
            throw new BadCredentialsException("Credenciales inválidas"); // mismo error en todos los casos
        }
        User user = found.get();
        Instant expiresAt = Instant.now().plus(jwt.expiration());
        return new LoginResult(jwt.generate(user, expiresAt), UserMapper.toAuthResponse(user, expiresAt));
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = users.save(new User(request.name().trim(), email, encoder.encode(request.password()), Role.USER));
        return UserMapper.toAuthResponse(user, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthResponse> currentUser(String email, Instant expiresAt) {
        return users.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .map(u -> UserMapper.toAuthResponse(u, expiresAt));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
