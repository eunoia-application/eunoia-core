package ru.eunoia.application.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;
    private String email;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    private boolean emailVerified;
    private boolean active;
    private boolean locked;
    private UserStats stats;
    private UserSettings settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;

}
