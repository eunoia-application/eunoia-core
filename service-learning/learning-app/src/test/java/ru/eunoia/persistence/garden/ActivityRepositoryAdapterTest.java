package ru.eunoia.persistence.garden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.eunoia.persistence.garden.entity.ActivityId;
import ru.eunoia.persistence.garden.repository.ActivityJpaRepository;

/**
 * Смоук-тест журнала занятий на реальном Postgres (Testcontainers). Проверяем, что {@code touch}
 * действительно апсертит день (первое действие — вставка со счётчиком 1, повторное за тот же день —
 * +1, а не новая строка), а {@code activeDaysSince} отдаёт мои дни от даты и не подмешивает чужие.
 * Срез контекста собираем вручную (как в MasteryRepositoryAdapterTest). Требует Docker.
 */
@SpringBootTest(classes = ActivityRepositoryAdapterTest.ActivitySliceConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ActivityRepositoryAdapterTest {

    private static final LocalDate DAY = LocalDate.of(2026, 7, 25);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    ActivityRepositoryAdapter adapter;

    @Autowired
    ActivityJpaRepository jpaRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void clean() {
        jpaRepository.deleteAll();
    }

    @Test
    void touch_firstTime_insertsDayWithOneAction() {
        adapter.touch(userId, DAY);

        assertThat(jpaRepository.findById(new ActivityId(userId, DAY)))
                .get().extracting("actions").isEqualTo(1);
    }

    /** Апсерт: повторный touch того же дня инкрементит счётчик, а не плодит строки. */
    @Test
    void touch_sameDayAgain_incrementsActions() {
        adapter.touch(userId, DAY);
        adapter.touch(userId, DAY);
        adapter.touch(userId, DAY);

        assertThat(jpaRepository.findById(new ActivityId(userId, DAY)))
                .get().extracting("actions").isEqualTo(3);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void activeDaysSince_returnsMyDaysFromDate_onlyMine() {
        adapter.touch(userId, DAY);                 // в окне
        adapter.touch(userId, DAY.minusDays(3));    // в окне
        adapter.touch(userId, DAY.minusDays(10));   // вне окна (раньше since)
        adapter.touch(otherUserId, DAY);            // чужой

        assertThat(adapter.activeDaysSince(userId, DAY.minusDays(5)))
                .containsExactlyInAnyOrder(DAY, DAY.minusDays(3));
    }

    @Test
    void activeDaysSince_none_returnsEmpty() {
        assertThat(adapter.activeDaysSince(userId, DAY.minusDays(30))).isEmpty();
    }

    @AutoConfigurationPackage
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class})
    @EnableJpaRepositories(basePackageClasses = ActivityJpaRepository.class)
    @Import(ActivityRepositoryAdapter.class)
    static class ActivitySliceConfig {
    }
}
