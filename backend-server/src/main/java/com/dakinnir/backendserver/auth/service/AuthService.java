package com.dakinnir.backendserver.auth.service;

import com.dakinnir.backendserver.auth.dto.AuthResponse;
import com.dakinnir.backendserver.auth.dto.LoginRequest;
import com.dakinnir.backendserver.auth.dto.RegisterRequest;
import com.dakinnir.backendserver.auth.dto.UserResponse;
import com.dakinnir.backendserver.auth.exception.InvalidCredentialException;
import com.dakinnir.backendserver.auth.exception.InvalidRefreshTokenException;
import com.dakinnir.backendserver.auth.exception.UserAlreadyExistException;
import com.dakinnir.backendserver.auth.model.RefreshToken;
import com.dakinnir.backendserver.auth.repository.RefreshTokenRepository;
import com.dakinnir.backendserver.auth.security.JwtService;
import com.dakinnir.backendserver.user.model.User;
import com.dakinnir.backendserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    private static final SecureRandom secureRandom = new SecureRandom();

    public AuthResponse authenticate(LoginRequest request) {

        // User existence checked by the authentication manager
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            request.usernameOrEmail(),
                            request.password()
                    )
            );

            // If authentication is successful, retrieve the authenticated user's details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Look up the authenticated user by email first - if it fails, try username
            String principalUsername = userDetails.getUsername();
            User user = userRepository.findByEmailIgnoreCase(principalUsername)
                    .orElseGet(() -> userRepository.findByUsernameIgnoreCase(principalUsername)
                            .orElseThrow(() -> new InvalidCredentialException("Invalid username/email or password")));

            String accessToken = jwtService.generateAccessToken(userDetails);
            RefreshToken refreshToken = createAndPersistRefreshToken(user);

            UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail());

            return new AuthResponse(
                    userResponse,
                    accessToken,
                    refreshToken.getToken(),
                    "Bearer",
                    accessTokenExpirationMs / 1000,
                    refreshTokenExpirationDays * 24 * 60 * 60
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialException("Invalid username/email or password");
        }
    }

    public AuthResponse createUser(RegisterRequest request) {
        boolean userExists = userRepository.existsByEmailIgnoreCase(request.email()) ||
                userRepository.existsByUsernameIgnoreCase(request.username());
        if (userExists) {
            throw new UserAlreadyExistException("User with the given username or email already exists");
        }

        // Hash the password before saving the user
        String encodedPassword = passwordEncoder.encode(request.password());

        // Account should be enabled by default - No email verification
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(encodedPassword)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(saved.getEmail())
                .password(saved.getPasswordHash())
                .authorities("ROLE_USER")
                .accountLocked(false)
                .disabled(!saved.isEnabled())
                .build();

        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = createAndPersistRefreshToken(saved);

        UserResponse userResponse = new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail());

        return new AuthResponse(
                userResponse,
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                accessTokenExpirationMs / 1000,
                refreshTokenExpirationDays * 24 * 60 * 60
        );
    }

    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        // User must re-authenticate to get a new refresh token if refresh token is expired or revoked
        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        // Revoke old token and create a new one
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .accountLocked(false)
                .disabled(!user.isEnabled())
                .build();

        // Generate new access token and refresh token
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken newRefreshToken = createAndPersistRefreshToken(user);

        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail());

        return new AuthResponse(
                userResponse,
                newAccessToken,
                newRefreshToken.getToken(),
                "Bearer",
                accessTokenExpirationMs / 1000,
                refreshTokenExpirationDays * 24 * 60 * 60
        );
    }

    private RefreshToken createAndPersistRefreshToken(User user) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant expiryDate = Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}
