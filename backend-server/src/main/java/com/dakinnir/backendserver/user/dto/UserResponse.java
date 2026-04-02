package com.dakinnir.backendserver.user.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        Instant createdAt
) { }
