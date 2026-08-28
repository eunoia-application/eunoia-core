package ru.eunoia.persistence.garden;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.eunoia.application.garden.port.out.ActivityRepositoryPort;
import ru.eunoia.persistence.garden.repository.ActivityJpaRepository;

/** JPA-адаптер журнала занятий. touch — атомарный upsert (нативный ON CONFLICT). */
@Component
@RequiredArgsConstructor
public class ActivityRepositoryAdapter implements ActivityRepositoryPort {

    private final ActivityJpaRepository jpaRepository;

    @Override
    @Transactional
    public void touch(UUID userId, LocalDate day) {
        jpaRepository.touch(userId, day);
    }

    @Override
    public Set<LocalDate> activeDaysSince(UUID userId, LocalDate since) {
        return new HashSet<>(jpaRepository.activeDaysSince(userId, since));
    }
}
