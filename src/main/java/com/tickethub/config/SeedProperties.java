package com.tickethub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Contraseñas de los usuarios semilla; vienen del .env (app.seed.* en application.yml). */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(String adminPassword, String demoPassword) {
}
