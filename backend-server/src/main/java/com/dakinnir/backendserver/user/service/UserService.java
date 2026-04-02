package com.dakinnir.backendserver.user.service;

import com.dakinnir.backendserver.user.dto.UserResponse;
import com.dakinnir.backendserver.user.dto.PagedUsersResponse;
import com.dakinnir.backendserver.user.exception.NoAuthenticatedUserException;
import com.dakinnir.backendserver.user.model.User;
import com.dakinnir.backendserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<User> getUsers() {
        return userRepository.findAll();
    }
    public UserResponse getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String username = authentication.getName();
            User foundUser = userRepository.findByEmailIgnoreCase(username)
                    .or(() -> userRepository.findByUsernameIgnoreCase(username)).orElse(null);

            if (foundUser == null) {
                throw new NoAuthenticatedUserException("Authenticated user not found");
            } else {
                return new UserResponse(foundUser.getId(), foundUser.getUsername(), foundUser.getEmail(), foundUser.getCreatedAt());
            }
        } else {
            throw new NoAuthenticatedUserException("Authenticated user not found");
        }
    }

    // Searching for a user by username or email (exact match)
    public UserResponse getUser(Long id) {
        User foundUser = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        // User found, return the response DTO
        return new UserResponse(
                foundUser.getId(),
                foundUser.getUsername(),
                foundUser.getEmail(),
                foundUser.getCreatedAt());
    }

    public PagedUsersResponse searchUsersByTerm(String term, int page, int size) {
        // Get the authenticated user to exclude from search results
        User currentUser = getAuthenticatedUser();
        Long excludedId = currentUser.getId();

        // Validate and adjust pagination parameters - no negative page numbers, and size between 1 and 20
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 20);

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<User> resultPage = userRepository
                .findByIdNotAndUsernameContainingIgnoreCaseOrIdNotAndEmailContainingIgnoreCase(
                        excludedId,
                        term,
                        excludedId,
                        term,
                        pageable
                );

        // Map Page<User> entities to UserResponse
        List<UserResponse> content = resultPage.getContent().stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt()))
                .toList();

        return new PagedUsersResponse(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    // Helper method to get the currently authenticated user from the security context
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NoAuthenticatedUserException("Authenticated user not found");
        }

        String principalName = authentication.getName();

        return userRepository.findByEmailIgnoreCase(principalName)
                .or(() -> userRepository.findByUsernameIgnoreCase(principalName))
                .orElseThrow(() -> new NoAuthenticatedUserException("Authenticated user not found"));
    }

}
