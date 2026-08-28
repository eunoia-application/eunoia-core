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
 * Строка grammar_mastery: (userId, grammarId) → статус. grammarId — id узла Grammar из Neo4j
 * (напр. past-simple), хранится строкой без FK (принцип двух графов). Параллель к MasteryEntity.
 */
@Entity
@Table(name = "grammar_mastery")
@IdClass(GrammarMasteryId.class)
@Getter
@Setter
public class GrammarMasteryEntity {

    @Id
    private UUID userId;
    @Id
    private String grammarId;

    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
