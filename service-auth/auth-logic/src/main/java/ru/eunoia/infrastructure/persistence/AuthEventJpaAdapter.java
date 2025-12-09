package ru.eunoia.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.eunoia.application.out.AuthEventRepositoryPort;
import ru.eunoia.domain.model.AuthEvent;
import ru.eunoia.domain.model.entity.AuthEventEntity;
import ru.eunoia.infrastructure.persistence.mapper.AuthEventMapper;
import ru.eunoia.infrastructure.persistence.repository.AuthEventJpaRepository;

@Log4j2
@Component
@RequiredArgsConstructor
public class AuthEventJpaAdapter implements AuthEventRepositoryPort {

    private final AuthEventJpaRepository jpaRepository;
    private final AuthEventMapper mapper;

    @Override
    @Transactional
    public AuthEvent save(AuthEvent event) {
        try {
            AuthEventEntity entity = mapper.toEntity(event);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }

            AuthEventEntity savedEntity = jpaRepository.save(entity);
            log.debug("Saved auth event: {} for user {}", event.getEventType(), event.getUserId());

            return mapper.toDomain(savedEntity);

        } catch (Exception e) {
            log.error("Failed to save auth event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save auth event", e);
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
        log.debug("Deleted auth event with id: {}", id);
    }

    @Override
    public List<AuthEvent> findByUserId(UUID userId) {
        List<AuthEventEntity> entities = jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findByUserId(UUID userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findByUserId(userId, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findByUserIdAndEventType(UUID userId, AuthEvent.EventType eventType) {
        AuthEventEntity.EventType entityEventType = mapper.mapEventType(eventType);
        List<AuthEventEntity> entities = jpaRepository.findByUserIdAndEventType(userId, entityEventType);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findByUserIdAndSuccess(UUID userId, boolean success) {
        List<AuthEventEntity> entities = jpaRepository.findByUserIdAndSuccess(userId, success);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findByEventType(AuthEvent.EventType eventType) {
        AuthEventEntity.EventType entityEventType = mapper.mapEventType(eventType);
        List<AuthEventEntity> entities = jpaRepository.findByEventTypeOrderByCreatedAtDesc(entityEventType);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findByEventType(AuthEvent.EventType eventType, int limit) {
        AuthEventEntity.EventType entityEventType = mapper.mapEventType(eventType);
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findByEventType(entityEventType, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findByEventTypeAndSuccess(AuthEvent.EventType eventType, boolean success) {
        AuthEventEntity.EventType entityEventType = mapper.mapEventType(eventType);
        List<AuthEventEntity> entities = jpaRepository.findByEventTypeAndSuccess(entityEventType, success);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findRecentEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findAll(pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findEventsBetween(LocalDateTime start, LocalDateTime end) {
        List<AuthEventEntity> entities = jpaRepository.findByCreatedAtBetween(start, end);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findEventsBefore(LocalDateTime dateTime) {
        List<AuthEventEntity> entities = jpaRepository.findByCreatedAtBefore(dateTime);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthEvent> findEventsAfter(LocalDateTime dateTime) {
        List<AuthEventEntity> entities = jpaRepository.findByCreatedAtAfter(dateTime);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteOldEvents(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        int deletedCount = jpaRepository.deleteOldEvents(cutoffDate);
        log.info("Deleted {} old auth events older than {} days", deletedCount, days);
    }

    @Override
    @Transactional
    public void deleteEventsByUserId(UUID userId) {
        List<AuthEventEntity> events = jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
        jpaRepository.deleteAll(events);
        log.info("Deleted all auth events for user {}", userId);
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public long countByEventType(AuthEvent.EventType eventType) {
        AuthEventEntity.EventType entityEventType = mapper.mapEventType(eventType);
        return jpaRepository.countByEventType(entityEventType);
    }

    @Override
    public long countByUserIdAndEventType(UUID userId, AuthEvent.EventType eventType) {
        AuthEventEntity.EventType entityEventType = mapper.mapEventType(eventType);
        return jpaRepository.countByUserIdAndEventType(userId, entityEventType);
    }

    @Override
    public long countByUserIdAndSuccess(UUID userId, boolean success) {
        return jpaRepository.countByUserIdAndSuccess(userId, success);
    }

    // Дополнительные методы для бизнес-логики

    public List<AuthEvent> findRecentFailedLogins(UUID userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<AuthEventEntity> entities = jpaRepository.findFailedLoginAttemptsSince(userId, since);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    public boolean hasSuspiciousActivity(UUID userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<AuthEventEntity> failedLogins = jpaRepository.findFailedLoginAttemptsSince(userId, since);
        return failedLogins.size() >= 5; // 5 неудачных попыток за hours часов
    }

}
