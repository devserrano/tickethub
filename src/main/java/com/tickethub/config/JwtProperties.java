package com.tickethub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** `app.jwt.*`: el secreto con el que se firman los tokens y cuánto duran. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
