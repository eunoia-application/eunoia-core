package ru.eunoia.application.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;
import ru.eunoia.application.knowledge.domain.model.Cefr;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.Lexeme;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.RelationType;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.port.out.LexiconRepositoryPort;
import ru.eunoia.application.learning.domain.model.LexemeCard;
import ru.eunoia.application.learning.domain.model.TopicView;

/**
 * Фасад учёбы склеивает два графа по id слова: структуру берёт из канона (LexiconRepositoryPort),
 * статус — из оверлея (MasteryRepositoryPort). Проверяем сборку связей и раскраску по мастерству,
 * дефолт UNKNOWN при отсутствии отметки и пустой результат, когда узла графа нет. Порты замоканы.
 */
@ExtendWith(MockitoExtension.class)
class LearningQueryUseCaseImplTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String GO = "en:go:VERB";

    @Mock
    private LexiconRepositoryPort lexicon;
    @Mock
    private MasteryRepositoryPort mastery;

    private LearningQueryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new LearningQueryUseCaseImpl(lexicon, mastery);
    }

    private static Lexeme go() {
        return new Lexeme(GO, "go", PartOfSpeech.VERB, "en", Cefr.A1, 42, List.of(), List.of());
    }

    @Test
    void lexemeCard_found_assemblesRelationsByType_andStatusFromMastery() {
        LexemeRef syn = new LexemeRef("en:walk:VERB", "walk", PartOfSpeech.VERB);
        LexemeRef ant = new LexemeRef("en:stop:VERB", "stop", PartOfSpeech.VERB);
        LexemeRef hyp = new LexemeRef("en:move:VERB", "move", PartOfSpeech.VERB);
        when(lexicon.findById(GO)).thenReturn(Optional.of(go()));
        when(lexicon.related(GO, RelationType.SYNONYM)).thenReturn(List.of(syn));
        when(lexicon.related(GO, RelationType.ANTONYM)).thenReturn(List.of(ant));
        when(lexicon.related(GO, RelationType.HYPERNYM)).thenReturn(List.of(hyp));
        when(mastery.find(USER, GO)).thenReturn(
                Optional.of(new Mastery(USER, GO, MasteryStatus.LEARNING, LocalDateTime.now())));

        Optional<LexemeCard> result = useCase.lexemeCard(USER, GO);

        assertThat(result).isPresent();
        LexemeCard card = result.get();
        assertThat(card.lexeme().lemma()).isEqualTo("go");
        assertThat(card.synonyms()).containsExactly(syn);
        assertThat(card.antonyms()).containsExactly(ant);
        assertThat(card.hypernyms()).containsExactly(hyp);
        assertThat(card.status()).isEqualTo(MasteryStatus.LEARNING);
    }

    @Test
    void lexemeCard_noMasteryMark_statusUnknown() {
        when(lexicon.findById(GO)).thenReturn(Optional.of(go()));
        when(lexicon.related(GO, RelationType.SYNONYM)).thenReturn(List.of());
        when(lexicon.related(GO, RelationType.ANTONYM)).thenReturn(List.of());
        when(lexicon.related(GO, RelationType.HYPERNYM)).thenReturn(List.of());
        when(mastery.find(USER, GO)).thenReturn(Optional.empty());

        Optional<LexemeCard> result = useCase.lexemeCard(USER, GO);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(MasteryStatus.UNKNOWN);
    }

    @Test
    void lexemeCard_notFound_empty_andMasteryUntouched() {
        when(lexicon.findById("missing")).thenReturn(Optional.empty());

        assertThat(useCase.lexemeCard(USER, "missing")).isEmpty();
        verifyNoInteractions(mastery);
    }

    @Test
    void topicView_found_colorsLeavesByMastery_missingMarkDefaultsUnknown() {
        Topic topic = new Topic("topic:travel", "Travel", "travel");
        LexemeRef go = new LexemeRef(GO, "go", PartOfSpeech.VERB);
        LexemeRef run = new LexemeRef("en:run:VERB", "run", PartOfSpeech.VERB);
        when(lexicon.findTopic("topic:travel")).thenReturn(Optional.of(topic));
        when(lexicon.lexemesInTopic("topic:travel")).thenReturn(List.of(go, run));
        when(mastery.statusesFor(USER, List.of(GO, "en:run:VERB")))
                .thenReturn(Map.of(GO, MasteryStatus.KNOWN));

        Optional<TopicView> result = useCase.topicView(USER, "topic:travel");

        assertThat(result).isPresent();
        TopicView view = result.get();
        assertThat(view.topic()).isEqualTo(topic);
        assertThat(view.leaves()).hasSize(2);
        assertThat(view.leaves().get(0).lexeme()).isEqualTo(go);
        assertThat(view.leaves().get(0).status()).isEqualTo(MasteryStatus.KNOWN);
        // run без отметки → UNKNOWN через getOrDefault
        assertThat(view.leaves().get(1).lexeme()).isEqualTo(run);
        assertThat(view.leaves().get(1).status()).isEqualTo(MasteryStatus.UNKNOWN);
    }

    @Test
    void topicView_topicNotFound_empty_andMasteryUntouched() {
        when(lexicon.findTopic("missing")).thenReturn(Optional.empty());

        assertThat(useCase.topicView(USER, "missing")).isEmpty();
        verifyNoInteractions(mastery);
    }

    @Test
    void search_delegatesToLexicon() {
        List<LexemeRef> hits = List.of(new LexemeRef(GO, "go", PartOfSpeech.VERB));
        when(lexicon.search("go", 10)).thenReturn(hits);

        assertThat(useCase.search("go", 10)).isSameAs(hits);
    }

    @Test
    void topicRoots_delegatesToLexicon() {
        List<Topic> roots = List.of(new Topic("topic:travel", "Travel", "travel"));
        when(lexicon.topicRoots()).thenReturn(roots);

        assertThat(useCase.topicRoots()).isSameAs(roots);
    }

    @Test
    void grammar_delegatesToLexicon() {
        Optional<Grammar> grammar = Optional.of(new Grammar("gr:present-simple", "Present Simple", Cefr.A1));
        when(lexicon.findGrammar("gr:present-simple")).thenReturn(grammar);

        assertThat(useCase.grammar("gr:present-simple")).isSameAs(grammar);
    }
}
