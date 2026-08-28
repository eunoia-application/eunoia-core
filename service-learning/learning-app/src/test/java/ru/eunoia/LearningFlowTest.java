package ru.eunoia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.eunoia.application.learning.model.Band;
import com.eunoia.application.learning.model.Cefr;
import com.eunoia.application.learning.model.Form;
import com.eunoia.application.learning.model.GrammarView;
import com.eunoia.application.learning.model.MasteryRequest;
import com.eunoia.application.learning.model.MasteryStatus;
import com.eunoia.application.learning.model.MasteryView;
import com.eunoia.application.learning.model.PartOfSpeech;
import com.eunoia.application.learning.model.TopicRef;
import com.eunoia.application.learning.model.TopicView;
import com.eunoia.application.learning.model.Translation;
import com.eunoia.application.learning.model.TreeSnapshot;
import com.eunoia.application.learning.model.WordCard;
import com.eunoia.application.learning.model.WordLeaf;
import com.eunoia.application.learning.model.WordPage;
import com.eunoia.application.learning.model.WordRef;
import com.eunoia.application.learning.model.WordVariant;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.eunoia.persistence.knowledge.KnowledgeSchemaInitializer;

/**
 * Сквозная проверка учебного ядра на обоих реальных хранилищах (Testcontainers): структура слов
 * и тем лежит в Neo4j, мой прогресс — в Postgres. Единица — СЛОВО (лемма): карточка склеивает части
 * речи (variants), мастерство — по ключу леммы (en:go). Главное — garden-view: отметил слово «знаю»
 * и тот же лист темы поменял цвет, хотя данные графов не смешиваются. auth не поднимаем — JWT
 * подменяем тестовым декодером (bearer = userId). Требует запущенный Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@Testcontainers
@Import(LearningFlowTest.TestSecurityConfig.class)
class LearningFlowTest {

    private static final String GO = "en:go";       // ключ леммы (без части речи)
    private static final String MOVE = "en:move";

    private static final ParameterizedTypeReference<List<WordRef>> WORD_REFS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<TopicRef>> TOPIC_REFS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<MasteryView>> MASTERY_VIEWS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<GrammarView>> GRAMMAR_VIEWS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Band>> BANDS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<WordLeaf>> WORD_LEAVES =
            new ParameterizedTypeReference<>() { };

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community");

    @Value("${local.server.port}")
    int port;

    @Autowired
    Neo4jClient neo4jClient;

    @Autowired
    KnowledgeSchemaInitializer schemaInitializer;

    /** Свой пользователь на каждый тест — оверлей в Postgres между тестами не чистим. */
    private final UUID userId = UUID.randomUUID();

    /** Чистим и заново засеваем маленький граф знаний перед каждым тестом. */
    @BeforeEach
    void seedGraph() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        neo4jClient.query("""
                CREATE (go:Lexeme {id:'en:go:VERB', lemma:'go', pos:'VERB', lang:'en', cefr:'A1', freqRank:1, ipa:'/ɡəʊ/'})
                CREATE (went:Form {text:'went', feature:'past'})
                CREATE (goRu:Translation {text:'идти', lang:'ru'})
                CREATE (go)-[:HAS_FORM]->(went)
                CREATE (go)-[:TRANSLATION]->(goRu)
                CREATE (move:Lexeme {id:'en:move:VERB', lemma:'move', pos:'VERB', lang:'en', cefr:'A1', freqRank:2})
                CREATE (go)-[:SYNONYM]->(move)
                CREATE (movement:Topic {id:'movement', name:'Movement', slug:'movement'})
                CREATE (go)-[:IN_TOPIC]->(movement)
                CREATE (grammar:Grammar {id:'past-simple', name:'Past Simple', cefr:'A2'})
                CREATE (present:Grammar {id:'present-simple', name:'Present Simple', cefr:'A1'})
                CREATE (present)-[:PREREQUISITE]->(grammar)
                CREATE (go)-[:ILLUSTRATES]->(grammar)
                """).run();
    }

    /**
     * Продуктовый сценарий: смотрю слово и ветку сада, отмечаю «знаю» — и та же ветка отдаётся
     * уже с новым статусом. Это и есть джойн Neo4j × Postgres на уровне вида.
     */
    @Test
    void gardenView_repaintsTopic_afterMasteryMark() {
        // карточка слова-леммы: атрибуты + части речи (variants) с формами/переводами/связями;
        // отметки ещё нет — значит UNKNOWN
        WordCard card = get("/learning/words/" + GO, WordCard.class);
        assertThat(card.getId()).isEqualTo(GO);
        assertThat(card.getLemma()).isEqualTo("go");
        assertThat(card.getIpa()).isEqualTo("/ɡəʊ/");
        assertThat(card.getFreqRank()).isEqualTo(1);
        assertThat(card.getStatus()).isEqualTo(MasteryStatus.UNKNOWN);
        assertThat(card.getVariants()).hasSize(1);
        WordVariant verb = card.getVariants().get(0);
        assertThat(verb.getPos()).isEqualTo(PartOfSpeech.VERB);
        assertThat(verb.getCefr()).isEqualTo(Cefr.A1);
        assertThat(verb.getForms()).extracting(Form::getText).contains("went");
        assertThat(verb.getTranslations()).extracting(Translation::getText).contains("идти");
        assertThat(verb.getSynonyms()).extracting(WordRef::getId).contains(MOVE);

        // поиск по префиксу леммы находит слово
        assertThat(getList("/learning/search?q=go", WORD_REFS))
                .extracting(WordRef::getId).contains(GO);

        // все слова по частоте — go (1) и move (2)
        WordPage all = get("/learning/words?offset=0&limit=100", WordPage.class);
        assertThat(all.getTotal()).isEqualTo(2);
        assertThat(all.getWords()).extracting(WordLeaf::getId).contains(GO, MOVE);

        // корневые темы отдаются списком
        assertThat(getList("/learning/topics", TOPIC_REFS))
                .extracting(TopicRef::getId).contains("movement");

        // ветка сада до отметки — лист серый
        TopicView before = get("/learning/topics/movement", TopicView.class);
        assertThat(before.getTopic().getName()).isEqualTo("Movement");
        assertThat(before.getWords())
                .extracting(WordLeaf::getId, WordLeaf::getStatus)
                .contains(tuple(GO, MasteryStatus.UNKNOWN));

        // отмечаем слово (лемму) как известное
        MasteryRequest request = new MasteryRequest();
        request.setStatus(MasteryStatus.KNOWN);
        MasteryView saved = client().put().uri("/learning/mastery/" + GO)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MasteryView.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getWordId()).isEqualTo(GO);
        assertThat(saved.getStatus()).isEqualTo(MasteryStatus.KNOWN);
        assertThat(saved.getUpdatedAt()).isNotNull();

        // та же ветка сада — лист позеленел: структура из Neo4j, статус из Postgres
        TopicView after = get("/learning/topics/movement", TopicView.class);
        assertThat(after.getWords())
                .extracting(WordLeaf::getId, WordLeaf::getStatus)
                .contains(tuple(GO, MasteryStatus.KNOWN));

        // карточка слова и список моих отметок тоже знают про новый статус
        assertThat(get("/learning/words/" + GO, WordCard.class).getStatus())
                .isEqualTo(MasteryStatus.KNOWN);
        assertThat(getList("/learning/mastery", MASTERY_VIEWS))
                .extracting(MasteryView::getWordId, MasteryView::getStatus)
                .contains(tuple(GO, MasteryStatus.KNOWN));
    }

    /**
     * Снапшот дерева одним вызовом: отметка «знаю» отражается и в словаре (листья), и в ветке-теме
     * (movement), и в журнале активности (сегодня был день → стрик 1). Реальный джойн двух графов.
     */
    @Test
    void tree_snapshotReflectsMarkAndActivity() {
        MasteryRequest req = new MasteryRequest();
        req.setStatus(MasteryStatus.KNOWN);
        client().put().uri("/learning/mastery/" + GO)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(req).retrieve().body(MasteryView.class);

        TreeSnapshot tree = get("/learning/tree", TreeSnapshot.class);

        assertThat(tree.getVocabulary().getKnown()).isEqualTo(1);
        assertThat(tree.getVocabulary().getLearning()).isZero();
        assertThat(tree.getVocabulary().getTotal()).isEqualTo(2);   // go, move
        assertThat(tree.getTopics())
                .filteredOn(t -> t.getId().equals("movement"))
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.getKnown()).isEqualTo(1);   // go отмечен и лежит в movement
                    assertThat(t.getTotal()).isEqualTo(1);
                });
        assertThat(tree.getActivity().getStreak()).isEqualTo(1);
        assertThat(tree.getActivity().getDaysActive30()).isEqualTo(1);
        assertThat(tree.getActivity().getLastActiveDate()).isNotNull();
        // грамматику ещё не отмечал: 2 правила в стволе (present/past-simple), 0 освоено
        assertThat(tree.getGrammar().getTotal()).isEqualTo(2);
        assertThat(tree.getGrammar().getKnown()).isZero();
    }

    /**
     * Прогресс грамматики (высота дерева): отметил правило «знаю» — статус виден в ствол-виде и в
     * снапшоте (grammar.known=1). Мастерство правила — параллель словесному, через grammar_mastery.
     */
    @Test
    void grammarMastery_marksRule_reflectedInViewAndTree() {
        // до отметки — правило серое
        assertThat(get("/learning/grammar/present-simple", GrammarView.class).getStatus())
                .isEqualTo(MasteryStatus.UNKNOWN);

        MasteryRequest req = new MasteryRequest();
        req.setStatus(MasteryStatus.KNOWN);
        GrammarView marked = client().put().uri("/learning/grammar/mastery/present-simple")
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(req).retrieve().body(GrammarView.class);
        assertThat(marked).isNotNull();
        assertThat(marked.getStatus()).isEqualTo(MasteryStatus.KNOWN);

        // ствол грамматики и снапшот знают про новый статус
        assertThat(getList("/learning/grammar", GRAMMAR_VIEWS))
                .filteredOn(g -> g.getId().equals("present-simple"))
                .singleElement()
                .satisfies(g -> assertThat(g.getStatus()).isEqualTo(MasteryStatus.KNOWN));
        assertThat(get("/learning/tree", TreeSnapshot.class).getGrammar().getKnown()).isEqualTo(1);
    }

    /**
     * Весь ствол грамматики: правила по возрастанию CEFR (present-simple A1 → past-simple A2),
     * с порядком изучения (prerequisites). Слова-примеры в списке не грузим.
     */
    @Test
    void listGrammar_returnsTrunkOrderedByCefr_withPrereqs() {
        List<GrammarView> trunk = getList("/learning/grammar", GRAMMAR_VIEWS);

        assertThat(trunk).extracting(GrammarView::getId)
                .containsExactly("present-simple", "past-simple");
        assertThat(trunk).filteredOn(g -> g.getId().equals("past-simple"))
                .singleElement()
                .satisfies(g -> assertThat(g.getPrerequisites()).containsExactly("present-simple"));
        assertThat(trunk).allSatisfy(g -> assertThat(g.getIllustratedBy()).isEmpty());
    }

    /** Правило по id: атрибуты + предшественники (PREREQUISITE) + слова-примеры (ILLUSTRATES → ключ леммы). */
    @Test
    void getGrammar_returnsRuleWithPrereqsAndExamples() {
        GrammarView grammar = get("/learning/grammar/past-simple", GrammarView.class);

        assertThat(grammar.getId()).isEqualTo("past-simple");
        assertThat(grammar.getName()).isEqualTo("Past Simple");
        assertThat(grammar.getCefr()).isEqualTo(Cefr.A2);
        assertThat(grammar.getPrerequisites()).containsExactly("present-simple");
        assertThat(grammar.getIllustratedBy()).extracting(WordRef::getId).contains(GO);
    }

    /** Блоки топ-слов (уровни) с прогрессом: go(ранг 1) и move(ранг 2) — оба в топ-100. */
    @Test
    void bands_listBlocks_top100ContainsSeededWords() {
        List<Band> bands = getList("/learning/bands", BANDS);

        assertThat(bands).extracting(Band::getId)
                .containsExactly("top-100", "top-500", "top-1000", "top-3000", "top-5000", "top-10000");
        assertThat(bands).filteredOn(b -> b.getId().equals("top-100"))
                .singleElement()
                .satisfies(b -> assertThat(b.getTotal()).isEqualTo(2));
    }

    /** «Учить» кладёт слово в очередь на изучение (статус LEARNING) — /learning/study его возвращает. */
    @Test
    void study_listsWordsMarkedToLearn() {
        MasteryRequest request = new MasteryRequest();
        request.setStatus(MasteryStatus.LEARNING);
        client().put().uri("/learning/mastery/" + GO)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MasteryView.class);

        List<WordLeaf> study = getList("/learning/study", WORD_LEAVES);

        assertThat(study).extracting(WordLeaf::getId).contains(GO);
        assertThat(study).allSatisfy(w -> assertThat(w.getStatus()).isEqualTo(MasteryStatus.LEARNING));
    }

    /** Нет слова в графе → доменное NotFoundException разворачивается в 404. */
    @Test
    void unknownWord_returns404() {
        assertThat(status("/learning/words/en:nope").value()).isEqualTo(404);
    }

    /** Нет темы → 404 (та же ветка обработчика, другой use case). */
    @Test
    void unknownTopic_returns404() {
        assertThat(status("/learning/topics/nope").value()).isEqualTo(404);
    }

    /** learning — resource-server: без токена учёба закрыта. */
    @Test
    void withoutToken_returns401() {
        HttpStatusCode code = client().get().uri("/learning/topics")
                .exchange((request, response) -> response.getStatusCode());

        assertThat(code.value()).isEqualTo(401);
    }

    /**
     * Схемой графа владеет сам сервис: констрейнты и индекс ставятся на старте. Повторный запуск
     * идемпотентен (IF NOT EXISTS), поэтому гоняем инициализатор ещё раз и проверяем, что схема
     * на месте и ничего не сломалось.
     */
    @Test
    void schemaInitializer_isIdempotent() {
        schemaInitializer.run(null);

        assertThat(names("SHOW CONSTRAINTS YIELD name RETURN name"))
                .contains("lexeme_id", "topic_id", "grammar_id");
        assertThat(names("SHOW INDEXES YIELD name RETURN name"))
                .contains("lexeme_lemma");
    }

    // --- хелперы ---

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private <T> T get(String uri, Class<T> type) {
        return client().get().uri(uri)
                .header("Authorization", "Bearer " + userId)
                .retrieve()
                .body(type);
    }

    private <T> List<T> getList(String uri, ParameterizedTypeReference<List<T>> type) {
        return client().get().uri(uri)
                .header("Authorization", "Bearer " + userId)
                .retrieve()
                .body(type);
    }

    /** Статус без раскрытия тела — чтобы 4xx не бросали исключение. */
    private HttpStatusCode status(String uri) {
        return client().get().uri(uri)
                .header("Authorization", "Bearer " + userId)
                .exchange((request, response) -> response.getStatusCode());
    }

    /** Имена объектов схемы графа (SHOW CONSTRAINTS / SHOW INDEXES). */
    private Collection<String> names(String cypher) {
        return neo4jClient.query(cypher)
                .fetchAs(String.class)
                .mappedBy((typeSystem, row) -> row.get("name").asString())
                .all();
    }

    /** Тестовый декодер: bearer-токен = userId (auth не поднимаем, подпись не проверяем). */
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("userId", token)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
        }
    }
}
