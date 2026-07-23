package ru.eunoia.application.learning.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;
import ru.eunoia.application.knowledge.domain.model.RelationType;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.port.out.LexiconRepositoryPort;
import ru.eunoia.application.learning.domain.model.GardenLeaf;
import ru.eunoia.application.learning.domain.model.LexemeCard;
import ru.eunoia.application.learning.domain.model.TopicView;
import ru.eunoia.application.learning.port.in.LearningQueryUseCase;

/**
 * Фасад над двумя контекстами: структуру берём из канона (Neo4j, LexiconRepositoryPort), статус —
 * из оверлея (Postgres, MasteryRepositoryPort), склеиваем по id слова. Данные графов остаются
 * раздельными — смешивается только вид (принцип двух графов).
 */
public class LearningQueryUseCaseImpl implements LearningQueryUseCase {

    private final LexiconRepositoryPort lexicon;
    private final MasteryRepositoryPort mastery;

    public LearningQueryUseCaseImpl(LexiconRepositoryPort lexicon, MasteryRepositoryPort mastery) {
        this.lexicon = lexicon;
        this.mastery = mastery;
    }

    @Override
    public Optional<LexemeCard> lexemeCard(UUID userId, String lexemeId) {
        return lexicon.findById(lexemeId).map(lexeme -> new LexemeCard(
                lexeme,
                lexicon.related(lexemeId, RelationType.SYNONYM),
                lexicon.related(lexemeId, RelationType.ANTONYM),
                lexicon.related(lexemeId, RelationType.HYPERNYM),
                statusOf(userId, lexemeId)));
    }

    @Override
    public List<LexemeRef> search(String query, int limit) {
        return lexicon.search(query, limit);
    }

    @Override
    public List<Topic> topicRoots() {
        return lexicon.topicRoots();
    }

    @Override
    public Optional<TopicView> topicView(UUID userId, String topicId) {
        return lexicon.findTopic(topicId).map(topic -> {
            List<LexemeRef> lexemes = lexicon.lexemesInTopic(topicId);
            Map<String, MasteryStatus> statuses = mastery.statusesFor(userId,
                    lexemes.stream().map(LexemeRef::id).toList());
            List<GardenLeaf> leaves = lexemes.stream()
                    .map(ref -> new GardenLeaf(ref, statuses.getOrDefault(ref.id(), MasteryStatus.UNKNOWN)))
                    .toList();
            return new TopicView(topic, leaves);
        });
    }

    @Override
    public Optional<Grammar> grammar(String grammarId) {
        return lexicon.findGrammar(grammarId);
    }

    /** Мой статус по слову; нет отметки → UNKNOWN. */
    private MasteryStatus statusOf(UUID userId, String lexemeId) {
        return mastery.find(userId, lexemeId).map(m -> m.status()).orElse(MasteryStatus.UNKNOWN);
    }
}
