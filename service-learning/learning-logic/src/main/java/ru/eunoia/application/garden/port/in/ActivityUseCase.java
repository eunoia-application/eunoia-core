package ru.eunoia.application.garden.port.in;

import java.util.UUID;
import ru.eunoia.application.garden.domain.model.ActivityStats;

/**
 * Журнал занятий: фиксируем, что пользователь сегодня что-то делал, и отдаём сводку для дерева
 * (стрик/погода/сезоны). История нужна дереву в M5 — поэтому пишем её уже сейчас.
 */
public interface ActivityUseCase {

    /** Отметить активность пользователя сегодня (в пределах дня идемпотентно, копит счётчик). */
    void record(UUID userId);

    /** Сводка занятий: стрик, последний активный день, активных дней за 30. */
    ActivityStats summary(UUID userId);
}
