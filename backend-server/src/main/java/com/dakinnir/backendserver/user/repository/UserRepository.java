package com.dakinnir.backendserver.user.repository;

import com.dakinnir.backendserver.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    // Term-based search by username or email (substring, case-insensitive)
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String usernameTerm, String emailTerm, Pageable pageable);
}
