package com.dakinnir.backendserver.user.controller;

import com.dakinnir.backendserver.user.dto.UserResponse;
import com.dakinnir.backendserver.user.dto.PagedUsersResponse;
import com.dakinnir.backendserver.user.model.User;
import com.dakinnir.backendserver.user.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        // Testing endpoint to return all users -
        return ResponseEntity.ok(userService.getUsers());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.ok(userService.getUserProfile());
    }

    // Exact search: username or email must match exactly (case-insensitive)
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(userService.getUser(id));
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
