package com.tickethub.mapper;

import com.tickethub.dto.response.AuthResponse;
import com.tickethub.entity.User;
import java.time.Instant;

public final class UserMapper {

    private UserMapper() {
    }

    public static AuthResponse toAuthResponse(User user, Instant expiresAt) {
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), expiresAt);
    }
}
