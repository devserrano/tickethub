package com.tickethub.security;

import com.tickethub.config.CorsProperties;
import com.tickethub.config.JwtProperties;
import com.tickethub.constants.ApiPaths;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** BCrypt: mismo formato que DataSeeder, así los usuarios semilla pueden iniciar sesión. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Evita el usuario en memoria que Spring crea por defecto; la autenticación es nuestra (JWT). */
    @Bean
    public UserDetailsService noDefaultUser() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, JwtProperties jwtProps)
            throws Exception {
        http
            // Sin sesión en el servidor: el estado vive en el JWT
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable())
            .cors(c -> {})

            // CSRF con cookie XSRF-TOKEN legible por JS; el front la reenvía en X-XSRF-TOKEN.
            // No se rota en cada petición (con JWT parecería un login nuevo cada vez y daría 403).
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                .ignoringRequestMatchers(ApiPaths.AUTH_LOGIN, ApiPaths.AUTH_REGISTER))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

            .headers(h -> h
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))

            .authorizeHttpRequests(auth -> auth
                // Front estático si se sirve desde el mismo backend (las páginas no traen datos)
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(ApiPaths.AUTH_LOGIN, ApiPaths.AUTH_REGISTER, ApiPaths.AUTH_LOGOUT).permitAll()
                // Rutas por rol, igual que los data-roles del menú del front
                .requestMatchers(ApiPaths.BASE + "/my-tickets/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(ApiPaths.BASE + "/review/**").hasAnyRole("LIBERADOR", "ADMIN")
                .requestMatchers(ApiPaths.BASE + "/inbox/**").hasAnyRole("AGENT", "ADMIN")
                .requestMatchers(ApiPaths.BASE + "/dashboard/**").hasAnyRole("LIBERADOR", "AGENT", "ADMIN")
                .requestMatchers(ApiPaths.BASE + "/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())

            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN)))

            .addFilterBefore(new JwtCookieFilter(jwtService, jwtProps.cookieName()),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Permite al front en otro puerto (Live Server, Vite) llamar a la API enviando la cookie. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(cors.allowedOrigins() == null ? List.of() : cors.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ApiPaths.BASE + "/**", config);
        return source;
    }
}
