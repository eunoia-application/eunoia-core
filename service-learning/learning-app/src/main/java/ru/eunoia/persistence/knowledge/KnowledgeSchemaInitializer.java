package ru.eunoia.persistence.knowledge;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Схемой графа владеет сам сервис: констрейнты и индексы создаём на старте, идемпотентно
 * (IF NOT EXISTS — повторный запуск ничего не ломает). Полноценные версионируемые миграции —
 * позже, если понадобится версионирование схемы графа.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeSchemaInitializer implements ApplicationRunner {

    private final Neo4jClient client;

    @Override
    public void run(ApplicationArguments args) {
        // Уникальность бизнес-id узлов с присвоенным id.
        client.query("CREATE CONSTRAINT lexeme_id IF NOT EXISTS FOR (l:Lexeme) REQUIRE l.id IS UNIQUE").run();
        client.query("CREATE CONSTRAINT topic_id IF NOT EXISTS FOR (t:Topic) REQUIRE t.id IS UNIQUE").run();
        client.query("CREATE CONSTRAINT grammar_id IF NOT EXISTS FOR (g:Grammar) REQUIRE g.id IS UNIQUE").run();
        // Ускоряем поиск слов по префиксу леммы.
        client.query("CREATE INDEX lexeme_lemma IF NOT EXISTS FOR (l:Lexeme) ON (l.lemma)").run();
    }
}
