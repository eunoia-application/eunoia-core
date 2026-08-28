package ru.eunoia.application.garden.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Персональный факт владения грамматическим правилом: пользователь ↔ правило ↔ статус.
 * {@code grammarId} — id узла Grammar из канона (напр. {@code past-simple}), ссылка только по id
 * (принцип двух графов). Параллель к {@link Mastery} по словам; при M5-Ф2 оба сольются в review_state.
 */
public record GrammarMastery(UUID userId, String grammarId, MasteryStatus status, LocalDateTime updatedAt) {
}
