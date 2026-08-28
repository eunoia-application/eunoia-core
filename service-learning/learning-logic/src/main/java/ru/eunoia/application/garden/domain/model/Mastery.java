package ru.eunoia.application.garden.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Персональный факт владения: пользователь ↔ слово ↔ статус. {@code lexemeId} — это id узла
 * канонического графа (напр. {@code en:go:VERB}), но ссылка только по id: garden не лезет в Neo4j
 * (принцип двух графов). Ключ — пара (userId, lexemeId).
 */
public record Mastery(UUID userId, String lexemeId, MasteryStatus status, LocalDateTime updatedAt) {
}
