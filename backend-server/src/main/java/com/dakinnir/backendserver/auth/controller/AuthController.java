package com.dakinnir.backendserver.auth.controller;

import com.dakinnir.backendserver.auth.dto.AuthResponse;
import com.dakinnir.backendserver.auth.dto.LoginRequest;
import com.dakinnir.backendserver.auth.dto.RefreshTokenRequest;
import com.dakinnir.backendserver.auth.dto.RegisterRequest;
import com.dakinnir.backendserver.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.authenticate(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
