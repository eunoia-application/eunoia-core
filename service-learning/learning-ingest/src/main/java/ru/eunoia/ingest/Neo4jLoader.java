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
 * импортированными лексемами — без висячих рёбер. Проход 3 — темы: узлы Topic из
 * каталога, иерархия SUBTOPIC и рёбра IN_TOPIC (слово → его ветка). Проход 4 —
 * грамматика: узлы Grammar, порядок PREREQUISITE и ILLUSTRATES (слово → правило).
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
                l.freqRank = r.freqRank, l.cefr = r.cefr, l.ipa = r.ipa
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

    /** Полная загрузка: констрейнты, узлы, связи между словами, темы, грамматика. */
    void load(LexemeAccumulator acc, TopicCatalog topics, GrammarCatalog grammar) {
        List<LexemeData> all = new ArrayList<>(acc.lexemes());
        Set<String> imported = acc.importedIds();

        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            // 0. Уникальность бизнес-id узлов (как в KnowledgeSchemaInitializer сервиса).
            constraint(session, "lexeme_id", "Lexeme");
            constraint(session, "topic_id", "Topic");
            constraint(session, "grammar_id", "Grammar");

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

            // 3. Темы: узлы Topic + иерархия SUBTOPIC + рёбра IN_TOPIC (слово → ветка).
            loadTopics(session, all, topics);

            // 4. Грамматика: узлы Grammar + порядок PREREQUISITE + ILLUSTRATES (слово → правило).
            loadGrammar(session, all, grammar);
        }
    }

    /** Идемпотентный констрейнт уникальности бизнес-id узла (как в схеме сервиса). */
    private static void constraint(Session session, String name, String label) {
        session.executeWriteWithoutResult(tx -> tx.run(
                "CREATE CONSTRAINT " + name + " IF NOT EXISTS "
                        + "FOR (n:" + label + ") REQUIRE n.id IS UNIQUE").consume());
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
            row.put("cefr", LexemeAccumulator.cefrOf(d.freqRank));   // из свёрнутого ранга
            row.put("ipa", d.ipa);                                   // может быть null
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

    /**
     * Темы сада: узлы Topic из каталога, иерархия SUBTOPIC (по parentId) и рёбра IN_TOPIC
     * (слово → его ветка). Пустой каталог → ничего не делаем.
     */
    private void loadTopics(Session session, List<LexemeData> all, TopicCatalog topics) {
        if (topics.branches().isEmpty()) {
            return;
        }
        // Узлы веток + иерархия SUBTOPIC.
        List<Map<String, Object>> topicRows = new ArrayList<>();
        List<Map<String, Object>> subRows = new ArrayList<>();
        for (TopicCatalog.Branch b : topics.branches()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.id());
            row.put("name", b.name());
            row.put("slug", b.slug());
            topicRows.add(row);
            if (!b.parentId().isEmpty()) {
                subRows.add(Map.of("parent", b.parentId(), "child", b.id()));
            }
        }
        session.executeWriteWithoutResult(tx -> {
            tx.run("UNWIND $rows AS r MERGE (t:Topic {id: r.id}) SET t.name = r.name, t.slug = r.slug",
                    Map.of("rows", topicRows)).consume();
            if (!subRows.isEmpty()) {
                tx.run("UNWIND $rows AS r MATCH (p:Topic {id: r.parent}), (c:Topic {id: r.child}) "
                        + "MERGE (p)-[:SUBTOPIC]->(c)", Map.of("rows", subRows)).consume();
            }
        });

        // Рёбра IN_TOPIC (слово → ветка), батчами.
        List<Map<String, Object>> links = new ArrayList<>();
        for (LexemeData d : all) {
            if (d.topicId != null) {
                links.add(Map.of("lex", d.id, "topic", d.topicId));
            }
        }
        for (int from = 0; from < links.size(); from += BATCH) {
            List<Map<String, Object>> batch = links.subList(from, Math.min(from + BATCH, links.size()));
            session.executeWriteWithoutResult(tx -> tx.run(
                    "UNWIND $rows AS r MATCH (l:Lexeme {id: r.lex}), (t:Topic {id: r.topic}) "
                            + "MERGE (l)-[:IN_TOPIC]->(t)", Map.of("rows", batch)).consume());
        }
        System.err.printf("  темы: узлов %d, IN_TOPIC %d%n", topicRows.size(), links.size());
    }

    /**
     * Грамматика: узлы Grammar из скелета, порядок PREREQUISITE и рёбра ILLUSTRATES
     * (слово → правило, которое оно иллюстрирует). Пустой скелет → ничего не делаем.
     */
    private void loadGrammar(Session session, List<LexemeData> all, GrammarCatalog grammar) {
        if (grammar.rules().isEmpty()) {
            return;
        }
        // Узлы правил + порядок PREREQUISITE.
        List<Map<String, Object>> ruleRows = new ArrayList<>();
        List<Map<String, Object>> prereqRows = new ArrayList<>();
        for (GrammarCatalog.Rule r : grammar.rules()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.id());
            row.put("name", r.name());
            row.put("cefr", r.cefr());
            ruleRows.add(row);
            if (!r.prereqId().isEmpty()) {
                prereqRows.add(Map.of("prereq", r.prereqId(), "rule", r.id()));
            }
        }
        session.executeWriteWithoutResult(tx -> {
            tx.run("UNWIND $rows AS r MERGE (g:Grammar {id: r.id}) SET g.name = r.name, g.cefr = r.cefr",
                    Map.of("rows", ruleRows)).consume();
            if (!prereqRows.isEmpty()) {
                tx.run("UNWIND $rows AS r MATCH (p:Grammar {id: r.prereq}), (g:Grammar {id: r.rule}) "
                        + "MERGE (p)-[:PREREQUISITE]->(g)", Map.of("rows", prereqRows)).consume();
            }
        });

        // Рёбра ILLUSTRATES (слово → правило), батчами.
        List<Map<String, Object>> links = new ArrayList<>();
        for (LexemeData d : all) {
            for (String gid : d.grammarIds) {
                links.add(Map.of("lex", d.id, "grammar", gid));
            }
        }
        for (int from = 0; from < links.size(); from += BATCH) {
            List<Map<String, Object>> batch = links.subList(from, Math.min(from + BATCH, links.size()));
            session.executeWriteWithoutResult(tx -> tx.run(
                    "UNWIND $rows AS r MATCH (l:Lexeme {id: r.lex}), (g:Grammar {id: r.grammar}) "
                            + "MERGE (l)-[:ILLUSTRATES]->(g)", Map.of("rows", batch)).consume());
        }
        System.err.printf("  грамматика: правил %d, ILLUSTRATES %d%n", ruleRows.size(), links.size());
    }

    @Override
    public void close() {
        driver.close();
    }
}
