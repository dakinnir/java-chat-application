package com.dakinnir.backendserver.user.service;

import com.dakinnir.backendserver.auth.dto.UserResponse;
import com.dakinnir.backendserver.user.dto.PagedUsersResponse;
import com.dakinnir.backendserver.user.model.User;
import com.dakinnir.backendserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    // Searching for a user by username or email (exact match)
    public UserResponse searchUser(String usernameOrEmailQuery) {
        User foundUser = userRepository.findByUsernameIgnoreCase(usernameOrEmailQuery)
                .or(() -> userRepository.findByEmailIgnoreCase(usernameOrEmailQuery))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmailQuery));

        return new UserResponse(foundUser.getId(), foundUser.getUsername(), foundUser.getEmail());
    }


    public PagedUsersResponse searchUsersByTerm(String term, int page, int size) {
        // Validate and adjust pagination parameters - no negative page numbers, and size between 1 and 20
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 20);

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<User> resultPage = userRepository
                .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);

        // Map Page<User> entities to UserResponse DTOs
        List<UserResponse> content = resultPage.getContent().stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getEmail()))
                .toList();

        return new PagedUsersResponse(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }
}
