package ru.eunoia.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * В модулите два хранилища — значит нужны ДВА транзакционных менеджера, и объявить их
 * приходится руками.
 *
 * <p>Почему: и JPA-, и Neo4j-автоконфиг Boot объявляют свой менеджер через
 * {@code @ConditionalOnMissingBean}, поэтому создаётся только один — JPA успевает первым,
 * а Neo4j-менеджера в контексте нет. При этом Spring Data Neo4j ищет бин <b>именно типа</b>
 * {@link Neo4jTransactionManager}; не найдя его, оставляет {@code Neo4jTemplate} без
 * transaction-template — и первый же запрос к графу падает с NPE.
 */
@Configuration
public class TransactionConfig {

    /** garden (Postgres/JPA) — основной: {@code @Transactional} без квалификатора идёт сюда. */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /** knowledge (Neo4j) — его находит Neo4jTemplate по типу; provider выбирает нужную базу. */
    @Bean
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver,
                                                           DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}
