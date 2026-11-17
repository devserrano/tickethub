package com.tickethub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee la cookie con el JWT y, si es válido, registra al usuario (principal = Claims) con su rol
 * (ROLE_USER, ROLE_ADMIN...). Las reglas de SecurityConfig deciden después si la ruta se permite.
 * No es @Component a propósito: así no se registra también como filtro global del servlet.
 */
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final String cookieName;

    public JwtCookieFilter(JwtService jwt, String cookieName) {
        this.jwt = jwt;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = readCookie(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwt.parse(token);
                String role = claims.get(JwtService.CLAIM_ROLE, String.class);
                var auth = new UsernamePasswordAuthenticationToken(
                        claims, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                // Token inválido o expirado: sigue como anónima y la regla de la ruta responde 401
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (cookieName.equals(c.getName()) && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }
}
