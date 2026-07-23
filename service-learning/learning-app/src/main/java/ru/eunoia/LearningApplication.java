package ru.eunoia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Учебное ядро (модулит). Два хранилища в одном развёртывании:
 *   - Neo4j — канонический граф знаний (контекст knowledge);
 *   - Postgres — персональный оверлей мастерства (контекст garden).
 * Репозитории разведены по пакетам, чтобы Spring Data не путал стораджи
 * (Neo4j сканирует persistence.knowledge, JPA — persistence.garden).
 */
@SpringBootApplication
@EnableNeo4jRepositories(basePackages = "ru.eunoia.persistence.knowledge")
@EnableJpaRepositories(basePackages = "ru.eunoia.persistence.garden")
public class LearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningApplication.class, args);
    }
}
