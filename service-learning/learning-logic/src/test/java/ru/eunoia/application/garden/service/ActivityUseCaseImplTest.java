package ru.eunoia.application.garden.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.garden.domain.model.ActivityStats;
import ru.eunoia.application.garden.port.out.ActivityRepositoryPort;

/** Журнал занятий: пишем сегодняшний день и считаем сводку из окна года. */
@ExtendWith(MockitoExtension.class)
class ActivityUseCaseImplTest {

    private static final UUID USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 25);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-25T09:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ActivityRepositoryPort repository;

    @Test
    void record_touchesTodaysDay() {
        new ActivityUseCaseImpl(repository, clock).record(USER);
        verify(repository).touch(USER, TODAY);
    }

    @Test
    void summary_readsYearWindowAndComputesStats() {
        when(repository.activeDaysSince(USER, TODAY.minusDays(365)))
                .thenReturn(Set.of(TODAY, TODAY.minusDays(1)));

        ActivityStats s = new ActivityUseCaseImpl(repository, clock).summary(USER);

        assertThat(s.streak()).isEqualTo(2);
        assertThat(s.lastActiveDate()).isEqualTo(TODAY);
        assertThat(s.daysActive30()).isEqualTo(2);
    }
}
