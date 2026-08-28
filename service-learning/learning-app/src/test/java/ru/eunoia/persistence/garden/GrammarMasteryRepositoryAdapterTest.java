package ru.eunoia.persistence.garden;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import ru.eunoia.application.garden.domain.model.GrammarMastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.persistence.garden.repository.GrammarMasteryJpaRepository;

/**
 * Смоук-тест адаптера прогресса по грамматике на реальном Postgres (Testcontainers): отметка
 * правила сохраняется/читается, повторная тем же ключом (userId+grammarId) перезаписывает статус,
 * чужие отметки в мои выборки не попадают, statusesFor отдаёт карту только по отмеченным. Требует Docker.
 */
@SpringBootTest(classes = GrammarMasteryRepositoryAdapterTest.GrammarSliceConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class GrammarMasteryRepositoryAdapterTest {

    private static final String PAST = "past-simple";
    private static final String PRESENT = "present-simple";
    private static final LocalDateTime MARKED_AT = LocalDateTime.of(2026, 7, 25, 12, 0);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    GrammarMasteryRepositoryAdapter adapter;

    @Autowired
    GrammarMasteryJpaRepository jpaRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void clean() {
        jpaRepository.deleteAll();
    }

    @Test
    void save_thenFind_returnsStoredMark() {
        GrammarMastery mark = new GrammarMastery(userId, PAST, MasteryStatus.LEARNING, MARKED_AT);

        assertThat(adapter.save(mark)).isEqualTo(mark);
        assertThat(adapter.find(userId, PAST)).contains(mark);
    }

    @Test
    void save_sameKeyAgain_upsertsStatus() {
        adapter.save(new GrammarMastery(userId, PAST, MasteryStatus.LEARNING, MARKED_AT));
        GrammarMastery updated = new GrammarMastery(userId, PAST, MasteryStatus.KNOWN, MARKED_AT.plusDays(1));
        adapter.save(updated);

        assertThat(adapter.find(userId, PAST)).contains(updated);
        assertThat(adapter.findByUser(userId)).hasSize(1);
    }

    @Test
    void find_missing_returnsEmpty() {
        assertThat(adapter.find(userId, PAST)).isEmpty();
    }

    @Test
    void findByUser_returnsAllMyMarks_andOnlyMine() {
        adapter.save(new GrammarMastery(userId, PAST, MasteryStatus.KNOWN, MARKED_AT));
        adapter.save(new GrammarMastery(userId, PRESENT, MasteryStatus.LEARNING, MARKED_AT));
        adapter.save(new GrammarMastery(otherUserId, PAST, MasteryStatus.KNOWN, MARKED_AT));

        assertThat(adapter.findByUser(userId))
                .extracting(GrammarMastery::grammarId, GrammarMastery::status)
                .containsExactlyInAnyOrder(
                        tuple(PAST, MasteryStatus.KNOWN),
                        tuple(PRESENT, MasteryStatus.LEARNING));
    }

    @Test
    void statusesFor_mapsMarkedIds_only() {
        adapter.save(new GrammarMastery(userId, PAST, MasteryStatus.KNOWN, MARKED_AT));
        adapter.save(new GrammarMastery(userId, PRESENT, MasteryStatus.LEARNING, MARKED_AT));

        Map<String, MasteryStatus> statuses = adapter.statusesFor(userId, List.of(PAST, PRESENT, "nope"));

        assertThat(statuses).containsExactlyInAnyOrderEntriesOf(
                Map.of(PAST, MasteryStatus.KNOWN, PRESENT, MasteryStatus.LEARNING));
    }

    @Test
    void statusesFor_emptyIds_returnsEmptyMap() {
        adapter.save(new GrammarMastery(userId, PAST, MasteryStatus.KNOWN, MARKED_AT));

        assertThat(adapter.statusesFor(userId, List.of())).isEmpty();
    }

    @AutoConfigurationPackage
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class})
    @EnableJpaRepositories(basePackageClasses = GrammarMasteryJpaRepository.class)
    @Import(GrammarMasteryRepositoryAdapter.class)
    static class GrammarSliceConfig {
    }
}
