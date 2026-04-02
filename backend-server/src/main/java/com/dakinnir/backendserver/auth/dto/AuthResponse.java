package com.dakinnir.backendserver.auth.dto;

import com.dakinnir.backendserver.user.dto.UserResponse;

public record AuthResponse(
        UserResponse user,
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds
) { }

