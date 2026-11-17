package com.tickethub.service.impl;

import com.tickethub.dto.response.UserResponse;
import com.tickethub.exception.ResourceNotFoundException;
import com.tickethub.mapper.UserMapper;
import com.tickethub.repository.UserRepository;
import com.tickethub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}
