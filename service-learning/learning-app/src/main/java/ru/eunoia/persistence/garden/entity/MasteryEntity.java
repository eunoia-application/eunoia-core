package ru.eunoia.persistence.garden.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Строка mastery: (userId, lexemeId) → статус. lexemeId — id узла из Neo4j-графа (напр. en:go:VERB),
 * хранится строкой без FK (принцип двух графов: garden ссылается по id, не встраивая канон).
 */
@Entity
@Table(name = "mastery")
@IdClass(MasteryId.class)
@Getter
@Setter
public class MasteryEntity {

    @Id
    private UUID userId;
    @Id
    private String lexemeId;

    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
