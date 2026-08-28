package ru.eunoia.application.garden.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.ActivityStats;
import ru.eunoia.application.garden.port.in.ActivityUseCase;
import ru.eunoia.application.garden.port.out.ActivityRepositoryPort;

/**
 * Журнал занятий. record() пишет сегодняшний день, summary() считает стрик/статистику из окна
 * последнего года (стрик длиннее года всё равно визуально «максимальный»). Clock инъектим —
 * так «сегодня» одно на весь use case и тестируемо.
 */
public class ActivityUseCaseImpl implements ActivityUseCase {

    /** Окно выборки для стрика/статистики: год назад — с запасом под длинные серии. */
    private static final int WINDOW_DAYS = 365;

    private final ActivityRepositoryPort activity;
    private final Clock clock;

    public ActivityUseCaseImpl(ActivityRepositoryPort activity, Clock clock) {
        this.activity = activity;
        this.clock = clock;
    }

    @Override
    public void record(UUID userId) {
        activity.touch(userId, LocalDate.now(clock));
    }

    @Override
    public ActivityStats summary(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        return ActivityStats.of(activity.activeDaysSince(userId, today.minusDays(WINDOW_DAYS)), today);
    }
}
