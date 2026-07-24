package ru.eunoia.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.knowledge.domain.model.Cefr;
import ru.eunoia.application.knowledge.domain.model.Form;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.LexemeVariant;
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Translation;
import ru.eunoia.application.knowledge.domain.model.Word;
import ru.eunoia.application.knowledge.domain.model.WordRef;
import ru.eunoia.application.learning.domain.model.TopicView;
import ru.eunoia.application.learning.domain.model.WordCard;
import ru.eunoia.application.learning.domain.model.WordLeaf;
import ru.eunoia.application.learning.domain.model.WordPage;

/**
 * Чистое преобразование домен → DTO контракта. Без моков — конструируем реальные доменные объекты
 * и сверяем каждое поле DTO. Единица — СЛОВО (лемма): карточка с variants. Enum'ы едут по имени;
 * отдельно проверяем nullable (pos/cefr/ipa) и wordId у отметки мастерства.
 */
class LearningApiMapperTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 6, 7, 8, 9, 10);

    private final LearningApiMapper mapper = new LearningApiMapper();

    @Test
    void toWordCard_mapsWord_variants_relations_enums_status() {
        var variant = new LexemeVariant(PartOfSpeech.VERB, Cefr.A1, 42,
                List.of(new Form("went", "past")),
                List.of(new Translation("идти", "ru")),
                List.of(new WordRef("en:walk", "walk", PartOfSpeech.VERB)),
                List.of(new WordRef("en:stop", "stop", PartOfSpeech.VERB)),
                List.of(new WordRef("en:move", "move", PartOfSpeech.VERB)));
        var word = new Word("en:go", "go", "/ɡoʊ/", 42, List.of(variant));

        var dto = mapper.toWordCard(new WordCard(word, MasteryStatus.LEARNING));

        assertThat(dto.getId()).isEqualTo("en:go");
        assertThat(dto.getLemma()).isEqualTo("go");
        assertThat(dto.getIpa()).isEqualTo("/ɡoʊ/");
        assertThat(dto.getFreqRank()).isEqualTo(42);
        assertThat(dto.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.LEARNING);
        assertThat(dto.getVariants()).hasSize(1);
        var v = dto.getVariants().get(0);
        assertThat(v.getPos()).isEqualTo(com.eunoia.application.learning.model.PartOfSpeech.VERB);
        assertThat(v.getCefr()).isEqualTo(com.eunoia.application.learning.model.Cefr.A1);
        assertThat(v.getFreqRank()).isEqualTo(42);
        assertThat(v.getForms().get(0).getText()).isEqualTo("went");
        assertThat(v.getForms().get(0).getFeature()).isEqualTo("past");
        assertThat(v.getTranslations().get(0).getText()).isEqualTo("идти");
        assertThat(v.getSynonyms().get(0).getId()).isEqualTo("en:walk");
        assertThat(v.getAntonyms().get(0).getId()).isEqualTo("en:stop");
        assertThat(v.getHypernyms().get(0).getId()).isEqualTo("en:move");
    }

    @Test
    void toWordCard_nullables_mappedAsNull_emptyLists() {
        var variant = new LexemeVariant(null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of());
        var word = new Word("x:y", "y", null, null, List.of(variant));

        var dto = mapper.toWordCard(new WordCard(word, MasteryStatus.UNKNOWN));

        assertThat(dto.getIpa()).isNull();
        assertThat(dto.getFreqRank()).isNull();
        assertThat(dto.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.UNKNOWN);
        var v = dto.getVariants().get(0);
        assertThat(v.getPos()).isNull();
        assertThat(v.getCefr()).isNull();
        assertThat(v.getForms()).isEmpty();
        assertThat(v.getTranslations()).isEmpty();
        assertThat(v.getSynonyms()).isEmpty();
    }

    @Test
    void toWordRef_mapsIdLemmaPos_nullPosStaysNull() {
        var withPos = mapper.toWordRef(new WordRef("en:go", "go", PartOfSpeech.VERB));
        assertThat(withPos.getId()).isEqualTo("en:go");
        assertThat(withPos.getLemma()).isEqualTo("go");
        assertThat(withPos.getPos()).isEqualTo(com.eunoia.application.learning.model.PartOfSpeech.VERB);

        var noPos = mapper.toWordRef(new WordRef("en:thing", "thing", null));
        assertThat(noPos.getPos()).isNull();
    }

    @Test
    void toTopicRef_mapsFields_andList() {
        Topic topic = new Topic("topic:travel", "Travel", "travel");

        var dto = mapper.toTopicRef(topic);
        assertThat(dto.getId()).isEqualTo("topic:travel");
        assertThat(dto.getName()).isEqualTo("Travel");
        assertThat(dto.getSlug()).isEqualTo("travel");

        var list = mapper.toTopicRefs(List.of(topic));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Travel");
    }

    @Test
    void toTopicView_mapsTopicAndWordLeaves_withTopics() {
        Topic topic = new Topic("topic:travel", "Travel", "travel");
        var known = new WordLeaf("en:go", "go",
                List.of(PartOfSpeech.VERB, PartOfSpeech.NOUN), Cefr.A1,
                List.of(new Topic("topic:food", "Еда", "food")), MasteryStatus.KNOWN);
        var unknown = new WordLeaf("en:run", "run", List.of(), null, List.of(), MasteryStatus.UNKNOWN);

        var dto = mapper.toTopicView(new TopicView(topic, List.of(known, unknown)));

        assertThat(dto.getTopic().getId()).isEqualTo("topic:travel");
        assertThat(dto.getWords()).hasSize(2);
        var leaf0 = dto.getWords().get(0);
        assertThat(leaf0.getId()).isEqualTo("en:go");
        assertThat(leaf0.getLemma()).isEqualTo("go");
        assertThat(leaf0.getPos()).containsExactly(
                com.eunoia.application.learning.model.PartOfSpeech.VERB,
                com.eunoia.application.learning.model.PartOfSpeech.NOUN);
        assertThat(leaf0.getCefr()).isEqualTo(com.eunoia.application.learning.model.Cefr.A1);
        assertThat(leaf0.getTopics()).extracting(com.eunoia.application.learning.model.TopicRef::getId)
                .containsExactly("topic:food");
        assertThat(leaf0.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.KNOWN);
        var leaf1 = dto.getWords().get(1);
        assertThat(leaf1.getPos()).isEmpty();
        assertThat(leaf1.getTopics()).isEmpty();
        assertThat(leaf1.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.UNKNOWN);
    }

    @Test
    void toBand_mapsFields_andList() {
        var band = new ru.eunoia.application.learning.domain.model.Band("top-100", "Топ-100", 1, 100, 100, 34, 12);

        var dto = mapper.toBand(band);
        assertThat(dto.getId()).isEqualTo("top-100");
        assertThat(dto.getLabel()).isEqualTo("Топ-100");
        assertThat(dto.getFromRank()).isEqualTo(1);
        assertThat(dto.getToRank()).isEqualTo(100);
        assertThat(dto.getTotal()).isEqualTo(100);
        assertThat(dto.getKnown()).isEqualTo(34);
        assertThat(dto.getLearning()).isEqualTo(12);
        assertThat(mapper.toBands(List.of(band))).hasSize(1);
    }

    @Test
    void toWordPage_mapsPagingAndLeaves() {
        var leaf = new WordLeaf("en:go", "go", List.of(PartOfSpeech.VERB), Cefr.A1, List.of(), MasteryStatus.KNOWN);

        var dto = mapper.toWordPage(new WordPage(2500, 0, 100, List.of(leaf)));

        assertThat(dto.getTotal()).isEqualTo(2500);
        assertThat(dto.getOffset()).isZero();
        assertThat(dto.getLimit()).isEqualTo(100);
        assertThat(dto.getWords()).hasSize(1);
        assertThat(dto.getWords().get(0).getId()).isEqualTo("en:go");
    }

    @Test
    void toWordLeaves_mapsList() {
        var leaf = new WordLeaf("en:go", "go", List.of(PartOfSpeech.VERB), Cefr.A1,
                List.of(), MasteryStatus.LEARNING);

        assertThat(mapper.toWordLeaves(List.of(leaf))).hasSize(1);
    }

    @Test
    void toGrammarView_mapsFields_prerequisites_illustratedBy() {
        var view = new ru.eunoia.application.learning.domain.model.GrammarView(
                new Grammar("gr:past-simple", "Past Simple", Cefr.A2, List.of("gr:present-simple")),
                List.of(new WordRef("en:go", "go", PartOfSpeech.VERB)));

        var dto = mapper.toGrammarView(view);

        assertThat(dto.getId()).isEqualTo("gr:past-simple");
        assertThat(dto.getName()).isEqualTo("Past Simple");
        assertThat(dto.getCefr()).isEqualTo(com.eunoia.application.learning.model.Cefr.A2);
        assertThat(dto.getPrerequisites()).containsExactly("gr:present-simple");
        assertThat(dto.getIllustratedBy()).hasSize(1);
        assertThat(dto.getIllustratedBy().get(0).getId()).isEqualTo("en:go");
    }

    @Test
    void toGrammarView_nullCefr_emptyLists() {
        var view = new ru.eunoia.application.learning.domain.model.GrammarView(
                new Grammar("gr:x", "X", null, List.of()), List.of());

        var dto = mapper.toGrammarView(view);

        assertThat(dto.getCefr()).isNull();
        assertThat(dto.getPrerequisites()).isEmpty();
        assertThat(dto.getIllustratedBy()).isEmpty();
    }

    @Test
    void toGrammarViews_mapsList() {
        var view = new ru.eunoia.application.learning.domain.model.GrammarView(
                new Grammar("gr:a", "A", Cefr.A1, List.of()), List.of());

        assertThat(mapper.toGrammarViews(List.of(view))).hasSize(1);
    }

    @Test
    void toMasteryView_mapsWordId_status_andList() {
        Mastery mastery = new Mastery(USER, "en:go", MasteryStatus.KNOWN, UPDATED);

        var dto = mapper.toMasteryView(mastery);
        assertThat(dto.getWordId()).isEqualTo("en:go");
        assertThat(dto.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.KNOWN);
        assertThat(dto.getUpdatedAt()).isEqualTo(UPDATED);

        var list = mapper.toMasteryViews(List.of(mastery));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getWordId()).isEqualTo("en:go");
    }

    @Test
    void toDomainStatus_mapsEveryStatusByName() {
        assertThat(mapper.toDomainStatus(com.eunoia.application.learning.model.MasteryStatus.KNOWN))
                .isEqualTo(MasteryStatus.KNOWN);
        assertThat(mapper.toDomainStatus(com.eunoia.application.learning.model.MasteryStatus.LEARNING))
                .isEqualTo(MasteryStatus.LEARNING);
        assertThat(mapper.toDomainStatus(com.eunoia.application.learning.model.MasteryStatus.UNKNOWN))
                .isEqualTo(MasteryStatus.UNKNOWN);
    }
}
