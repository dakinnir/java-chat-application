# AGENTS.md — Backend Server

## Project Overview
Spring Boot **4.0.5** backend for a realtime chat application. Java 21, Gradle 9.4.1, PostgreSQL.
> Spring Boot 4 is a major version bump from 3.x — do not assume 3.x API compatibility.

## Key Tech Stack
| Layer | Library |
|---|---|
| Web | `spring-boot-starter-webmvc` (not `spring-boot-starter-web`) |
| Persistence | `spring-boot-starter-data-jpa` + PostgreSQL (`org.postgresql:postgresql`) |
| Auth | `spring-boot-starter-security` |
| Validation | `spring-boot-starter-validation` |
| Code gen | Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`, etc.) |

## Base Package
All application code lives under `com.dakinnir.backendserver` (`src/main/java/com/dakinnir/backendserver/`).

## Developer Commands
```bash
# Build (skip tests)
./gradlew build -x test

# Run locally (hot-reload via spring-boot-devtools is active)
./gradlew bootRun

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```
Use `./gradlew` (wrapper), not a system-installed Gradle.

## Configuration
`src/main/resources/application.properties` is the single config file. PostgreSQL datasource and Spring Security credentials **must be set** before the app starts — the context-loads test (`BackendServerApplicationTests`) will fail without a reachable database unless mocked.

## Spring Boot 4 Naming Conventions (differs from 3.x)
- Web starter: `spring-boot-starter-webmvc`
- Test slices follow the same pattern: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`

## Java 21 Usage
Prefer Java 21 idioms: records for DTOs/responses, sealed interfaces for domain variants, pattern matching, and `SequencedCollection` APIs where applicable.

## Security
`spring-boot-starter-security` is on the classpath — **all endpoints are locked by default**. Any new REST controller must either configure a `SecurityFilterChain` bean or annotate paths explicitly. Do not expose endpoints without deliberate security decisions.

## Key Files
- `build.gradle` — dependency declarations and toolchain (Java 21)
- `src/main/java/com/dakinnir/backendserver/BackendServerApplication.java` — app entry point (`@SpringBootApplication`)
- `src/main/resources/application.properties` — runtime configuration
- `src/test/java/com/dakinnir/backendserver/BackendServerApplicationTests.java` — smoke test (`contextLoads`)

