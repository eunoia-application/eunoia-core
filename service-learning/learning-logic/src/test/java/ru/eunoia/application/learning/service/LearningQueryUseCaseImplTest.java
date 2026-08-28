package ru.eunoia.application.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.garden.domain.model.ActivityStats;
import ru.eunoia.application.garden.domain.model.GrammarMastery;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.in.ActivityUseCase;
import ru.eunoia.application.garden.port.in.GrammarMasteryUseCase;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;
import ru.eunoia.application.knowledge.domain.model.Cefr;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.LexemeVariant;
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Word;
import ru.eunoia.application.knowledge.domain.model.WordRef;
import ru.eunoia.application.knowledge.domain.model.WordSummary;
import ru.eunoia.application.knowledge.port.out.LexiconRepositoryPort;
import ru.eunoia.application.learning.domain.model.Band;
import ru.eunoia.application.learning.domain.model.TopicView;
import ru.eunoia.application.learning.domain.model.TreeGrammar;
import ru.eunoia.application.learning.domain.model.TreeSnapshot;
import ru.eunoia.application.learning.domain.model.TreeTopic;
import ru.eunoia.application.learning.domain.model.WordCard;
import ru.eunoia.application.learning.domain.model.WordLeaf;
import ru.eunoia.application.learning.domain.model.WordPage;

/**
 * Фасад учёбы склеивает два графа по КЛЮЧУ ЛЕММЫ (en:go). Проверяем карточку-слово, раскраску
 * листьев, пагинацию, блоки топ-слов с прогрессом и список «Учить». Порты замоканы.
 */
@ExtendWith(MockitoExtension.class)
class LearningQueryUseCaseImplTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String GO = "en:go";

    @Mock
    private LexiconRepositoryPort lexicon;
    @Mock
    private MasteryRepositoryPort mastery;
    @Mock
    private ActivityUseCase activity;
    @Mock
    private GrammarMasteryUseCase grammarMastery;

    private LearningQueryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new LearningQueryUseCaseImpl(lexicon, mastery, activity, grammarMastery);
    }

    private static Word go() {
        var verb = new LexemeVariant(PartOfSpeech.VERB, Cefr.A1, 42,
                List.of(), List.of(), List.of(), List.of(), List.of());
        return new Word(GO, "go", "/ɡoʊ/", 42, List.of(verb));
    }

    @Test
    void treeSnapshot_joinsVocabularyTopicsAndActivity() {
        Topic animals = new Topic("animals", "Животные", "animals");
        Topic travel = new Topic("travel", "Город и транспорт", "travel");
        Topic food = new Topic("food", "Еда и напитки", "food");   // тема без моих отметок
        var marks = List.of(
                new Mastery(USER, GO, MasteryStatus.KNOWN, LocalDateTime.now()),
                new Mastery(USER, "en:run", MasteryStatus.LEARNING, LocalDateTime.now()));
        when(mastery.findByUser(USER)).thenReturn(marks);
        when(lexicon.countWords()).thenReturn(5863L);
        when(lexicon.wordsByKeys(Set.of(GO, "en:run"))).thenReturn(List.of(
                new WordSummary(GO, "go", List.of(PartOfSpeech.VERB), Cefr.A1, 1, List.of(animals)),
                new WordSummary("en:run", "run", List.of(PartOfSpeech.VERB), Cefr.A1, 200, List.of(travel))));
        when(lexicon.topicWordCounts()).thenReturn(Map.of("animals", 140L, "travel", 80L, "food", 200L));
        when(lexicon.topicRoots()).thenReturn(List.of(animals, travel, food));
        var stats = new ActivityStats(4, LocalDate.of(2026, 7, 25), 11);
        when(activity.summary(USER)).thenReturn(stats);
        when(grammarMastery.findByUser(USER)).thenReturn(List.of(
                new GrammarMastery(USER, "past-simple", MasteryStatus.KNOWN, LocalDateTime.now()),
                new GrammarMastery(USER, "present-simple", MasteryStatus.LEARNING, LocalDateTime.now())));
        when(lexicon.countGrammar()).thenReturn(27L);

        TreeSnapshot snap = useCase.treeSnapshot(USER);

        assertThat(snap.vocabulary().known()).isEqualTo(1);
        assertThat(snap.vocabulary().learning()).isEqualTo(1);
        assertThat(snap.vocabulary().total()).isEqualTo(5863);
        assertThat(snap.topics())
                .extracting(TreeTopic::id, TreeTopic::known, TreeTopic::learning, TreeTopic::total)
                .containsExactly(
                        tuple("animals", 1, 0, 140),
                        tuple("travel", 0, 1, 80),
                        tuple("food", 0, 0, 200));
        assertThat(snap.activity()).isSameAs(stats);
        assertThat(snap.grammar()).isEqualTo(new TreeGrammar(1, 1, 27));
    }

    @Test
    void treeSnapshot_noMarks_zeroProgress() {
        when(mastery.findByUser(USER)).thenReturn(List.of());
        when(lexicon.countWords()).thenReturn(5863L);
        when(lexicon.wordsByKeys(Set.of())).thenReturn(List.of());
        when(lexicon.topicWordCounts()).thenReturn(Map.of("animals", 140L));
        when(lexicon.topicRoots()).thenReturn(List.of(new Topic("animals", "Животные", "animals")));
        when(activity.summary(USER)).thenReturn(ActivityStats.empty());
        when(grammarMastery.findByUser(USER)).thenReturn(List.of());
        when(lexicon.countGrammar()).thenReturn(27L);

        TreeSnapshot snap = useCase.treeSnapshot(USER);

        assertThat(snap.vocabulary().known()).isZero();
        assertThat(snap.vocabulary().learning()).isZero();
        assertThat(snap.topics()).singleElement()
                .extracting(TreeTopic::known, TreeTopic::total).isEqualTo(List.of(0, 140));
        assertThat(snap.activity().streak()).isZero();
        assertThat(snap.grammar()).isEqualTo(new TreeGrammar(0, 0, 27));
    }

    @Test
    void wordCard_found_assemblesWord_andStatusByLemma() {
        when(lexicon.findWord(GO)).thenReturn(Optional.of(go()));
        when(mastery.find(USER, GO)).thenReturn(
                Optional.of(new Mastery(USER, GO, MasteryStatus.LEARNING, LocalDateTime.now())));

        Optional<WordCard> result = useCase.wordCard(USER, GO);

        assertThat(result).isPresent();
        assertThat(result.get().word().lemma()).isEqualTo("go");
        assertThat(result.get().status()).isEqualTo(MasteryStatus.LEARNING);
    }

    @Test
    void wordCard_noMark_statusUnknown() {
        when(lexicon.findWord(GO)).thenReturn(Optional.of(go()));
        when(mastery.find(USER, GO)).thenReturn(Optional.empty());

        assertThat(useCase.wordCard(USER, GO).orElseThrow().status()).isEqualTo(MasteryStatus.UNKNOWN);
    }

    @Test
    void wordCard_notFound_empty_andMasteryUntouched() {
        when(lexicon.findWord("missing")).thenReturn(Optional.empty());

        assertThat(useCase.wordCard(USER, "missing")).isEmpty();
        verifyNoInteractions(mastery);
    }

    @Test
    void topicView_found_colorsLeaves_carriesTopics() {
        Topic topic = new Topic("topic:travel", "Travel", "travel");
        var go = new WordSummary(GO, "go", List.of(PartOfSpeech.VERB), Cefr.A1, 42, List.of(topic));
        var run = new WordSummary("en:run", "run", List.of(PartOfSpeech.VERB), Cefr.A1, 50, List.of());
        when(lexicon.findTopic("topic:travel")).thenReturn(Optional.of(topic));
        when(lexicon.wordsInTopic("topic:travel")).thenReturn(List.of(go, run));
        when(mastery.statusesFor(USER, List.of(GO, "en:run")))
                .thenReturn(Map.of(GO, MasteryStatus.KNOWN));

        TopicView view = useCase.topicView(USER, "topic:travel").orElseThrow();

        assertThat(view.words()).hasSize(2);
        assertThat(view.words().get(0).id()).isEqualTo(GO);
        assertThat(view.words().get(0).topics()).containsExactly(topic);
        assertThat(view.words().get(0).status()).isEqualTo(MasteryStatus.KNOWN);
        assertThat(view.words().get(1).status()).isEqualTo(MasteryStatus.UNKNOWN);
    }

    @Test
    void topicView_topicNotFound_empty_andMasteryUntouched() {
        when(lexicon.findTopic("missing")).thenReturn(Optional.empty());

        assertThat(useCase.topicView(USER, "missing")).isEmpty();
        verifyNoInteractions(mastery);
    }

    @Test
    void listWords_noBand_pagesAllWords() {
        var go = new WordSummary(GO, "go", List.of(PartOfSpeech.VERB), Cefr.A1, 42, List.of());
        when(lexicon.allWords(0, 100)).thenReturn(List.of(go));
        when(lexicon.countWords()).thenReturn(2500L);
        when(mastery.statusesFor(USER, List.of(GO))).thenReturn(Map.of(GO, MasteryStatus.LEARNING));

        WordPage page = useCase.listWords(USER, null, 0, 100);

        assertThat(page.total()).isEqualTo(2500);
        assertThat(page.words()).hasSize(1);
        assertThat(page.words().get(0).status()).isEqualTo(MasteryStatus.LEARNING);
    }

    @Test
    void listWords_withBand_usesRankRange() {
        var go = new WordSummary(GO, "go", List.of(PartOfSpeech.VERB), Cefr.A1, 42, List.of());
        when(lexicon.wordsInRank(1, 100, 0, 100)).thenReturn(List.of(go));
        when(lexicon.countWordsInRank(1, 100)).thenReturn(100L);
        when(mastery.statusesFor(USER, List.of(GO))).thenReturn(Map.of());

        WordPage page = useCase.listWords(USER, "top-100", 0, 100);

        assertThat(page.total()).isEqualTo(100);
        assertThat(page.words()).hasSize(1);
        assertThat(page.words().get(0).status()).isEqualTo(MasteryStatus.UNKNOWN);
    }

    @Test
    void listWords_unknownBand_emptyPage() {
        WordPage page = useCase.listWords(USER, "nope", 5, 10);

        assertThat(page.total()).isZero();
        assertThat(page.offset()).isEqualTo(5);
        assertThat(page.words()).isEmpty();
    }

    @Test
    void bands_computesTotals_andMyProgressPerBlock() {
        var marks = List.of(
                new Mastery(USER, GO, MasteryStatus.KNOWN, LocalDateTime.now()),
                new Mastery(USER, "en:run", MasteryStatus.LEARNING, LocalDateTime.now()),
                new Mastery(USER, "en:ghost", MasteryStatus.LEARNING, LocalDateTime.now()));
        when(mastery.findByUser(USER)).thenReturn(marks);
        // ghost нет в ranksOf → ранг null → в прогресс блоков не попадает (ветка continue)
        when(lexicon.ranksOf(List.of(GO, "en:run", "en:ghost"))).thenReturn(Map.of(GO, 1, "en:run", 200));
        when(lexicon.countWordsInRank(1, 100)).thenReturn(100L);
        when(lexicon.countWordsInRank(101, 500)).thenReturn(400L);
        when(lexicon.countWordsInRank(501, 1000)).thenReturn(500L);
        when(lexicon.countWordsInRank(1001, 3000)).thenReturn(2000L);
        when(lexicon.countWordsInRank(3001, 5000)).thenReturn(2000L);
        when(lexicon.countWordsInRank(5001, 10000)).thenReturn(5000L);

        List<Band> bands = useCase.bands(USER);

        assertThat(bands).hasSize(6);
        assertThat(bands.get(0).id()).isEqualTo("top-100");
        assertThat(bands.get(0).total()).isEqualTo(100);
        assertThat(bands.get(0).known()).isEqualTo(1);       // go(1) знаю
        assertThat(bands.get(0).learning()).isZero();
        assertThat(bands.get(1).learning()).isEqualTo(1);    // run(200) учу — в 101–500
        assertThat(bands.get(1).known()).isZero();
    }

    @Test
    void study_returnsMyLearningWords() {
        var marks = List.of(
                new Mastery(USER, GO, MasteryStatus.KNOWN, LocalDateTime.now()),
                new Mastery(USER, "en:run", MasteryStatus.LEARNING, LocalDateTime.now()));
        when(mastery.findByUser(USER)).thenReturn(marks);
        var run = new WordSummary("en:run", "run", List.of(PartOfSpeech.VERB), Cefr.A1, 200, List.of());
        when(lexicon.wordsByKeys(List.of("en:run"))).thenReturn(List.of(run));
        when(mastery.statusesFor(USER, List.of("en:run"))).thenReturn(Map.of("en:run", MasteryStatus.LEARNING));

        List<WordLeaf> study = useCase.study(USER);

        assertThat(study).hasSize(1);
        assertThat(study.get(0).id()).isEqualTo("en:run");
        assertThat(study.get(0).status()).isEqualTo(MasteryStatus.LEARNING);
    }

    @Test
    void searchWords_delegatesToLexicon() {
        List<WordRef> hits = List.of(new WordRef(GO, "go", null));
        when(lexicon.searchWords("go", 10)).thenReturn(hits);

        assertThat(useCase.searchWords("go", 10)).isSameAs(hits);
    }

    @Test
    void topicRoots_delegatesToLexicon() {
        List<Topic> roots = List.of(new Topic("topic:travel", "Travel", "travel"));
        when(lexicon.topicRoots()).thenReturn(roots);

        assertThat(useCase.topicRoots()).isSameAs(roots);
    }

    @Test
    void grammar_wrapsRuleWithStatusAndIllustratingWords() {
        var rule = new Grammar("gr:past-simple", "Past Simple", Cefr.A2, List.of("gr:present-simple"));
        var words = List.of(new WordRef(GO, "go", PartOfSpeech.VERB));
        when(lexicon.findGrammar("gr:past-simple")).thenReturn(Optional.of(rule));
        when(lexicon.grammarIllustratedBy("gr:past-simple")).thenReturn(words);
        when(grammarMastery.statuses(USER, List.of("gr:past-simple")))
                .thenReturn(Map.of("gr:past-simple", MasteryStatus.LEARNING));

        var view = useCase.grammar(USER, "gr:past-simple");

        assertThat(view).isPresent();
        assertThat(view.get().status()).isEqualTo(MasteryStatus.LEARNING);
        assertThat(view.get().illustratedBy()).isSameAs(words);
    }

    @Test
    void grammar_noMark_statusUnknown() {
        var rule = new Grammar("gr:x", "X", Cefr.A1, List.of());
        when(lexicon.findGrammar("gr:x")).thenReturn(Optional.of(rule));
        when(lexicon.grammarIllustratedBy("gr:x")).thenReturn(List.of());
        when(grammarMastery.statuses(USER, List.of("gr:x"))).thenReturn(Map.of());

        assertThat(useCase.grammar(USER, "gr:x").orElseThrow().status()).isEqualTo(MasteryStatus.UNKNOWN);
    }

    @Test
    void grammarTrunk_overlaysStatus_emptyIllustratedBy() {
        var rules = List.of(
                new Grammar("gr:present-simple", "Present Simple", Cefr.A1, List.of()),
                new Grammar("gr:past-simple", "Past Simple", Cefr.A2, List.of()));
        when(lexicon.grammarTrunk()).thenReturn(rules);
        when(grammarMastery.statuses(USER, List.of("gr:present-simple", "gr:past-simple")))
                .thenReturn(Map.of("gr:present-simple", MasteryStatus.KNOWN));

        var trunk = useCase.grammarTrunk(USER);

        assertThat(trunk).extracting(v -> v.grammar().id(), v -> v.status())
                .containsExactly(
                        tuple("gr:present-simple", MasteryStatus.KNOWN),
                        tuple("gr:past-simple", MasteryStatus.UNKNOWN));
        assertThat(trunk.get(0).illustratedBy()).isEmpty();
    }
}
