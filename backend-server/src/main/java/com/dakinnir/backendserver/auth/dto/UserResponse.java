package com.dakinnir.backendserver.auth.dto;

public record UserResponse(
        Long id,
        String username,
        String email
) { }
