package ru.eunoia.application.garden.port.out;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Хранилище журнала занятий (Postgres). Реализуется JPA-адаптером в learning-app. */
public interface ActivityRepositoryPort {

    /** Отметить день активности пользователя (upsert: +1 к счётчику действий за этот день). */
    void touch(UUID userId, LocalDate day);

    /** Дни, в которые пользователь был активен, начиная с {@code since} (включительно). */
    Set<LocalDate> activeDaysSince(UUID userId, LocalDate since);
}
