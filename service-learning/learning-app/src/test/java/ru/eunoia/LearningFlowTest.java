package ru.eunoia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.eunoia.application.learning.model.Cefr;
import com.eunoia.application.learning.model.Form;
import com.eunoia.application.learning.model.GardenLeaf;
import com.eunoia.application.learning.model.GrammarView;
import com.eunoia.application.learning.model.LexemeCard;
import com.eunoia.application.learning.model.LexemeRef;
import com.eunoia.application.learning.model.MasteryRequest;
import com.eunoia.application.learning.model.MasteryStatus;
import com.eunoia.application.learning.model.MasteryView;
import com.eunoia.application.learning.model.PartOfSpeech;
import com.eunoia.application.learning.model.TopicRef;
import com.eunoia.application.learning.model.TopicView;
import com.eunoia.application.learning.model.Translation;
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
 * и тем лежит в Neo4j, мой прогресс — в Postgres. Главное, что проверяем, — garden-view: отметил
 * слово «знаю» и тот же лист темы поменял цвет, хотя данные графов не смешиваются (склеиваем
 * только вид, по id слова). auth не поднимаем — JWT подменяем тестовым декодером (bearer = userId).
 * Требует запущенный Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@Testcontainers
@Import(LearningFlowTest.TestSecurityConfig.class)
class LearningFlowTest {

    private static final String GO = "en:go:VERB";
    private static final String MOVE = "en:move:VERB";

    private static final ParameterizedTypeReference<List<LexemeRef>> LEXEME_REFS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<TopicRef>> TOPIC_REFS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<MasteryView>> MASTERY_VIEWS =
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
                CREATE (go:Lexeme {id:'en:go:VERB', lemma:'go', pos:'VERB', lang:'en', cefr:'A1', freqRank:1})
                CREATE (went:Form {text:'went', feature:'past'})
                CREATE (goRu:Translation {text:'идти', lang:'ru'})
                CREATE (go)-[:HAS_FORM]->(went)
                CREATE (go)-[:TRANSLATION]->(goRu)
                CREATE (move:Lexeme {id:'en:move:VERB', lemma:'move', pos:'VERB', lang:'en', cefr:'A1', freqRank:2})
                CREATE (go)-[:SYNONYM]->(move)
                CREATE (movement:Topic {id:'movement', name:'Movement', slug:'movement'})
                CREATE (go)-[:IN_TOPIC]->(movement)
                CREATE (grammar:Grammar {id:'past-simple', name:'Past Simple', cefr:'A1'})
                """).run();
    }

    /**
     * Продуктовый сценарий: смотрю слово и ветку сада, отмечаю «знаю» — и та же ветка отдаётся
     * уже с новым статусом. Это и есть джойн Neo4j × Postgres на уровне вида.
     */
    @Test
    void gardenView_repaintsTopic_afterMasteryMark() {
        // карточка слова: атрибуты, формы, переводы и синонимы приехали из графа,
        // отметки ещё нет — значит UNKNOWN
        LexemeCard card = get("/learning/lexemes/" + GO, LexemeCard.class);
        assertThat(card.getLemma()).isEqualTo("go");
        assertThat(card.getPos()).isEqualTo(PartOfSpeech.VERB);
        assertThat(card.getCefr()).isEqualTo(Cefr.A1);
        assertThat(card.getFreqRank()).isEqualTo(1);
        assertThat(card.getForms()).extracting(Form::getText).contains("went");
        assertThat(card.getTranslations()).extracting(Translation::getText).contains("идти");
        assertThat(card.getSynonyms()).extracting(LexemeRef::getId).contains(MOVE);
        assertThat(card.getStatus()).isEqualTo(MasteryStatus.UNKNOWN);

        // поиск по префиксу леммы находит слово
        assertThat(getList("/learning/search?q=go", LEXEME_REFS))
                .extracting(LexemeRef::getId).contains(GO);

        // корневые темы отдаются списком
        assertThat(getList("/learning/topics", TOPIC_REFS))
                .extracting(TopicRef::getId).contains("movement");

        // ветка сада до отметки — лист серый
        TopicView before = get("/learning/topics/movement", TopicView.class);
        assertThat(before.getTopic().getName()).isEqualTo("Movement");
        assertThat(before.getTopic().getSlug()).isEqualTo("movement");
        assertThat(before.getLexemes())
                .extracting(GardenLeaf::getId, GardenLeaf::getStatus)
                .contains(tuple(GO, MasteryStatus.UNKNOWN));

        // отмечаем слово как известное
        MasteryRequest request = new MasteryRequest();
        request.setStatus(MasteryStatus.KNOWN);
        MasteryView saved = client().put().uri("/learning/mastery/" + GO)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MasteryView.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getLexemeId()).isEqualTo(GO);
        assertThat(saved.getStatus()).isEqualTo(MasteryStatus.KNOWN);
        assertThat(saved.getUpdatedAt()).isNotNull();

        // та же ветка сада — лист позеленел: структура из Neo4j, статус из Postgres
        TopicView after = get("/learning/topics/movement", TopicView.class);
        assertThat(after.getLexemes())
                .extracting(GardenLeaf::getId, GardenLeaf::getStatus)
                .contains(tuple(GO, MasteryStatus.KNOWN));

        // карточка слова и список моих отметок тоже знают про новый статус
        assertThat(get("/learning/lexemes/" + GO, LexemeCard.class).getStatus())
                .isEqualTo(MasteryStatus.KNOWN);
        assertThat(getList("/learning/mastery", MASTERY_VIEWS))
                .extracting(MasteryView::getLexemeId, MasteryView::getStatus)
                .contains(tuple(GO, MasteryStatus.KNOWN));
    }

    /** Грамматика читается из того же графа знаний. */
    @Test
    void getGrammar_returnsRuleFromGraph() {
        GrammarView grammar = get("/learning/grammar/past-simple", GrammarView.class);

        assertThat(grammar.getId()).isEqualTo("past-simple");
        assertThat(grammar.getName()).isEqualTo("Past Simple");
        assertThat(grammar.getCefr()).isEqualTo(Cefr.A1);
    }

    /** Нет узла в графе → доменное NotFoundException разворачивается в 404. */
    @Test
    void unknownLexeme_returns404() {
        assertThat(status("/learning/lexemes/en:nope:VERB").value()).isEqualTo(404);
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
