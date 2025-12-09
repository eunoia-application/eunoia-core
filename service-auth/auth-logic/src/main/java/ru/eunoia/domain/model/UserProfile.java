package ru.eunoia.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    private boolean emailVerified;
    private UserStats stats;
    private UserSettings settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
