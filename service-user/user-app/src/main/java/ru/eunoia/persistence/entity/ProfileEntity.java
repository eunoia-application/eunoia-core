package ru.eunoia.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Строка profiles. userId — присвоенный id из auth (НЕ @GeneratedValue). Настройки разложены в колонки. */
@Entity
@Table(name = "profiles")
@Getter
@Setter
public class ProfileEntity {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String username;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    @Column(nullable = false)
    private boolean emailVerified;

    // settings (плоско в колонки)
    @Column(nullable = false)
    private String theme;
    private String interfaceLanguage;
    @Column(nullable = false)
    private String profileVisibility;
    @Column(nullable = false)
    private boolean emailNotifications;
    @Column(nullable = false)
    private boolean aiSuggestionsEnabled;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
