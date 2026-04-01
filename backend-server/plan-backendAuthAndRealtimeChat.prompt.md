## Plan: Backend Auth and Realtime Chat (Spring Boot 4, Java 21)

Spring Boot 4 backend for a realtime chat application with JWT access/refresh tokens, stateless security, PostgreSQL persistence, and WebSocket-based realtime messaging. The system uses auto-incrementing numeric primary keys (identity columns), Java 21 idioms (records, pattern matching), and Spring Security with proper configuration for HTTP and WebSocket endpoints.

### Steps

1. Define Domain Model and Persistence Layer  
2. Implement DTOs, Mappers, and Validation  
3. Configure Security, JWT, and Authentication Flows  
4. Implement REST Controllers for Auth and Chat  
5. Implement WebSocket Configuration and Messaging Flow  
6. Integrate Persistence with WebSockets and Message Delivery Semantics  
7. Configure Application Properties and Profiles  
8. Add Tests (Unit, Slice, and Basic Integration)  

---

### 1. Domain Model and Persistence Layer

**1.1. Base Entity and ID Strategy**

- Use auto-incrementing numeric primary keys (e.g., `Long` mapped to PostgreSQL `BIGSERIAL`/`IDENTITY`) for all persistent entities.  
- Configure Hibernate/JPA to generate identifiers via `@GeneratedValue(strategy = GenerationType.IDENTITY)` for simplicity.  
- No inheritance hierarchy is strictly required, but if convenient, define a `BaseEntity` mapped superclass with `id` and `createdAt`/`updatedAt` audit fields.

**1.2. User Entity**

- Package: `com.dakinnir.backendserver.user` (or similar).  
- Entity: `User` with fields:
  - `Long id`
  - `String username` (unique, case-insensitive constraint at DB level if desired)
  - `String email` (unique)
  - `String passwordHash`
  - `Set<Role> roles` (simple role model; use enum `Role` mapped as string or a join table)
  - `Instant createdAt`
  - `Instant updatedAt`
  - Optional: `boolean enabled`, `boolean locked` for account status
- Constraints:
  - Unique indexes for `username` and `email`.
- JPA mappings:
  - Annotate with `@Entity`, `@Table` with unique constraints.
  - For roles, use `@ElementCollection(fetch = EAGER)`, `@Enumerated(EnumType.STRING)`, or a many-to-many association to a `Role` entity.
- Implement `UserRepository` extending `JpaRepository<User, Long>` with query methods:
  - `Optional<User> findByUsernameIgnoreCase(String username)`
  - `Optional<User> findByEmailIgnoreCase(String email)`
  - `boolean existsByUsernameIgnoreCase(String username)`
  - `boolean existsByEmailIgnoreCase(String email)`

**1.3. Role Model**

- `enum Role { USER, ADMIN }` (extendable).  
- Store roles as strings in DB.  
- When mapping to Spring Security, prefix with `"ROLE_"` as needed.

**1.4. Chat Room Entity**

- Package: `com.dakinnir.backendserver.chat`.  
- Entity: `ChatRoom` with fields:
  - `Long id`
  - `String name` (unique or scoped per type)
  - Optional: `String type` (e.g., `"DIRECT"`, `"GROUP"`) or `ChatRoomType` enum
  - `Instant createdAt`
- Relationships:
  - Many-to-many `Set<User> participants` or a separate join entity `ChatRoomParticipant` for extensibility (roles per room, join time).  
- JPA:
  - If using a direct many-to-many:
    - `@ManyToMany`
    - `@JoinTable(name = "chat_room_participants", joinColumns = ..., inverseJoinColumns = ...)`
  - Unique constraint for rooms if you want 1:1 direct chats (e.g., combination of participants).

**1.5. Chat Message Entity**

- Entity: `ChatMessage` with fields:
  - `Long id`
  - `ChatRoom room`
  - `User sender`
  - `String content` (text body)
  - Optional: `String contentType` (`"TEXT"`, `"IMAGE"`, etc.; keep as simple text for now)
  - `Instant sentAt`
  - Optional: `boolean edited`, `Instant editedAt`
- JPA:
  - `@ManyToOne(optional = false)` for `room` and `sender`.
  - Indexes on `room_id`, `sent_at` for fast retrieval.
- Repository:
  - `ChatMessageRepository` extending `JpaRepository<ChatMessage, Long>` with paged query methods:
    - `Page<ChatMessage> findByRoomIdOrderBySentAtAsc(Long roomId, Pageable pageable)`
    - Optionally: `Slice<ChatMessage> findByRoomIdAndSentAtBeforeOrderBySentAtDesc(...)` for "load earlier" pagination.

**1.6. Refresh Token Model**

- Two design options:
  - (A) Stateless refresh (store all data in signed refresh JWT, no DB persistence).
  - (B) Stateful refresh (store refresh tokens in DB to support explicit revocation/rotation checking).
- Choose (B) for more control:
  - Entity: `RefreshToken` with fields:
    - `Long id`
    - `User user`
    - `String token` (secure random string or JWT ID/ref; index this)
    - `Instant expiresAt`
    - `Instant createdAt`
    - `boolean revoked`
  - Repository: `RefreshTokenRepository` with:
    - `Optional<RefreshToken> findByToken(String token)`
    - `void deleteByUserId(Long userId)` or batch revocation methods.
- Alternatively, treat the DB `RefreshToken` as a record keyed by a `jti` from the JWT and store only that ID.

---

### 2. DTOs, Mappers, and Validation

**2.1. Use Java 21 Records for DTOs**

- Define request/response payloads as `record` types in `dto` subpackages (e.g., `auth.dto`, `chat.dto`).

**2.2. Auth DTOs**

- `RegisterRequest`:
  - Fields: `String username`, `String email`, `String password`.
  - Annotations:
    - `@NotBlank` for required fields.
    - `@Email` for email.
    - Appropriate size constraints (e.g., `@Size(min=3, max=32)` for username, `@Size(min=8, max=72)` for password).
- `LoginRequest`:
  - Fields: `String usernameOrEmail`, `String password`.
  - Validation with `@NotBlank`.
- `TokenResponse`:
  - Fields: `String accessToken`, `String refreshToken`, `String tokenType` (`"Bearer"`), optional `long expiresIn` (seconds for access token).
- `RefreshTokenRequest`:
  - Field: `String refreshToken`.
  - `@NotBlank`.

**2.3. User DTOs**

- `UserResponse`:
  - Fields: `Long id`, `String username`, `String email`, `Set<String> roles`.
- Optionally, `UserProfileResponse` record for extended user info.

**2.4. Chat DTOs**

- `CreateRoomRequest`:
  - Fields: `String name`, optional set of participant IDs for group rooms.
  - Validation: `@NotBlank` on `name`.
- `ChatRoomResponse`:
  - Fields: `Long id`, `String name`, optional `Set<UserResponse> participants`.
- `SendMessageRequest`:
  - Fields: `Long roomId`, `String content`.
  - `@NotNull` and `@NotBlank`.
- `ChatMessageResponse`:
  - Fields:
    - `Long id`
    - `Long roomId`
    - `Long senderId`
    - `String senderUsername`
    - `String content`
    - `Instant sentAt`
- `MessagePageResponse`:
  - Fields: `List<ChatMessageResponse> messages`, `boolean hasNext`, `int page`, `int size`.

**2.5. WebSocket Payload DTOs**

- `InboundChatMessage` (from client via STOMP / WebSocket):
  - Fields: `Long roomId`, `String content`.
- `OutboundChatMessage` (to clients):
  - Same as `ChatMessageResponse` or reference that record.

**2.6. Mapping Layer**

- Create mapper classes (`UserMapper`, `ChatMapper`) or simple static mapper methods to convert:
  - `User` -> `UserResponse`
  - `ChatRoom` -> `ChatRoomResponse`
  - `ChatMessage` -> `ChatMessageResponse`
- Use Java 21 pattern matching and `switch` where helpful, but keep mapping straightforward.

---

### 3. Security, JWT, and Authentication Flows

**3.1. Password Encoding**

- Configure a `PasswordEncoder` bean using `BCryptPasswordEncoder` (or Argon2 if preferred and supported).
- During registration:
  - Hash password and store only the hash.
- During authentication:
  - Use `passwordEncoder.matches(rawPassword, encodedPasswordFromDB)`.

**3.2. Spring Security Configuration**

- Define a `SecurityConfig` class in `com.dakinnir.backendserver.config`:
  - Expose `SecurityFilterChain` bean configuring:
    - CSRF: disable for stateless REST, but handle CSRF implications for browser-based clients; for this API-only backend, fully stateless is expected.
    - Session management: stateless (`SessionCreationPolicy.STATELESS`).
    - Authorize requests:
      - Permit all for `/api/auth/**` (register, login, refresh-token, maybe health).
      - Permit health check endpoints like `/actuator/health` if actuator is used (optional).
      - Require authentication for `/api/chat/**`.
      - Configure WebSocket handshake and STOMP endpoints:
        - Allow handshake endpoints with appropriate security; actual messaging will be secured via JWT.
    - Add custom JWT authentication filter before `UsernamePasswordAuthenticationFilter`.
  - Configure CORS with allowed origins, methods, and headers (at least for development).

**3.3. UserDetails and Authentication Provider**

- Implement a custom `UserDetailsService`:
  - Loads `User` by username or email (for login processes that allow both).
  - Maps roles to `GrantedAuthority` instances.
- Implement an `AuthenticationProvider` or rely on default `DaoAuthenticationProvider` using:
  - Custom `UserDetailsService`.
  - `PasswordEncoder`.

**3.4. JWT Token Service**

- Configure `JwtService` or `TokenService`:
  - Use `io.jsonwebtoken` or Spring Security JWT support (depending on dependencies); ensure compatibility with Spring Boot 4.
  - Properties:
    - Secret key (HMAC) or key pair (RSA/ECDSA) loaded from `application.properties`.
    - Access token validity: e.g., 15 minutes.
    - Refresh token validity: e.g., 7–30 days.
  - Methods:
    - `String generateAccessToken(User user)`:
      - Subject: user ID or username.
      - Claims: roles, username, maybe email.
      - Short expiration.
    - `String generateRefreshToken(User user)` or `generateRefreshToken(RefreshToken refreshToken)`:
      - Longer expiration.
      - Could embed a `jti` to link to DB record.
    - `Optional<JwtClaims> parseAndValidate(String token)`:
      - Validate signature and expiration.
      - Extract subject and claims.

**3.5. JWT Authentication Filter**

- Implement `JwtAuthenticationFilter` extending `OncePerRequestFilter`:
  - Reads `Authorization` header (`Bearer <access-token>`).
  - Parses and validates JWT.
  - Loads user by ID/username from claims.
  - Populates `SecurityContext` with `UsernamePasswordAuthenticationToken` having authorities.
  - Skips filter for endpoints that are publicly accessible (e.g., `/api/auth/**`).
- Register this filter in `SecurityFilterChain` before `UsernamePasswordAuthenticationFilter`.

**3.6. Auth Service**

- `AuthService` responsibilities:
  - Registration:
    - Validate uniqueness of username/email via `UserRepository`.
    - Encode password and persist `User`.
    - Optionally directly issue access + refresh tokens upon successful registration.
  - Login:
    - Accept username or email.
    - Look up user, verify password.
    - Generate access and refresh tokens.
    - Persist refresh token entity if using DB-backed refresh tokens.
  - Refresh:
    - Validate refresh token (JWT and/or DB record).
    - Check `revoked` and expiration status.
    - If valid, issue new access token (and optionally a rotated refresh token).
  - Logout / revoke:
    - Optionally revoke refresh token(s) for a user by marking them as revoked.

**3.7. Token Revocation Strategy**

- If using DB-backed refresh tokens:
  - When a refresh token is used:
    - Optionally implement rotation: mark current token as revoked and issue a new one.
  - On logout:
    - Mark all tokens for the user as revoked or delete them.
- Access tokens remain stateless and short-lived; revocation typically not persisted (or handled by token blacklisting if necessary, which can be added later).

---

### 4. REST Controllers for Auth and Chat

**4.1. AuthController**

- Base path: `/api/auth`.
- Endpoints:
  - `POST /api/auth/register`:
    - Request body: `RegisterRequest`.
    - Response: `TokenResponse` or `UserResponse` + tokens; choose `TokenResponse` containing tokens and optionally embed user info if needed.
    - Validations:
      - Return `400 Bad Request` for invalid input.
      - Return `409 Conflict` for duplicate username/email.
  - `POST /api/auth/login`:
    - Request: `LoginRequest`.
    - Response: `TokenResponse`.
    - Errors:
      - `401 Unauthorized` for invalid credentials.
  - `POST /api/auth/refresh`:
    - Request: `RefreshTokenRequest`.
    - Response: `TokenResponse` (new access token, possibly new refresh token).
    - Errors:
      - `401 Unauthorized` or `403 Forbidden` for invalid/expired token.
  - Optional: `POST /api/auth/logout`:
    - Request: body or token info to revoke.
    - Behavior: mark refresh tokens revoked.
- Use `@Validated` on controller methods or class to trigger bean validation.

**4.2. Chat REST Controller**

- Base path: `/api/chat`.
- Endpoints (all require authentication):
  - `POST /api/chat/rooms`:
    - Request: `CreateRoomRequest`.
    - Response: `ChatRoomResponse`.
    - Behavior:
      - Create and persist chat room.
      - Optionally add creator as participant.
  - `GET /api/chat/rooms`:
    - Response: list of `ChatRoomResponse`s.
    - Optionally filter rooms by current user participation.
  - `GET /api/chat/rooms/{roomId}/messages`:
    - Query parameters: `page`, `size` or `before` timestamp for pagination.
    - Response: `MessagePageResponse`.
    - Behavior: fetch historically persisted messages, sorted by `sentAt`.
  - `POST /api/chat/rooms/{roomId}/messages`:
    - Request: `SendMessageRequest` (or reuse path `roomId`).
    - Response: `ChatMessageResponse`.
    - Behavior:
      - Persist message.
      - Optionally also forward message to WebSocket for realtime subscribers.

**4.3. Error Handling**

- Implement a global `@ControllerAdvice` for API error responses:
  - Convert validation errors to well-defined JSON structure.
  - Map domain-specific exceptions (e.g., `UsernameAlreadyExistsException`, `RoomNotFoundException`, `InvalidTokenException`) to appropriate HTTP status codes.

---

### 5. WebSocket Configuration and Messaging Flow

**5.1. WebSocket/Stomp Endpoint Setup**

- Create a `WebSocketConfig` class:
  - Annotate with `@EnableWebSocketMessageBroker`.
  - Implement `WebSocketMessageBrokerConfigurer`.
- Configure:
  - STOMP endpoint:
    - `registerStompEndpoints`: register `/ws` or `/ws/chat`.
    - Enable SockJS fallback for browser support if needed: `.withSockJS()`.
    - Configure allowed origins (`setAllowedOrigins` or `setAllowedOriginPatterns`).
  - Message broker:
    - `enableSimpleBroker("/topic", "/queue")`:
      - `/topic/chat-room.{roomId}` for broadcasting messages to chat rooms.
    - `setApplicationDestinationPrefixes("/app")` for incoming messages from clients to application handlers.

**5.2. WebSocket Security and JWT Integration**

- Use JWT for authenticating WebSocket handshake and STOMP frames:
  - Decide between:
    - (A) Pass JWT as `Authorization` header during WebSocket handshake.
    - (B) Pass JWT as a query parameter or STOMP `CONNECT` header.
  - Prefer (A) for alignment with HTTP semantics; fall back to STOMP headers if necessary.
- Implement WebSocket authentication:
  - Implement `ChannelInterceptor` that:
    - Intercepts `CONNECT` frames.
    - Extracts JWT from headers.
    - Validates token via `JwtService`.
    - If valid, build a `Principal` and set the user in the `StompHeaderAccessor`/`MessageHeaders`.
    - Rejects connection if invalid.
- Optionally, integrate with Spring Security messaging configuration:
  - Use `@EnableWebSocketSecurity` or messaging security configuration if available in Spring Security for Boot 4.
  - Configure message-level access rules:
    - `/app/**` require authenticated user.
    - `/topic/**` subscription allowed only for authenticated users.

**5.3. Message Handling Controller (STOMP)**

- Implement `ChatWebSocketController` using `@Controller`:
  - Map incoming STOMP messages:
    - `@MessageMapping("/chat.send")`:
      - Payload type: `InboundChatMessage`.
      - Behavior:
        - Extract authenticated user from `Principal`.
        - Validate `roomId` and membership.
        - Persist message via `ChatMessageService`.
        - Convert persisted message to `OutboundChatMessage`.
        - Use `SimpMessagingTemplate` to `convertAndSend` to `/topic/chat-room.{roomId}`.
    - Optional: `@MessageMapping("/chat.typing")` for typing indicators (can be extended later).
- Outbound destination naming:
  - Standardize on `/topic/chat-room.{roomId}` or `/topic/rooms/{roomId}`.

---

### 6. Persistence Integration with WebSockets and Delivery Semantics

**6.1. Atomic Persistence and Broadcast**

- Ensure chat messages are persisted before broadcasting:
  - Inside `ChatMessageService`:
    - Create message entity from request and authenticated user.
    - Save entity via repository.
    - Return DTO for broadcasting.
- Avoid broadcasting unpersisted messages to keep history in sync with realtime events.

**6.2. Room Membership Enforcement**

- Before allowing send or subscribe:
  - Check if authenticated user is a participant in the room:
    - At REST level for `/api/chat/rooms/{roomId}/messages`.
    - At WebSocket level before processing `/app/chat.send` messages or allowing subscriptions to `/topic/chat-room.{roomId}`.
- Implement a `ChatRoomService` with methods:
  - `boolean isUserInRoom(Long roomId, Long userId)`
  - `ChatRoom requireUserInRoom(Long roomId, Long userId)` that throws exception if not.

**6.3. Pagination and Ordering**

- For history retrieval:
  - Use `Pageable` or `Slice` with `sentAt` ordering.
  - Return metadata (`hasNext`, `page`, `size`) to clients for pagination UI.

---

### 7. Application Configuration and Profiles

**7.1. application.properties**

- Configure PostgreSQL datasource:
  - `spring.datasource.url=jdbc:postgresql://...`
  - `spring.datasource.username=...`
  - `spring.datasource.password=...`
- Configure JPA/Hibernate:
  - `spring.jpa.hibernate.ddl-auto=validate` (or `update` during development).
  - `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect` aligned with Boot 4 defaults where needed.
- Configure JWT properties:
  - `app.security.jwt.secret=...` (or keystore configuration for asymmetric keys).
  - `app.security.jwt.access-token-expiration=...` (e.g., seconds or ISO duration).
  - `app.security.jwt.refresh-token-expiration=...`.
- Configure WebSocket/CORS:
  - `app.cors.allowed-origins=http://localhost:3000` (for frontend).
- Optionally configure logging and test DB overrides.

**7.2. Profiles**

- At minimum, define:
  - `application-dev.properties` for local development using local PostgreSQL.
  - `application-test.properties` for tests, providing an in-memory DB or test containers configuration.
- Ensure `BackendServerApplicationTests` context loads:
  - Provide a reachable DB configuration (or in-memory) for tests.
  - If using Testcontainers, configure as needed.

---

### 8. Testing Strategy

**8.1. Unit Tests**

- For services:
  - `AuthServiceTests`:
    - Registration flow: unique checks, password encoding, user persistence.
    - Login: valid credentials vs. invalid.
    - Refresh token: valid/invalid/expired/revoked scenarios.
  - `JwtServiceTests`:
    - Access/refresh generation and parsing.
    - Expiration handling.
- For mappers:
  - Validate conversion between entities and DTOs.

**8.2. Spring Test Slices**

- Web layer tests:
  - Use `@WebMvcTest`-style slice adapted for Spring Boot 4 webmvc test starter.
  - Test `AuthController` and `ChatController` endpoints with mock services.
- JPA tests:
  - Use `@DataJpaTest` slice for repositories:
    - Check queries (e.g., `findByUsernameIgnoreCase`, `findByRoomIdOrderBySentAtAsc`).

**8.3. Security Tests**

- Basic security configuration tests:
  - Ensure `/api/auth/**` endpoints are accessible without authentication.
  - Ensure `/api/chat/**` endpoints are protected and require JWT.
- If possible, use mocked JWT or a test configuration of `JwtService` for integration tests.

**8.4. WebSocket Tests (Optional Initial Phase)**

- Integration tests for WebSocket endpoints:
  - Connect with valid JWT.
  - Subscribe to `/topic/chat-room.{roomId}`.
  - Send `/app/chat.send` message and assert broadcast reception.
- These can be added in a later phase if initial scope focuses on HTTP and domain correctness.

---

### Further Considerations

1. Decide whether to issue tokens on registration or require explicit login after registration.  
2. Decide on refresh token rotation vs. fixed refresh; rotation is more secure but more complex.  
3. Decide if direct messages vs. group chats are supported initially; design `ChatRoom` flexible enough for both.
