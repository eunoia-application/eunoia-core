package ru.eunoia.persistence.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Translation;
import ru.eunoia.application.knowledge.domain.model.Word;
import ru.eunoia.application.knowledge.domain.model.WordRef;
import ru.eunoia.application.knowledge.domain.model.WordSummary;
import ru.eunoia.persistence.knowledge.repository.LexemeNeo4jRepository;

/**
 * Смоук-тест Neo4j-адаптера на реальном Neo4j 5 (Testcontainers). Единица — СЛОВО (лемма):
 * findWord склеивает POS-узлы одной леммы в Word с variants (формы/переводы/связи), а списки
 * (поиск/тема/все) — distinct по лемме. Требует запущенный Docker.
 *
 * <p>Контекст поднимаем «срезом» вручную — только Neo4j-автоконфиги + адаптер и репозитории.
 * (Готовый {@code @DataNeo4jTest} в Boot 4 уехал в отдельный модуль; собираем эквивалент.)
 */
@SpringBootTest(classes = LexiconRepositoryAdapterTest.KnowledgeSliceConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class LexiconRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community");

    @Autowired
    LexiconRepositoryAdapter adapter;

    @Autowired
    Neo4jClient client;

    /** Чистим и заново засеваем маленький граф перед каждым тестом (id вида en:lemma:POS). */
    @BeforeEach
    void seed() {
        client.query("MATCH (n) DETACH DELETE n").run();
        client.query("""
                CREATE (go:Lexeme {id:'en:go:VERB', lemma:'go', pos:'VERB', lang:'en', cefr:'A1', freqRank:1, ipa:'/ɡoʊ/'})
                CREATE (went:Form {text:'went', feature:'past'})
                CREATE (goRu:Translation {text:'идти', lang:'ru'})
                CREATE (go)-[:HAS_FORM]->(went)
                CREATE (go)-[:TRANSLATION]->(goRu)
                CREATE (goNoun:Lexeme {id:'en:go:NOUN', lemma:'go', pos:'NOUN', lang:'en', cefr:'A2', freqRank:5})
                CREATE (postpone:Lexeme {id:'en:postpone:VERB', lemma:'postpone', pos:'VERB', lang:'en', cefr:'B2', freqRank:100})
                CREATE (delay:Lexeme {id:'en:delay:VERB', lemma:'delay', pos:'VERB', lang:'en', cefr:'B1', freqRank:50})
                CREATE (postpone)-[:SYNONYM]->(delay)
                CREATE (dog:Lexeme {id:'en:dog:NOUN', lemma:'dog', pos:'NOUN', lang:'en', cefr:'A1', freqRank:10})
                CREATE (animal:Lexeme {id:'en:animal:NOUN', lemma:'animal', pos:'NOUN', lang:'en', cefr:'A2', freqRank:200})
                CREATE (dog)-[:HYPERNYM]->(animal)
                CREATE (movement:Topic {id:'movement', name:'Movement', slug:'movement'})
                CREATE (go)-[:IN_TOPIC]->(movement)
                CREATE (grammar:Grammar {id:'past-simple', name:'Past Simple', cefr:'A1'})
                """).run();
    }

    @Test
    void findWord_aggregatesVariants_withFormsTranslations() {
        Word go = adapter.findWord("en:go").orElseThrow();

        assertThat(go.id()).isEqualTo("en:go");
        assertThat(go.lemma()).isEqualTo("go");
        assertThat(go.ipa()).isEqualTo("/ɡoʊ/");
        assertThat(go.freqRank()).isEqualTo(1);   // минимум среди частей речи (1 и 5)
        assertThat(go.variants()).extracting(v -> v.pos())
                .contains(PartOfSpeech.VERB, PartOfSpeech.NOUN);
        var verb = go.variants().stream().filter(v -> v.pos() == PartOfSpeech.VERB).findFirst().orElseThrow();
        assertThat(verb.forms()).extracting(Form::text).contains("went");
        assertThat(verb.translations()).extracting(Translation::text).contains("идти");
    }

    @Test
    void findWord_unknown_returnsEmpty() {
        assertThat(adapter.findWord("en:does-not-exist")).isEmpty();
    }

    @Test
    void searchWords_byLemmaPrefix_distinctLemma() {
        var hits = adapter.searchWords("go", 10);

        assertThat(hits).extracting(WordRef::id).contains("en:go");
        assertThat(hits).extracting(WordRef::lemma).contains("go");
    }

    @Test
    void findWord_synonym_variantContainsDelay() {
        Word postpone = adapter.findWord("en:postpone").orElseThrow();

        assertThat(postpone.variants().get(0).synonyms())
                .extracting(WordRef::id).contains("en:delay");
    }

    @Test
    void findWord_hypernym_variantContainsAnimal() {
        Word dog = adapter.findWord("en:dog").orElseThrow();

        assertThat(dog.variants().get(0).hypernyms())
                .extracting(WordRef::id).contains("en:animal");
    }

    @Test
    void wordsInTopic_containsGo_withPos() {
        var words = adapter.wordsInTopic("movement");

        assertThat(words).extracting(WordSummary::id).contains("en:go");
        assertThat(words).filteredOn(w -> w.id().equals("en:go"))
                .singleElement()
                .satisfies(w -> assertThat(w.pos()).contains(PartOfSpeech.VERB));
    }

    @Test
    void allWords_ordersByFrequency_andCountsDistinctLemmas() {
        var words = adapter.allWords(0, 100);

        assertThat(words).extracting(WordSummary::id).contains("en:go", "en:dog", "en:animal");
        assertThat(words.get(0).id()).isEqualTo("en:go");   // ранг 1 — первым
        assertThat(adapter.countWords()).isEqualTo(5L);      // go, postpone, delay, dog, animal
    }

    @Test
    void topicRoots_containsMovement() {
        assertThat(adapter.topicRoots()).extracting(Topic::id).contains("movement");
    }

    @Test
    void topicWordCounts_countsDistinctLemmasPerTopic() {
        // в movement связан go (VERB) → одна уникальная лемма
        assertThat(adapter.topicWordCounts()).containsEntry("movement", 1L);
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

    @Test
    void wordsInTopic_carriesWordTopics() {
        assertThat(adapter.wordsInTopic("movement"))
                .filteredOn(w -> w.id().equals("en:go"))
                .singleElement()
                .satisfies(w -> assertThat(w.topics()).extracting(Topic::id).contains("movement"));
    }

    @Test
    void wordsInRank_andCount_byFrequencyWindow() {
        // ранги: go=1, dog=10 → в [1,10]; delay=50, postpone=100, animal=200 — вне
        assertThat(adapter.countWordsInRank(1, 10)).isEqualTo(2L);
        assertThat(adapter.wordsInRank(1, 10, 0, 100))
                .extracting(WordSummary::id).containsExactly("en:go", "en:dog");
    }

    @Test
    void ranksOf_returnsMinRankPerLemmaKey() {
        assertThat(adapter.ranksOf(List.of("en:go", "en:dog")))
                .containsEntry("en:go", 1)     // min(1, 5) по частям речи
                .containsEntry("en:dog", 10);
    }

    @Test
    void wordsByKeys_returnsSummaries() {
        assertThat(adapter.wordsByKeys(List.of("en:go")))
                .extracting(WordSummary::id).containsExactly("en:go");
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
