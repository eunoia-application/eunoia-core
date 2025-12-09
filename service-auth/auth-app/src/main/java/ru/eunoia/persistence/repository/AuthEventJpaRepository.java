package ru.eunoia.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.eunoia.application.domain.model.entity.AuthEventEntity;

@Repository
public interface AuthEventJpaRepository extends JpaRepository<AuthEventEntity, UUID> {

    // === Поиск по пользователю ===
    List<AuthEventEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Page<AuthEventEntity> findByUserId(UUID userId, Pageable pageable);
    List<AuthEventEntity> findByUserIdAndEventType(UUID userId, AuthEventEntity.EventType eventType);
    List<AuthEventEntity> findByUserIdAndSuccess(UUID userId, boolean success);

    // === Поиск по типу события ===
    List<AuthEventEntity> findByEventTypeOrderByCreatedAtDesc(AuthEventEntity.EventType eventType);
    Page<AuthEventEntity> findByEventType(AuthEventEntity.EventType eventType, Pageable pageable);
    List<AuthEventEntity> findByEventTypeAndSuccess(AuthEventEntity.EventType eventType, boolean success);

    // === Поиск по времени ===
    List<AuthEventEntity> findByCreatedAtAfter(LocalDateTime dateTime);
    List<AuthEventEntity> findByCreatedAtBefore(LocalDateTime dateTime);
    List<AuthEventEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // === Комбинированные запросы ===
    @Query("SELECT ae FROM AuthEventEntity ae WHERE ae.userId = :userId AND ae.createdAt >= :since")
    List<AuthEventEntity> findUserEventsSince(@Param("userId") UUID userId,
            @Param("since") LocalDateTime since);

    @Query("SELECT ae FROM AuthEventEntity ae WHERE ae.eventType IN :eventTypes ORDER BY ae.createdAt DESC")
    List<AuthEventEntity> findByEventTypes(@Param("eventTypes") List<AuthEventEntity.EventType> eventTypes);

    // === Статистика ===
    long countByUserId(UUID userId);
    long countByEventType(AuthEventEntity.EventType eventType);
    long countByUserIdAndEventType(UUID userId, AuthEventEntity.EventType eventType);
    long countByUserIdAndSuccess(UUID userId, boolean success);

    // === Очистка старых записей ===
    @Modifying
    @Query("DELETE FROM AuthEventEntity ae WHERE ae.createdAt < :dateTime")
    int deleteOldEvents(@Param("dateTime") LocalDateTime dateTime);

    // === Поиск подозрительной активности ===
    @Query("""
        SELECT ae FROM AuthEventEntity ae 
        WHERE ae.userId = :userId 
        AND ae.eventType = 'LOGIN_FAILED' 
        AND ae.createdAt >= :since
        ORDER BY ae.createdAt DESC
    """)
    List<AuthEventEntity> findFailedLoginAttemptsSince(
            @Param("userId") UUID userId,
            @Param("since") LocalDateTime since
    );

}
