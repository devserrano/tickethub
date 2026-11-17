package com.tickethub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.jwt.* de application.yml. El secreto es texto plano (JWT_SECRET del .env), mínimo 32 bytes. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes, String cookieName, boolean cookieSecure) {
}
