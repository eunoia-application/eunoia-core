package ru.eunoia.persistence.knowledge;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import ru.eunoia.application.knowledge.domain.model.Cefr;
import ru.eunoia.application.knowledge.domain.model.Form;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.LexemeVariant;
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Translation;
import ru.eunoia.application.knowledge.domain.model.Word;
import ru.eunoia.application.knowledge.domain.model.WordRef;
import ru.eunoia.application.knowledge.domain.model.WordSummary;
import ru.eunoia.application.knowledge.port.out.LexiconRepositoryPort;
import ru.eunoia.persistence.knowledge.entity.GrammarNode;
import ru.eunoia.persistence.knowledge.entity.LexemeNode;
import ru.eunoia.persistence.knowledge.entity.TopicNode;
import ru.eunoia.persistence.knowledge.repository.GrammarNeo4jRepository;
import ru.eunoia.persistence.knowledge.repository.LexemeNeo4jRepository;
import ru.eunoia.persistence.knowledge.repository.TopicNeo4jRepository;

/**
 * Neo4j-адаптер чтения канонического графа. Единица — СЛОВО (лемма): {@link #findWord} собирает
 * все POS-узлы одной леммы в {@link Word} с variants. Списки (поиск/тема/все/блок) — distinct по
 * лемме через {@link Neo4jClient} (агрегаты min/collect), с темами слова для разбивки по категориям.
 */
@Component
@RequiredArgsConstructor
public class LexiconRepositoryAdapter implements LexiconRepositoryPort {

    /** Общий «хвост» агрегата слова: свернуть по лемме + собрать части речи и темы. */
    private static final String WORD_AGG = """
            OPTIONAL MATCH (l)-[:IN_TOPIC]->(t:Topic)
            WITH l.lemma AS lemma, min(l.freqRank) AS rank,
                 collect(DISTINCT l.pos) AS pos, min(l.cefr) AS cefr, collect(DISTINCT t) AS ts
            RETURN lemma, rank, pos, cefr, [x IN ts | {id:x.id, name:x.name, slug:x.slug}] AS topics
            """;

    private final LexemeNeo4jRepository lexemeRepo;
    private final TopicNeo4jRepository topicRepo;
    private final GrammarNeo4jRepository grammarRepo;
    private final Neo4jClient neo4jClient;

    @Override
    public Optional<Word> findWord(String lemmaKey) {
        List<LexemeNode> nodes = lexemeRepo.findByIdStartingWith(lemmaKey + ":");
        if (nodes.isEmpty()) {
            return Optional.empty();
        }
        nodes.sort(Comparator.comparingInt(n -> n.getFreqRank() == null ? Integer.MAX_VALUE : n.getFreqRank()));
        List<LexemeVariant> variants = nodes.stream().map(this::toVariant).toList();
        String ipa = nodes.stream().map(LexemeNode::getIpa).filter(Objects::nonNull).findFirst().orElse(null);
        return Optional.of(new Word(lemmaKey, nodes.get(0).getLemma(), ipa, nodes.get(0).getFreqRank(), variants));
    }

    @Override
    public List<WordRef> searchWords(String query, int limit) {
        return neo4jClient.query(
                        "MATCH (l:Lexeme) WHERE l.lemma STARTS WITH $q "
                                + "WITH l.lemma AS lemma, min(l.freqRank) AS rank "
                                + "RETURN lemma ORDER BY rank ASC LIMIT $limit")
                .bind(query).to("q").bind(limit).to("limit")
                .fetchAs(WordRef.class)
                .mappedBy((ts, rec) -> {
                    String lemma = rec.get("lemma").asString();
                    return new WordRef("en:" + lemma, lemma, null);
                })
                .all().stream().toList();
    }

    @Override
    public List<Topic> topicRoots() {
        return topicRepo.findRoots().stream().map(this::toTopic).toList();
    }

    @Override
    public Optional<Topic> findTopic(String id) {
        return topicRepo.findById(id).map(this::toTopic);
    }

    @Override
    public List<WordSummary> wordsInTopic(String topicId) {
        return neo4jClient.query(
                        "MATCH (:Topic {id: $topicId})<-[:IN_TOPIC]-(l:Lexeme) " + WORD_AGG + " ORDER BY rank ASC")
                .bind(topicId).to("topicId")
                .fetchAs(WordSummary.class)
                .mappedBy((ts, rec) -> toSummary(rec))
                .all().stream().toList();
    }

    @Override
    public List<WordSummary> allWords(int offset, int limit) {
        return neo4jClient.query(
                        "MATCH (l:Lexeme) " + WORD_AGG + " ORDER BY rank ASC SKIP $offset LIMIT $limit")
                .bind(offset).to("offset").bind(limit).to("limit")
                .fetchAs(WordSummary.class)
                .mappedBy((ts, rec) -> toSummary(rec))
                .all().stream().toList();
    }

    @Override
    public long countWords() {
        return neo4jClient.query("MATCH (l:Lexeme) RETURN count(DISTINCT l.lemma) AS n")
                .fetchAs(Long.class).mappedBy((ts, rec) -> rec.get("n").asLong()).one().orElse(0L);
    }

    @Override
    public List<WordSummary> wordsInRank(int fromRank, int toRank, int offset, int limit) {
        return neo4jClient.query(
                        "MATCH (l:Lexeme) OPTIONAL MATCH (l)-[:IN_TOPIC]->(t:Topic) "
                                + "WITH l.lemma AS lemma, min(l.freqRank) AS rank, "
                                + "collect(DISTINCT l.pos) AS pos, min(l.cefr) AS cefr, collect(DISTINCT t) AS ts "
                                + "WHERE rank >= $from AND rank <= $to "
                                + "RETURN lemma, rank, pos, cefr, [x IN ts | {id:x.id, name:x.name, slug:x.slug}] AS topics "
                                + "ORDER BY rank ASC SKIP $offset LIMIT $limit")
                .bind(fromRank).to("from").bind(toRank).to("to")
                .bind(offset).to("offset").bind(limit).to("limit")
                .fetchAs(WordSummary.class)
                .mappedBy((ts, rec) -> toSummary(rec))
                .all().stream().toList();
    }

    @Override
    public long countWordsInRank(int fromRank, int toRank) {
        return neo4jClient.query(
                        "MATCH (l:Lexeme) WITH l.lemma AS lemma, min(l.freqRank) AS rank "
                                + "WHERE rank >= $from AND rank <= $to RETURN count(lemma) AS n")
                .bind(fromRank).to("from").bind(toRank).to("to")
                .fetchAs(Long.class).mappedBy((ts, rec) -> rec.get("n").asLong()).one().orElse(0L);
    }

    @Override
    public Map<String, Integer> ranksOf(Collection<String> lemmaKeys) {
        Map<String, Integer> out = new HashMap<>();
        if (lemmaKeys.isEmpty()) {
            return out;
        }
        List<String> lemmas = lemmaKeys.stream().map(LexiconRepositoryAdapter::lemmaOf).toList();
        neo4jClient.query(
                        "MATCH (l:Lexeme) WHERE l.lemma IN $lemmas "
                                + "WITH l.lemma AS lemma, min(l.freqRank) AS rank RETURN lemma, rank")
                .bind(lemmas).to("lemmas")
                .fetch().all()
                .forEach(row -> out.put("en:" + row.get("lemma"), ((Number) row.get("rank")).intValue()));
        return out;
    }

    @Override
    public List<WordSummary> wordsByKeys(Collection<String> lemmaKeys) {
        if (lemmaKeys.isEmpty()) {
            return List.of();
        }
        List<String> lemmas = lemmaKeys.stream().map(LexiconRepositoryAdapter::lemmaOf).toList();
        return neo4jClient.query("MATCH (l:Lexeme) WHERE l.lemma IN $lemmas " + WORD_AGG + " ORDER BY rank ASC")
                .bind(lemmas).to("lemmas")
                .fetchAs(WordSummary.class)
                .mappedBy((ts, rec) -> toSummary(rec))
                .all().stream().toList();
    }

    @Override
    public Optional<Grammar> findGrammar(String id) {
        return grammarRepo.findById(id).map(this::toGrammar);
    }

    @Override
    public List<Grammar> grammarTrunk() {
        // findAll(Sort) — стандартная загрузка SDN: гидрирует связь prerequisites у каждого узла.
        return grammarRepo.findAll(Sort.by("cefr", "name")).stream().map(this::toGrammar).toList();
    }

    @Override
    public List<WordRef> grammarIllustratedBy(String grammarId) {
        return lexemeRepo.illustrating(grammarId).stream().map(this::toWordRef).toList();
    }

    // --- маппинг узел → домен ---

    private LexemeVariant toVariant(LexemeNode n) {
        List<Form> forms = n.getForms() == null ? List.of()
                : n.getForms().stream().map(f -> new Form(f.getText(), f.getFeature())).toList();
        List<Translation> translations = n.getTranslations() == null ? List.of()
                : n.getTranslations().stream().map(t -> new Translation(t.getText(), t.getLang())).toList();
        return new LexemeVariant(
                PartOfSpeech.valueOf(n.getPos()),
                n.getCefr() == null ? null : Cefr.valueOf(n.getCefr()),
                n.getFreqRank(),
                forms,
                translations,
                relatedRefs(n.getId(), "SYNONYM"),
                relatedRefs(n.getId(), "ANTONYM"),
                relatedRefs(n.getId(), "HYPERNYM"));
    }

    private List<WordRef> relatedRefs(String nodeId, String type) {
        return lexemeRepo.relatedByType(nodeId, type).stream().map(this::toWordRef).toList();
    }

    /** Ссылка на слово: id → ключ леммы (без части речи), pos — конкретная часть речи узла. */
    private WordRef toWordRef(LexemeNode n) {
        return new WordRef(lemmaKey(n.getId()), n.getLemma(), PartOfSpeech.valueOf(n.getPos()));
    }

    /** Сводка слова из строки агрегата (lemma/rank/pos[]/cefr/topics[]). */
    private WordSummary toSummary(org.neo4j.driver.Record rec) {
        String lemma = rec.get("lemma").asString();
        List<PartOfSpeech> pos = rec.get("pos").asList(Value::asString).stream()
                .map(PartOfSpeech::valueOf).toList();
        Cefr cefr = rec.get("cefr").isNull() ? null : Cefr.valueOf(rec.get("cefr").asString());
        Integer rank = rec.get("rank").isNull() ? null : rec.get("rank").asInt();
        List<Topic> topics = rec.get("topics").asList(
                t -> new Topic(str(t.get("id")), str(t.get("name")), str(t.get("slug"))));
        return new WordSummary("en:" + lemma, lemma, pos, cefr, rank, topics);
    }

    private Topic toTopic(TopicNode n) {
        return new Topic(n.getId(), n.getName(), n.getSlug());
    }

    private Grammar toGrammar(GrammarNode n) {
        List<String> prerequisites = n.getPrerequisites() == null ? List.of()
                : n.getPrerequisites().stream().map(GrammarNode::getId).toList();
        return new Grammar(n.getId(), n.getName(),
                n.getCefr() == null ? null : Cefr.valueOf(n.getCefr()), prerequisites);
    }

    private static String str(Value v) {
        return v.isNull() ? null : v.asString();
    }

    /** "en:go:VERB" → "en:go" (ключ леммы = всё до последнего ':'). */
    private static String lemmaKey(String nodeId) {
        int i = nodeId.lastIndexOf(':');
        return i < 0 ? nodeId : nodeId.substring(0, i);
    }

    /** "en:go" → "go" (лемма без языкового префикса). */
    private static String lemmaOf(String lemmaKey) {
        int i = lemmaKey.indexOf(':');
        return i < 0 ? lemmaKey : lemmaKey.substring(i + 1);
    }
}
