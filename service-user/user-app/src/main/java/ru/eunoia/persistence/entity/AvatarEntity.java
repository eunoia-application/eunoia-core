package ru.eunoia.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Строка avatars: байты аватара по userId. Отдельная таблица от profiles, чтобы блоб
 * не тянулся в каждый запрос профиля. byte[] маппится в bytea (без @Lob — иначе PG large-object).
 */
@Entity
@Table(name = "avatars")
@Getter
@Setter
public class AvatarEntity {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
