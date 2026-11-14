package com.storyforge.controller;

import com.storyforge.domain.user.AuthDtos.AuthResponse;
import com.storyforge.domain.user.AuthDtos.LoginRequest;
import com.storyforge.domain.user.AuthDtos.LogoutRequest;
import com.storyforge.domain.user.AuthDtos.RefreshRequest;
import com.storyforge.domain.user.AuthDtos.RegisterRequest;
import com.storyforge.domain.user.AuthDtos.UserResponse;
import com.storyforge.service.auth.AuthService;
import com.storyforge.service.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) LogoutRequest request) {
        if (request != null) {
            authService.logout(request.refreshToken());
        }
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.me(currentUser.id());
    }
}
