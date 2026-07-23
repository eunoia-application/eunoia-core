package ru.eunoia.persistence.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.neo4j.autoconfigure.DataNeo4jAutoConfiguration;
import org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.eunoia.application.knowledge.domain.model.Cefr;
import ru.eunoia.application.knowledge.domain.model.Form;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.Lexeme;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.RelationType;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Translation;
import ru.eunoia.persistence.knowledge.repository.LexemeNeo4jRepository;

/**
 * Смоук-тест Neo4j-адаптера на реальном Neo4j 5 (Testcontainers). Проверяем, что отображение
 * граф → домен работает end-to-end: карточка слова с формой/переводом, поиск по префиксу,
 * связи по типу ребра, слова темы, а также темы и грамматика. Требует запущенный Docker.
 *
 * <p>Контекст поднимаем «срезом» вручную — только Neo4j-автоконфиги + адаптер и репозитории —
 * чтобы не тянуть JPA/Eureka/security всего модулита. (Готовый срез {@code @DataNeo4jTest}
 * в Boot 4 переехал в отдельный модуль; собираем эквивалент из доступных автоконфигов.)
 */
@SpringBootTest(classes = LexiconRepositoryAdapterIT.KnowledgeSliceConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class LexiconRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community");

    @Autowired
    LexiconRepositoryAdapter adapter;

    @Autowired
    Neo4jClient client;

    /** Чистим и заново засеваем маленький граф перед каждым тестом. */
    @BeforeEach
    void seed() {
        client.query("MATCH (n) DETACH DELETE n").run();
        client.query("""
                CREATE (go:Lexeme {id:'go', lemma:'go', pos:'VERB', lang:'en', cefr:'A1', freqRank:1})
                CREATE (went:Form {text:'went', feature:'past'})
                CREATE (goRu:Translation {text:'идти', lang:'ru'})
                CREATE (go)-[:HAS_FORM]->(went)
                CREATE (go)-[:TRANSLATION]->(goRu)
                CREATE (postpone:Lexeme {id:'postpone', lemma:'postpone', pos:'VERB', lang:'en', cefr:'B2', freqRank:100})
                CREATE (delay:Lexeme {id:'delay', lemma:'delay', pos:'VERB', lang:'en', cefr:'B1', freqRank:50})
                CREATE (postpone)-[:SYNONYM]->(delay)
                CREATE (dog:Lexeme {id:'dog', lemma:'dog', pos:'NOUN', lang:'en', cefr:'A1', freqRank:10})
                CREATE (animal:Lexeme {id:'animal', lemma:'animal', pos:'NOUN', lang:'en', cefr:'A2', freqRank:200})
                CREATE (dog)-[:HYPERNYM]->(animal)
                CREATE (movement:Topic {id:'movement', name:'Movement', slug:'movement'})
                CREATE (go)-[:IN_TOPIC]->(movement)
                CREATE (grammar:Grammar {id:'past-simple', name:'Past Simple', cefr:'A1'})
                """).run();
    }

    @Test
    void findById_returnsFullCard_withFormAndTranslation() {
        Lexeme go = adapter.findById("go").orElseThrow();

        assertThat(go.id()).isEqualTo("go");
        assertThat(go.lemma()).isEqualTo("go");
        assertThat(go.pos()).isEqualTo(PartOfSpeech.VERB);
        assertThat(go.lang()).isEqualTo("en");
        assertThat(go.cefr()).isEqualTo(Cefr.A1);
        assertThat(go.freqRank()).isEqualTo(1);
        assertThat(go.forms())
                .extracting(Form::text, Form::feature)
                .containsExactly(tuple("went", "past"));
        assertThat(go.translations())
                .extracting(Translation::text, Translation::lang)
                .containsExactly(tuple("идти", "ru"));
    }

    @Test
    void findById_unknown_returnsEmpty() {
        assertThat(adapter.findById("does-not-exist")).isEmpty();
    }

    @Test
    void search_byLemmaPrefix_containsMatch() {
        var hits = adapter.search("go", 10);

        assertThat(hits).extracting(LexemeRef::id).contains("go");
        assertThat(hits).extracting(LexemeRef::pos).contains(PartOfSpeech.VERB);
    }

    @Test
    void related_bySynonym_containsDelay() {
        assertThat(adapter.related("postpone", RelationType.SYNONYM))
                .extracting(LexemeRef::id)
                .contains("delay");
    }

    @Test
    void related_byHypernym_containsAnimal() {
        assertThat(adapter.related("dog", RelationType.HYPERNYM))
                .extracting(LexemeRef::id)
                .contains("animal");
    }

    @Test
    void lexemesInTopic_containsGo() {
        assertThat(adapter.lexemesInTopic("movement"))
                .extracting(LexemeRef::id)
                .contains("go");
    }

    @Test
    void topicRoots_containsMovement() {
        assertThat(adapter.topicRoots())
                .extracting(Topic::id)
                .contains("movement");
    }

    @Test
    void findTopic_mapsNode() {
        Topic topic = adapter.findTopic("movement").orElseThrow();

        assertThat(topic.name()).isEqualTo("Movement");
        assertThat(topic.slug()).isEqualTo("movement");
    }

    @Test
    void findGrammar_mapsNode_withCefr() {
        Grammar grammar = adapter.findGrammar("past-simple").orElseThrow();

        assertThat(grammar.name()).isEqualTo("Past Simple");
        assertThat(grammar.cefr()).isEqualTo(Cefr.A1);
    }

    /**
     * Минимальный срез контекста: драйвер + SDN (Neo4jClient/шаблон/tx-менеджер) + наши
     * репозитории и адаптер. {@code @AutoConfigurationPackage} даёт сканеру найти @Node-узлы
     * в пакете {@code ru.eunoia.persistence.knowledge}.
     */
    @AutoConfigurationPackage
    @ImportAutoConfiguration({Neo4jAutoConfiguration.class, DataNeo4jAutoConfiguration.class})
    @EnableNeo4jRepositories(basePackageClasses = LexemeNeo4jRepository.class)
    @Import(LexiconRepositoryAdapter.class)
    static class KnowledgeSliceConfig {
    }
}
