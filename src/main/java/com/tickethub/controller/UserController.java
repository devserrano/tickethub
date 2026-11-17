package com.tickethub.controller;

import com.tickethub.constants.ApiPaths;
import com.tickethub.dto.response.UserResponse;
import com.tickethub.security.AuthenticatedUser;
import com.tickethub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.USERS)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Quién soy según mi token. El front lo usa al abrir la app para saber si la sesión sigue viva. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.getById(principal.getId());
    }
}
