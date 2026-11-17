package com.tickethub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades generales bajo `app:` en application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendUrl) {
}
