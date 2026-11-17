package com.tickethub.controller;

import com.tickethub.config.JwtProperties;
import com.tickethub.constants.ApiPaths;
import com.tickethub.dto.request.LoginRequest;
import com.tickethub.dto.request.RegisterRequest;
import com.tickethub.dto.response.AuthResponse;
import com.tickethub.security.JwtService;
import com.tickethub.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtProperties jwtProps;

    /** Valida credenciales y deja el JWT en una cookie HttpOnly. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie(result.token(), jwtService.expiration()).toString())
                .body(result.body());
    }

    /** Alta abierta: la cuenta nace como USER. No inicia sesión automáticamente. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /** Borra la cookie del navegador. Requiere X-XSRF-TOKEN. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO).toString())
                .build();
    }

    /** Usuario de la sesión actual. Sin cookie válida SecurityConfig responde 401. */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal Claims claims) {
        return authService.currentUser(claims.getSubject(), claims.getExpiration().toInstant())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    private ResponseCookie sessionCookie(String value, Duration maxAge) {
        return ResponseCookie.from(jwtProps.cookieName(), value)
                .httpOnly(true)                  // JavaScript no puede leerla (mitiga robo por XSS)
                .secure(jwtProps.cookieSecure()) // true en producción (HTTPS)
                .sameSite("Strict")              // no se envía desde otros sitios (mitiga CSRF)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
