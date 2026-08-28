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
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.persistence.garden.repository.MasteryJpaRepository;

/**
 * Смоук-тест garden-адаптера на реальном Postgres (Testcontainers). Проверяем, что персональный
 * оверлей действительно ложится в таблицу mastery: отметка сохраняется и читается, повторная
 * отметка тем же ключом (userId+lexemeId) перезаписывает статус, а не плодит строки, и чужие
 * отметки в мои выборки не попадают. Требует запущенный Docker.
 *
 * <p>Контекст поднимаем «срезом» вручную — DataSource из контейнера + Hibernate + Liquibase
 * (таблицу создаёт changelog) + наш репозиторий и адаптер, — чтобы не тянуть Neo4j/web/security
 * всего модулита. (Готовый срез {@code @DataJpaTest} в Boot 4 переехал в отдельный модуль;
 * собираем эквивалент из доступных автоконфигов — как в LexiconRepositoryAdapterTest.)
 */
@SpringBootTest(classes = MasteryRepositoryAdapterTest.GardenSliceConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class MasteryRepositoryAdapterTest {

    /** lexemeId — id узла канонического графа, garden хранит его строкой без FK. */
    private static final String GO = "en:go:VERB";
    private static final String RUN = "en:run:VERB";
    private static final LocalDateTime MARKED_AT = LocalDateTime.of(2026, 7, 23, 12, 0);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MasteryRepositoryAdapter adapter;

    @Autowired
    MasteryJpaRepository jpaRepository;

    /** Свои пользователи на каждый тест (JUnit создаёт экземпляр класса на каждый метод). */
    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    /** Контейнер один на класс — чистим таблицу перед каждым тестом. */
    @BeforeEach
    void clean() {
        jpaRepository.deleteAll();
    }

    @Test
    void save_thenFind_returnsStoredMark() {
        Mastery mark = new Mastery(userId, GO, MasteryStatus.LEARNING, MARKED_AT);

        assertThat(adapter.save(mark)).isEqualTo(mark);
        assertThat(adapter.find(userId, GO)).contains(mark);
    }

    /** Ключ присвоенный (userId+lexemeId) — повторная отметка перезаписывает статус, а не дублирует строку. */
    @Test
    void save_sameKeyAgain_upsertsStatus() {
        adapter.save(new Mastery(userId, GO, MasteryStatus.LEARNING, MARKED_AT));

        Mastery updated = new Mastery(userId, GO, MasteryStatus.KNOWN, MARKED_AT.plusDays(1));
        adapter.save(updated);

        assertThat(adapter.find(userId, GO)).contains(updated);
        assertThat(adapter.findByUser(userId)).hasSize(1);
    }

    @Test
    void find_missing_returnsEmpty() {
        assertThat(adapter.find(userId, GO)).isEmpty();
    }

    @Test
    void findByUser_returnsAllMyMarks_andOnlyMine() {
        adapter.save(new Mastery(userId, GO, MasteryStatus.KNOWN, MARKED_AT));
        adapter.save(new Mastery(userId, RUN, MasteryStatus.LEARNING, MARKED_AT));
        adapter.save(new Mastery(otherUserId, GO, MasteryStatus.KNOWN, MARKED_AT));

        assertThat(adapter.findByUser(userId))
                .extracting(Mastery::lexemeId, Mastery::status)
                .containsExactlyInAnyOrder(
                        tuple(GO, MasteryStatus.KNOWN),
                        tuple(RUN, MasteryStatus.LEARNING));
    }

    /** Раскраска сада: по набору слов отдаём карту статусов; неотмеченных слов в карте нет. */
    @Test
    void statusesFor_mapsMarkedIds_only() {
        adapter.save(new Mastery(userId, GO, MasteryStatus.KNOWN, MARKED_AT));
        adapter.save(new Mastery(userId, RUN, MasteryStatus.LEARNING, MARKED_AT));
        adapter.save(new Mastery(otherUserId, GO, MasteryStatus.UNKNOWN, MARKED_AT));

        Map<String, MasteryStatus> statuses =
                adapter.statusesFor(userId, List.of(GO, RUN, "en:nope:VERB"));

        assertThat(statuses).containsExactlyInAnyOrderEntriesOf(
                Map.of(GO, MasteryStatus.KNOWN, RUN, MasteryStatus.LEARNING));
    }

    /** Пустой набор слов — короткое замыкание: пустая карта, пустой IN в базу не уходит. */
    @Test
    void statusesFor_emptyIds_returnsEmptyMap() {
        adapter.save(new Mastery(userId, GO, MasteryStatus.KNOWN, MARKED_AT));

        assertThat(adapter.statusesFor(userId, List.of())).isEmpty();
    }

    /**
     * Минимальный срез контекста: DataSource (адрес берётся из контейнера через
     * {@code @ServiceConnection}), Hibernate, Liquibase (создаёт таблицу mastery) и наши
     * репозиторий с адаптером. {@code @AutoConfigurationPackage} даёт Hibernate найти
     * {@code @Entity} в пакете {@code ru.eunoia.persistence.garden}.
     */
    @AutoConfigurationPackage
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class})
    @EnableJpaRepositories(basePackageClasses = MasteryJpaRepository.class)
    @Import(MasteryRepositoryAdapter.class)
    static class GardenSliceConfig {
    }
}
