package com.dakinnir.backendserver.user.controller;

import com.dakinnir.backendserver.auth.dto.UserResponse;
import com.dakinnir.backendserver.user.dto.PagedUsersResponse;
import com.dakinnir.backendserver.user.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Exact search: username or email must match exactly (case-insensitive)
    @GetMapping("/search/exact")
    public ResponseEntity<UserResponse> searchUserExact(
            @RequestParam("q")
            @NotBlank
            @Size(max = 255)
            String usernameOrEmailQuery
    ) {
        return ResponseEntity.ok(userService.searchUser(usernameOrEmailQuery));
    }

    // Term-based search: returns list of users whose username or email contains the term (case-insensitive)
    @GetMapping("/search")
    public ResponseEntity<PagedUsersResponse> searchUsersByTerm(
            @RequestParam("q")
            @NotBlank
            @Size(max = 255)
            String term,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userService.searchUsersByTerm(term, page, size));
    }

}
