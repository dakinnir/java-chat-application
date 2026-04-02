package com.dakinnir.backendserver.user.dto;

import java.util.List;

public record PagedUsersResponse(
        List<UserResponse> users,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}