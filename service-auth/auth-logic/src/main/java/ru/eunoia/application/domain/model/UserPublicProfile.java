package ru.eunoia.application.domain.model;

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
public class UserPublicProfile {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    private UserPublicStats stats;
    private LocalDateTime createdAt;
}
