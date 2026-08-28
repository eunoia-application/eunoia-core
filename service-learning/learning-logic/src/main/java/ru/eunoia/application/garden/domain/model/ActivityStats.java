package ru.eunoia.application.garden.domain.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Сводка занятий пользователя для дерева: стрик (дней подряд), последний активный день и
 * сколько активных дней за последние 30. Из этого фронт лепит погоду/сезоны/цветение.
 *
 * <p>Считается из множества «активных дней» (в какие даты было хоть одно действие) — сама история
 * копится в garden (activity_log). Числа непрерывные, маппинг в визуал — на фронте.
 */
public record ActivityStats(int streak, LocalDate lastActiveDate, int daysActive30) {

    private static final int RECENT_WINDOW = 30;

    /** Пустая сводка (истории ещё нет). */
    public static ActivityStats empty() {
        return new ActivityStats(0, null, 0);
    }

    /**
     * Считает сводку по активным дням относительно {@code today}. Стрик — цепочка подряд идущих
     * дней, оканчивающаяся сегодня (или вчера, если сегодня ещё не занимался — чтобы серия не
     * обнулялась в течение дня). daysActive30 — активных дней за последние 30 (включая сегодня).
     */
    public static ActivityStats of(Collection<LocalDate> activeDays, LocalDate today) {
        if (activeDays.isEmpty()) {
            return empty();
        }
        Set<LocalDate> days = new HashSet<>(activeDays);

        // стартуем стрик с сегодня, иначе со вчера (льготный день), иначе серия разорвана
        LocalDate cursor = days.contains(today) ? today
                : days.contains(today.minusDays(1)) ? today.minusDays(1)
                : null;
        int streak = 0;
        while (cursor != null && days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        LocalDate last = null;
        int recent = 0;
        LocalDate recentFrom = today.minusDays(RECENT_WINDOW - 1L);
        for (LocalDate d : days) {
            if (last == null || d.isAfter(last)) {
                last = d;
            }
            if (!d.isBefore(recentFrom) && !d.isAfter(today)) {
                recent++;
            }
        }
        return new ActivityStats(streak, last, recent);
    }
}
