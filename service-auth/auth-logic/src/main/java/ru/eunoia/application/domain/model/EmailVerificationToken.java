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
public class EmailVerificationToken {
    private UUID id;
    private UUID userId;
    private String email;
    private String tokenHash;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private boolean used;

    /**
     * Пометить токен как использованный с указанием времени
     */
    public void markAsUsed() {
        this.used = true;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * Пометить токен как верифицированный
     */
    public void markAsVerified() {
        this.used = true;
        this.verifiedAt = LocalDateTime.now();
    }
}
