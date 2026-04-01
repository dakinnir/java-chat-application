# Realtime Chat – Backend

Backend service for a realtime chat application, built with Spring Boot and PostgreSQL.  
This backend service will handle user authentication, 1:1 and group chat features.

## Technologies

- **Language:** Java 21
- **Framework:** Spring Boot 4
- **Security:** Spring Security 7, JWT (JJWT)
- **Persistence:** Spring Data JPA, PostgreSQL
- **Validation:** Jakarta Bean Validation
- **Build:** Gradle
- **Utilities:** Lombok

## Core Features (Current)

### User & Auth

- User model with:
    - Unique `username` and `email`
    - BCrypt-hashed password
    - Role-based access (`USER`, `ADMIN`)
    - Enabled/disabled flag, timestamps
- Email/username-based login:
    - Login with either email or username
    - Password verification via `AuthenticationManager` + `PasswordEncoder`
- JWT-based authentication:
    - Short-lived **access tokens** (JWT, HS256)
    - Long-lived, opaque **refresh tokens** stored in the database
    - Token rotation on refresh (old refresh token is revoked)
- Auth endpoints:
    - `POST /api/auth/register` – register with username, email, password
    - `POST /api/auth/login` – login with username/email + password
    - `POST /api/auth/refresh` – exchange refresh token for new access + refresh tokens
- Global error handling:
    - Consistent JSON responses for:
        - Validation errors
        - Invalid credentials
        - Duplicate users
        - Invalid/expired refresh tokens

### Configuration

- Main config in `application.yml`:
    - PostgreSQL datasource
    - JPA/Hibernate settings
    - JWT settings:
        - `app.security.jwt.secret` (Base64 key, override via `APP_SECURITY_JWT_SECRET`)
        - Access token lifetime (ms)
        - Refresh token lifetime (days)

## Current Development Status

- User model, repositories, and PostgreSQL integration
- Secure login & registration with BCrypt and JWT access tokens
- Refresh token flow with rotation and revocation
- Global validation & exception handling
- Next steps:
    - User search (by email) to start chats
    - 1:1 and group chat entities & APIs
    - WebSocket setup for realtime messaging

## Running (Local)

From the backend project directory:

```bash
# Set a secure JWT secret (recommended)
export APP_SECURITY_JWT_SECRET=$(openssl rand -base64 32)

# Run tests
./gradlew test

# Start the application
./gradlew bootRun
```
