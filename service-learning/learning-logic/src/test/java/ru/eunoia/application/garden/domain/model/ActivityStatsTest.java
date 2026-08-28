package ru.eunoia.application.garden.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Стрик и статистика активности из множества активных дней. */
class ActivityStatsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 25);

    @Test
    void empty_isZeroes() {
        ActivityStats s = ActivityStats.empty();
        assertThat(s.streak()).isZero();
        assertThat(s.lastActiveDate()).isNull();
        assertThat(s.daysActive30()).isZero();
    }

    @Test
    void of_noDays_isEmpty() {
        assertThat(ActivityStats.of(List.of(), TODAY)).isEqualTo(ActivityStats.empty());
    }

    @Test
    void streak_countsConsecutiveRunEndingToday() {
        ActivityStats s = ActivityStats.of(
                List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY);
        assertThat(s.streak()).isEqualTo(3);
        assertThat(s.lastActiveDate()).isEqualTo(TODAY);
        assertThat(s.daysActive30()).isEqualTo(3);
    }

    @Test
    void streak_graceDay_countsRunEndingYesterday_whenTodayIdle() {
        ActivityStats s = ActivityStats.of(List.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY);
        assertThat(s.streak()).isEqualTo(2);
        assertThat(s.lastActiveDate()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void streak_gapWithinRun_stopsAtGap() {
        // сегодня активен, но вчера пропущено → серия = только сегодня
        ActivityStats s = ActivityStats.of(List.of(TODAY, TODAY.minusDays(2)), TODAY);
        assertThat(s.streak()).isEqualTo(1);
        assertThat(s.daysActive30()).isEqualTo(2);
    }

    @Test
    void streak_broken_whenLastActiveOlderThanYesterday() {
        ActivityStats s = ActivityStats.of(Set.of(TODAY.minusDays(5)), TODAY);
        assertThat(s.streak()).isZero();
        assertThat(s.lastActiveDate()).isEqualTo(TODAY.minusDays(5));
        assertThat(s.daysActive30()).isEqualTo(1);
    }

    @Test
    void daysActive30_countsOnlyLast30Days() {
        // граница окна: today-29 внутри, today-30 снаружи
        ActivityStats s = ActivityStats.of(
                List.of(TODAY.minusDays(29), TODAY.minusDays(30)), TODAY);
        assertThat(s.daysActive30()).isEqualTo(1);
        assertThat(s.streak()).isZero();
        assertThat(s.lastActiveDate()).isEqualTo(TODAY.minusDays(29));
    }
}
