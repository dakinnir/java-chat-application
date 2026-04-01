package com.dakinnir.backendserver.auth.dto;

public record AuthResponse(
        UserResponse user,
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds
) { }

