package com.tickethub.mapper;

import com.tickethub.dto.response.UserResponse;
import com.tickethub.entity.User;
import com.tickethub.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

/** Convierte entre la entidad y lo que se expone por la API. */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public UserResponse toResponse(AuthenticatedUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
