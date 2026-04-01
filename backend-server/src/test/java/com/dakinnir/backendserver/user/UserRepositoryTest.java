package com.dakinnir.backendserver.user;

import com.dakinnir.backendserver.user.model.Role;
import com.dakinnir.backendserver.user.model.User;
import com.dakinnir.backendserver.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("should persist user with default role and timestamps")
    void persistUser_withDefaultRoleAndTimestamps() {
        User user = User.builder()
                .username("testuser")
                .email("test@user.com")
                .passwordHash("testpassword")
                .build();

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getRoles()).containsExactly(Role.USER);

        Optional<User> reloaded = userRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getEmail()).isEqualTo("test@user.com");
    }
}
