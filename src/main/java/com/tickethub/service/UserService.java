package com.tickethub.service;

import com.tickethub.dto.response.UserResponse;

public interface UserService {

    UserResponse getById(Long id);
}
