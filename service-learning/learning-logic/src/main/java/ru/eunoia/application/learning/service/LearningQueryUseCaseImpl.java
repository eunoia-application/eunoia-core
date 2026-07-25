package ru.eunoia.application.learning.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.in.ActivityUseCase;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.WordRef;
import ru.eunoia.application.knowledge.domain.model.WordSummary;
import ru.eunoia.application.knowledge.port.out.LexiconRepositoryPort;
import ru.eunoia.application.learning.domain.model.Band;
import ru.eunoia.application.learning.domain.model.GrammarView;
import ru.eunoia.application.learning.domain.model.TopicView;
import ru.eunoia.application.learning.domain.model.TreeSnapshot;
import ru.eunoia.application.learning.domain.model.TreeTopic;
import ru.eunoia.application.learning.domain.model.TreeVocabulary;
import ru.eunoia.application.learning.domain.model.WordCard;
import ru.eunoia.application.learning.domain.model.WordLeaf;
import ru.eunoia.application.learning.domain.model.WordPage;
import ru.eunoia.application.learning.port.in.LearningQueryUseCase;

/**
 * Фасад над двумя контекстами: структуру берём из канона (Neo4j), статус — из оверлея (Postgres),
 * склеиваем по КЛЮЧУ ЛЕММЫ (en:go). Единица обучения — слово. Блоки топ-слов (уровни по частоте) —
 * первичная навигация, категория слова — вторичная разбивка внутри блока. Мастерство — по слову.
 */
public class LearningQueryUseCaseImpl implements LearningQueryUseCase {

    /** Эксклюзивные блоки-уровни по рангу частоты (от простого к сложному). */
    private record BandDef(String id, String label, int from, int to) {
    }

    private static final List<BandDef> BANDS = List.of(
            new BandDef("top-100", "Топ-100", 1, 100),
            new BandDef("top-500", "101–500", 101, 500),
            new BandDef("top-1000", "501–1000", 501, 1000),
            new BandDef("top-3000", "1001–3000", 1001, 3000),
            new BandDef("top-5000", "3001–5000", 3001, 5000),
            new BandDef("top-10000", "5001–10000", 5001, 10000));

    private final LexiconRepositoryPort lexicon;
    private final MasteryRepositoryPort mastery;
    private final ActivityUseCase activity;

    public LearningQueryUseCaseImpl(LexiconRepositoryPort lexicon, MasteryRepositoryPort mastery,
                                    ActivityUseCase activity) {
        this.lexicon = lexicon;
        this.mastery = mastery;
        this.activity = activity;
    }

    @Override
    public TreeSnapshot treeSnapshot(UUID userId) {
        List<Mastery> marks = mastery.findByUser(userId);

        // листья: суммарный словарный прогресс + карта статусов по id (один проход)
        int known = 0;
        int learning = 0;
        Map<String, MasteryStatus> statusById = new HashMap<>();
        for (Mastery m : marks) {
            statusById.put(m.lexemeId(), m.status());
            if (m.status() == MasteryStatus.KNOWN) {
                known++;
            } else if (m.status() == MasteryStatus.LEARNING) {
                learning++;
            }
        }
        TreeVocabulary vocabulary = new TreeVocabulary(known, learning, (int) lexicon.countWords());

        // ветки: total по темам (из канона) × мой прогресс по каждой теме (слово даёт вклад
        // в КАЖДУЮ свою тему).
        Map<String, Long> totals = lexicon.topicWordCounts();
        Map<String, int[]> mine = new HashMap<>();   // topicId → [known, learning]
        for (WordSummary w : lexicon.wordsByKeys(statusById.keySet())) {
            MasteryStatus st = statusById.getOrDefault(w.id(), MasteryStatus.UNKNOWN);
            for (Topic t : w.topics()) {
                int[] kl = mine.computeIfAbsent(t.id(), k -> new int[2]);
                if (st == MasteryStatus.KNOWN) {
                    kl[0]++;
                } else if (st == MasteryStatus.LEARNING) {
                    kl[1]++;
                }
            }
        }
        List<TreeTopic> topics = lexicon.topicRoots().stream().map(t -> {
            int[] kl = mine.getOrDefault(t.id(), new int[2]);
            return new TreeTopic(t.id(), t.name(), t.slug(), kl[0], kl[1],
                    totals.getOrDefault(t.id(), 0L).intValue());
        }).toList();

        return new TreeSnapshot(vocabulary, topics, activity.summary(userId));
    }

    @Override
    public Optional<WordCard> wordCard(UUID userId, String lemmaKey) {
        return lexicon.findWord(lemmaKey).map(word -> new WordCard(word, statusOf(userId, lemmaKey)));
    }

    @Override
    public List<WordRef> searchWords(String query, int limit) {
        return lexicon.searchWords(query, limit);
    }

    @Override
    public List<Topic> topicRoots() {
        return lexicon.topicRoots();
    }

    @Override
    public Optional<TopicView> topicView(UUID userId, String topicId) {
        return lexicon.findTopic(topicId)
                .map(topic -> new TopicView(topic, leaves(userId, lexicon.wordsInTopic(topicId))));
    }

    @Override
    public List<Band> bands(UUID userId) {
        List<Mastery> marks = mastery.findByUser(userId);
        Map<String, Integer> ranks = lexicon.ranksOf(marks.stream().map(Mastery::lexemeId).toList());
        return BANDS.stream().map(b -> {
            int known = 0;
            int learning = 0;
            for (Mastery m : marks) {
                Integer rank = ranks.get(m.lexemeId());
                if (rank == null || rank < b.from() || rank > b.to()) {
                    continue;
                }
                if (m.status() == MasteryStatus.KNOWN) {
                    known++;
                } else if (m.status() == MasteryStatus.LEARNING) {
                    learning++;
                }
            }
            return new Band(b.id(), b.label(), b.from(), b.to(),
                    (int) lexicon.countWordsInRank(b.from(), b.to()), known, learning);
        }).toList();
    }

    @Override
    public WordPage listWords(UUID userId, String band, int offset, int limit) {
        if (band == null || band.isBlank()) {
            return new WordPage((int) lexicon.countWords(), offset, limit,
                    leaves(userId, lexicon.allWords(offset, limit)));
        }
        return BANDS.stream().filter(b -> b.id().equals(band)).findFirst()
                .map(b -> new WordPage((int) lexicon.countWordsInRank(b.from(), b.to()), offset, limit,
                        leaves(userId, lexicon.wordsInRank(b.from(), b.to(), offset, limit))))
                .orElseGet(() -> new WordPage(0, offset, limit, List.of()));
    }

    @Override
    public List<WordLeaf> study(UUID userId) {
        List<String> learning = mastery.findByUser(userId).stream()
                .filter(m -> m.status() == MasteryStatus.LEARNING)
                .map(Mastery::lexemeId)
                .toList();
        return leaves(userId, lexicon.wordsByKeys(learning));
    }

    @Override
    public Optional<GrammarView> grammar(String grammarId) {
        return lexicon.findGrammar(grammarId)
                .map(g -> new GrammarView(g, lexicon.grammarIllustratedBy(grammarId)));
    }

    @Override
    public List<GrammarView> grammarTrunk() {
        return lexicon.grammarTrunk().stream().map(g -> new GrammarView(g, List.of())).toList();
    }

    /** Сводки слов → листья с темами и моим статусом (по ключу леммы; нет отметки → UNKNOWN). */
    private List<WordLeaf> leaves(UUID userId, List<WordSummary> words) {
        Map<String, MasteryStatus> statuses = mastery.statusesFor(userId,
                words.stream().map(WordSummary::id).toList());
        return words.stream()
                .map(w -> new WordLeaf(w.id(), w.lemma(), w.pos(), w.cefr(), w.topics(),
                        statuses.getOrDefault(w.id(), MasteryStatus.UNKNOWN)))
                .toList();
    }

    /** Мой статус по слову (лемме); нет отметки → UNKNOWN. */
    private MasteryStatus statusOf(UUID userId, String lemmaKey) {
        return mastery.find(userId, lemmaKey).map(Mastery::status).orElse(MasteryStatus.UNKNOWN);
    }
}
