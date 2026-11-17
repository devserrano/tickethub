package com.tickethub.service.impl;

import com.tickethub.dto.request.LoginRequest;
import com.tickethub.dto.request.RegisterRequest;
import com.tickethub.dto.response.AuthResponse;
import com.tickethub.dto.response.UserResponse;
import com.tickethub.entity.User;
import com.tickethub.enums.Role;
import com.tickethub.exception.EmailAlreadyExistsException;
import com.tickethub.mapper.UserMapper;
import com.tickethub.repository.UserRepository;
import com.tickethub.security.AuthenticatedUser;
import com.tickethub.security.JwtService;
import com.tickethub.service.AuthService;
import com.tickethub.util.Emails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public AuthResponse login(LoginRequest request) {
        // si el correo no existe, la contraseña no coincide o la cuenta está desactivada, esto lanza una excepción
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(Emails.normalize(request.email()), request.password()));

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, TOKEN_TYPE, jwtService.expiresInSeconds(), userMapper.toResponse(user));
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = Emails.normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER) // nadie se registra con un rol más alto; solo un ADMIN los promueve
                .build();
        return userMapper.toResponse(userRepository.save(user));
    }
}
