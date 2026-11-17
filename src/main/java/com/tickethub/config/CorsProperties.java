package com.tickethub.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.cors.* : orígenes del front que pueden llamar a la API con cookies. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
