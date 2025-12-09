package ru.eunoia.infrastructure.external.userclient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.eunoia.application.port.out.UserServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.eunoia.application.domain.exception.ServiceUnavailableException;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.infrastructure.external.userclient.dto.CreateUserRequestDto;
import ru.eunoia.infrastructure.external.userclient.dto.UpdateLastLoginRequestDto;
import ru.eunoia.infrastructure.external.userclient.dto.UpdatePasswordRequestDto;
import ru.eunoia.infrastructure.external.userclient.dto.UpdateUserRequestDto;
import ru.eunoia.infrastructure.external.userclient.dto.UserResponseDto;
import ru.eunoia.infrastructure.external.userclient.dto.ValidatePasswordRequestDto;
import ru.eunoia.infrastructure.external.userclient.mapper.UserResponseMapper;

@Log4j2
@Component
@RequiredArgsConstructor
public class UserServiceEurekaAdapter implements UserServicePort {

    @Qualifier("userServiceRestClient")
    private final RestClient restClient;
    private final UserResponseMapper userResponseMapper;

    @Override
    @CircuitBreaker(name = "userService", fallbackMethod = "findUserByIdFallback")
    @Retry(name = "userService", fallbackMethod = "findUserByIdFallback")
    @TimeLimiter(name = "userService", fallbackMethod = "findUserByIdFallback")
    public CompletableFuture<Optional<User>> findUserById(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = restClient.get()
                        .uri("/api/users/{userId}", userId.toString())
                        .retrieve()
                        .body(UserResponseDto.class);

                return Optional.ofNullable(userResponseMapper.toDomain(response));
            } catch (Exception e) {
                log.warn("Failed to find user by id {}: {}", userId, e.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<User>> findUserByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = restClient.get()
                        .uri("/api/users/by-email?email={email}", email)
                        .retrieve()
                        .body(UserResponseDto.class);

                return Optional.ofNullable(userResponseMapper.toDomain(response));
            } catch (Exception e) {
                log.warn("Failed to find user by email {}: {}", email, e.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<User>> findUserByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = restClient.get()
                        .uri("/api/users/by-username?username={username}", username)
                        .retrieve()
                        .body(UserResponseDto.class);

                return Optional.ofNullable(userResponseMapper.toDomain(response));
            } catch (Exception e) {
                log.warn("Failed to find user by username {}: {}", username, e.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> userExistsByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                restClient.get()
                        .uri("/api/users/exists-by-email?email={email}", email)
                        .retrieve()
                        .toBodilessEntity();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> userExistsByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                restClient.get()
                        .uri("/api/users/exists-by-username?username={username}", username)
                        .retrieve()
                        .toBodilessEntity();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<User> createUser(User user) {
        return CompletableFuture.supplyAsync(() -> {
            CreateUserRequestDto requestDto = CreateUserRequestDto.builder()
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .passwordHash(user.getPasswordHash())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .build();

            var response = restClient.post()
                    .uri("/api/users")
                    .body(requestDto)
                    .retrieve()
                    .body(UserResponseDto.class);

            return userResponseMapper.toDomain(response);
        });
    }

    @Override
    public CompletableFuture<User> updateUser(User user) {
        return CompletableFuture.supplyAsync(() -> {
            UpdateUserRequestDto requestDto = UpdateUserRequestDto.builder()
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatarUrl(user.getAvatarUrl())
                    .bio(user.getBio())
                    .emailVerified(user.isEmailVerified())
                    .active(user.isActive())
                    .locked(user.isLocked())
                    .lastLoginAt(user.getLastLoginAt())
                    .failedLoginAttempts(user.getFailedLoginAttempts())
                    .lockedUntil(user.getLockedUntil())
                    .build();

            var response = restClient.put()
                    .uri("/api/users/{userId}", user.getId().toString())
                    .body(requestDto)
                    .retrieve()
                    .body(UserResponseDto.class);

            return userResponseMapper.toDomain(response);
        });
    }

    @Override
    public CompletableFuture<Boolean> validatePassword(UUID userId, String rawPassword) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ValidatePasswordRequestDto request = ValidatePasswordRequestDto.builder()
                        .userId(userId)
                        .password(rawPassword)
                        .build();

                return restClient.post()
                        .uri("/api/users/validate-password")
                        .body(request)
                        .retrieve()
                        .body(Boolean.class);
            } catch (Exception e) {
                log.error("Failed to validate password for user {}: {}", userId, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Void> updatePassword(UUID userId, String newPasswordHash) {
        return CompletableFuture.runAsync(() -> {
            UpdatePasswordRequestDto request = UpdatePasswordRequestDto.builder()
                    .userId(userId)
                    .newPasswordHash(newPasswordHash)
                    .updatedAt(LocalDateTime.now())
                    .build();

            restClient.put()
                    .uri("/api/users/{userId}/password", userId.toString())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        });
    }

    @Override
    public CompletableFuture<Void> lockUser(UUID userId) {
        return CompletableFuture.runAsync(() -> restClient.post()
                .uri("/api/users/{userId}/lock", userId.toString())
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public CompletableFuture<Void> unlockUser(UUID userId) {
        return CompletableFuture.runAsync(() -> restClient.post()
                .uri("/api/users/{userId}/unlock", userId.toString())
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public CompletableFuture<Void> activateUser(UUID userId) {
        return CompletableFuture.runAsync(() -> restClient.post()
                .uri("/api/users/{userId}/activate", userId.toString())
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public CompletableFuture<Void> deactivateUser(UUID userId) {
        return CompletableFuture.runAsync(() -> restClient.post()
                .uri("/api/users/{userId}/deactivate", userId.toString())
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public CompletableFuture<Void> updateLastLogin(UUID userId) {
        return CompletableFuture.runAsync(() -> {
            UpdateLastLoginRequestDto request = UpdateLastLoginRequestDto.builder()
                    .userId(userId)
                    .lastLoginAt(LocalDateTime.now())
                    .build();

            restClient.put()
                    .uri("/api/users/{userId}/last-login", userId.toString())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        });
    }

    @Override
    public CompletableFuture<Void> incrementFailedLoginAttempts(UUID userId) {
        return CompletableFuture.runAsync(() -> restClient.post()
                .uri("/api/users/{userId}/increment-failed-attempts", userId.toString())
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public CompletableFuture<Void> resetFailedLoginAttempts(UUID userId) {
        return CompletableFuture.runAsync(() -> restClient.post()
                .uri("/api/users/{userId}/reset-failed-attempts", userId.toString())
                .retrieve()
                .toBodilessEntity());
    }

    // === Fallback методы ===

    private CompletableFuture<Optional<User>> findUserByIdFallback(UUID userId, Throwable t) {
        log.warn("Fallback for findUserById {}: {}", userId, t.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }

    private CompletableFuture<Optional<User>> findUserByEmailFallback(String email, Throwable t) {
        log.warn("Fallback for findUserByEmail {}: {}", email, t.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }

    private CompletableFuture<User> createUserFallback(User user, Throwable t) {
        log.error("Fallback for createUser: {}", t.getMessage());
        throw new ServiceUnavailableException("User service is unavailable");
    }
}
