package ru.eunoia.persistence.knowledge;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.eunoia.application.knowledge.domain.model.Cefr;
import ru.eunoia.application.knowledge.domain.model.Form;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.Lexeme;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.RelationType;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Translation;
import ru.eunoia.application.knowledge.port.out.LexiconRepositoryPort;
import ru.eunoia.persistence.knowledge.entity.GrammarNode;
import ru.eunoia.persistence.knowledge.entity.LexemeNode;
import ru.eunoia.persistence.knowledge.entity.TopicNode;
import ru.eunoia.persistence.knowledge.repository.GrammarNeo4jRepository;
import ru.eunoia.persistence.knowledge.repository.LexemeNeo4jRepository;
import ru.eunoia.persistence.knowledge.repository.TopicNeo4jRepository;

/**
 * Neo4j-адаптер чтения канонического графа. Маппинг узел → домен вручную (как в JPA-адаптере):
 * строковые pos/cefr узлов собираем в enum'ы домена, полное слово отдаём только в findById,
 * списки — лёгкими {@link LexemeRef}, чтобы не тянуть весь граф.
 */
@Component
@RequiredArgsConstructor
public class LexiconRepositoryAdapter implements LexiconRepositoryPort {

    private final LexemeNeo4jRepository lexemeRepo;
    private final TopicNeo4jRepository topicRepo;
    private final GrammarNeo4jRepository grammarRepo;

    @Override
    public Optional<Lexeme> findById(String id) {
        return lexemeRepo.findById(id).map(this::toLexeme);
    }

    @Override
    public List<LexemeRef> search(String query, int limit) {
        return lexemeRepo.searchByLemmaPrefix(query, limit).stream().map(this::toRef).toList();
    }

    @Override
    public List<LexemeRef> related(String lexemeId, RelationType type) {
        return lexemeRepo.relatedByType(lexemeId, type.name()).stream().map(this::toRef).toList();
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
    public List<LexemeRef> lexemesInTopic(String topicId) {
        return lexemeRepo.inTopic(topicId).stream().map(this::toRef).toList();
    }

    @Override
    public Optional<Grammar> findGrammar(String id) {
        return grammarRepo.findById(id).map(this::toGrammar);
    }

    // --- маппинг узел → домен ---

    /** Лёгкая ссылка: только id/лемма/часть речи (для списков). */
    private LexemeRef toRef(LexemeNode n) {
        return new LexemeRef(n.getId(), n.getLemma(), PartOfSpeech.valueOf(n.getPos()));
    }

    /** Полная карточка слова: атрибуты + формы + переводы. cefr — null-safe. */
    private Lexeme toLexeme(LexemeNode n) {
        List<Form> forms = n.getForms() == null ? List.of()
                : n.getForms().stream().map(f -> new Form(f.getText(), f.getFeature())).toList();
        List<Translation> translations = n.getTranslations() == null ? List.of()
                : n.getTranslations().stream().map(t -> new Translation(t.getText(), t.getLang())).toList();
        return new Lexeme(
                n.getId(),
                n.getLemma(),
                PartOfSpeech.valueOf(n.getPos()),
                n.getLang(),
                n.getCefr() == null ? null : Cefr.valueOf(n.getCefr()),
                n.getFreqRank(),
                forms,
                translations);
    }

    private Topic toTopic(TopicNode n) {
        return new Topic(n.getId(), n.getName(), n.getSlug());
    }

    private Grammar toGrammar(GrammarNode n) {
        return new Grammar(n.getId(), n.getName(), n.getCefr() == null ? null : Cefr.valueOf(n.getCefr()));
    }
}
