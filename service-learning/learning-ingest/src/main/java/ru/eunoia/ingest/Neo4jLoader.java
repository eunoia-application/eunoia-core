package ru.eunoia.ingest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

/**
 * Загрузка накопленного графа в Neo4j штатным neo4j-java-driver. Всё идемпотентно
 * (CONSTRAINT + MERGE), UNWIND-батчами — тул можно гонять повторно без дублей.
 *
 * <p>Проход 1 — узлы Lexeme + их «листья» (формы HAS_FORM, переводы TRANSLATION).
 * Проход 2 — связи между словами (SYNONYM/ANTONYM/HYPERNYM), только между реально
 * импортированными лексемами — без висячих рёбер.
 *
 * <p>Схема узлов совпадает с той, что читает сервис learning (LexemeNode/FormNode/
 * TranslationNode): у Lexeme присвоенный бизнес-id, у форм/переводов id генерит сама база
 * (мы их не задаём — SDN на чтении берёт внутренний elementId).
 */
final class Neo4jLoader implements AutoCloseable {

    /** Размер UNWIND-батча — компромисс «мало round-trip'ов / не пухнет транзакция». */
    private static final int BATCH = 1000;

    private static final String NODE_CYPHER = """
            UNWIND $rows AS r
            MERGE (l:Lexeme {id: r.id})
            SET l.lemma = r.lemma, l.pos = r.pos, l.lang = 'en',
                l.freqRank = r.freqRank, l.cefr = r.cefr
            """;

    private static final String FORM_CYPHER = """
            UNWIND $rows AS r
            MATCH (l:Lexeme {id: r.id})
            UNWIND r.forms AS f
            MERGE (l)-[:HAS_FORM]->(:Form {text: f.text, feature: f.feature})
            """;

    private static final String TRANSLATION_CYPHER = """
            UNWIND $rows AS r
            MATCH (l:Lexeme {id: r.id})
            UNWIND r.translations AS t
            MERGE (l)-[:TRANSLATION]->(:Translation {text: t.text, lang: t.lang})
            """;

    private final Driver driver;
    private final String database;

    Neo4jLoader(String uri, String user, String pass, String database) {
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, pass));
        this.database = database;
        driver.verifyConnectivity();   // ранний внятный отказ, если Neo4j не поднят
    }

    /** Полная загрузка: констрейнт, затем узлы, затем связи. */
    void load(LexemeAccumulator acc) {
        List<LexemeData> all = new ArrayList<>(acc.lexemes());
        Set<String> imported = acc.importedIds();

        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            // 0. Уникальность бизнес-id узлов (как в KnowledgeSchemaInitializer сервиса).
            session.executeWriteWithoutResult(tx -> tx.run(
                    "CREATE CONSTRAINT lexeme_id IF NOT EXISTS "
                            + "FOR (l:Lexeme) REQUIRE l.id IS UNIQUE").consume());

            // 1. Узлы + формы + переводы, батчами.
            int loaded = 0;
            for (int from = 0; from < all.size(); from += BATCH) {
                List<LexemeData> batch = all.subList(from, Math.min(from + BATCH, all.size()));
                loadNodeBatch(session, batch);
                loaded += batch.size();
                System.err.printf("  узлы: %d/%d%n", loaded, all.size());
            }

            // 2. Связи (пары — только там, где обе стороны импортированы).
            loadEdges(session, "SYNONYM", pairs(all, imported, d -> d.synonymIds));
            loadEdges(session, "ANTONYM", pairs(all, imported, d -> d.antonymIds));
            loadEdges(session, "HYPERNYM", pairs(all, imported, d -> d.hypernymIds));
        }
    }

    private void loadNodeBatch(Session session, List<LexemeData> batch) {
        List<Map<String, Object>> rows = new ArrayList<>(batch.size());
        for (LexemeData d : batch) {
            List<Map<String, Object>> forms = new ArrayList<>(d.forms.size());
            for (Form f : d.forms) {
                forms.add(Map.of("text", f.text(), "feature", f.feature()));
            }
            List<Map<String, Object>> translations = new ArrayList<>(d.translations.size());
            for (Translation t : d.translations) {
                translations.add(Map.of("text", t.text(), "lang", t.lang()));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", d.id);
            row.put("lemma", d.lemma);
            row.put("pos", d.pos);
            row.put("freqRank", d.freqRank);
            row.put("cefr", d.cefr);
            row.put("forms", forms);
            row.put("translations", translations);
            rows.add(row);
        }
        Map<String, Object> params = Map.of("rows", rows);
        // Три запроса в одной транзакции: узлы, потом их формы, потом переводы.
        session.executeWriteWithoutResult(tx -> {
            tx.run(NODE_CYPHER, params).consume();
            tx.run(FORM_CYPHER, params).consume();
            tx.run(TRANSLATION_CYPHER, params).consume();
        });
    }

    /** Пары рёбер (from -> to) выбранного типа, отфильтрованные до импортированных узлов. */
    private static List<String[]> pairs(List<LexemeData> all, Set<String> imported,
                                        Function<LexemeData, Set<String>> pick) {
        List<String[]> out = new ArrayList<>();
        for (LexemeData d : all) {
            for (String target : pick.apply(d)) {
                if (imported.contains(target)) {   // без висячих рёбер
                    out.add(new String[] {d.id, target});
                }
            }
        }
        return out;
    }

    private void loadEdges(Session session, String type, List<String[]> pairs) {
        // Тип ребра нельзя параметризовать в Cypher — подставляем из своей константы (не из данных).
        String cypher = "UNWIND $pairs AS p "
                + "MATCH (a:Lexeme {id: p.from}), (b:Lexeme {id: p.to}) "
                + "MERGE (a)-[:" + type + "]->(b)";
        for (int from = 0; from < pairs.size(); from += BATCH) {
            List<String[]> batch = pairs.subList(from, Math.min(from + BATCH, pairs.size()));
            List<Map<String, Object>> rows = new ArrayList<>(batch.size());
            for (String[] p : batch) {
                rows.add(Map.of("from", p[0], "to", p[1]));
            }
            Map<String, Object> params = Map.of("pairs", rows);
            session.executeWriteWithoutResult(tx -> tx.run(cypher, params).consume());
        }
        System.err.printf("  связи %s: %d%n", type, pairs.size());
    }

    @Override
    public void close() {
        driver.close();
    }
}
