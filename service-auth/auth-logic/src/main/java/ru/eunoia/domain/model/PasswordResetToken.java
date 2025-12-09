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
public class PasswordResetToken {

    private UUID id;
    private UUID userId;
    private String email;
    private String tokenHash;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private boolean used;
    private String newPasswordHash;

    /**
     * Пометить токен как использованный
     */
    public void markAsUsed(String newPasswordHash) {
        this.used = true;
        this.usedAt = LocalDateTime.now();
        this.newPasswordHash = newPasswordHash;
    }
}
