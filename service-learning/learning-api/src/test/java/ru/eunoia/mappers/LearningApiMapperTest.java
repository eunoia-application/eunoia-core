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
import ru.eunoia.application.knowledge.domain.model.Lexeme;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Translation;
import ru.eunoia.application.learning.domain.model.GardenLeaf;
import ru.eunoia.application.learning.domain.model.LexemeCard;
import ru.eunoia.application.learning.domain.model.TopicView;

/**
 * Чистое преобразование домен → DTO контракта. Без моков — конструируем реальные доменные объекты
 * и сверяем каждое поле DTO. Домен и контракт держат одинаковые простые имена, поэтому доменные
 * типы импортируем, а DTO называем полным путём. Enum'ы едут по имени; отдельно проверяем nullable
 * (pos/cefr) и то, что у листа сада CEFR всегда null.
 */
class LearningApiMapperTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 6, 7, 8, 9, 10);

    private final LearningApiMapper mapper = new LearningApiMapper();

    @Test
    void toLexemeCard_mapsAttributes_forms_translations_relations_enums_status() {
        Lexeme lexeme = new Lexeme("en:go:VERB", "go", PartOfSpeech.VERB, "en", Cefr.A1, 42,
                List.of(new Form("went", "past")),
                List.of(new Translation("идти", "ru")));
        LexemeRef syn = new LexemeRef("en:walk:VERB", "walk", PartOfSpeech.VERB);
        LexemeRef ant = new LexemeRef("en:stop:VERB", "stop", PartOfSpeech.VERB);
        LexemeRef hyp = new LexemeRef("en:move:VERB", "move", PartOfSpeech.VERB);
        LexemeCard domain = new LexemeCard(lexeme, List.of(syn), List.of(ant), List.of(hyp),
                MasteryStatus.LEARNING);

        var dto = mapper.toLexemeCard(domain);

        assertThat(dto.getId()).isEqualTo("en:go:VERB");
        assertThat(dto.getLemma()).isEqualTo("go");
        assertThat(dto.getPos()).isEqualTo(com.eunoia.application.learning.model.PartOfSpeech.VERB);
        assertThat(dto.getCefr()).isEqualTo(com.eunoia.application.learning.model.Cefr.A1);
        assertThat(dto.getFreqRank()).isEqualTo(42);
        assertThat(dto.getForms()).hasSize(1);
        assertThat(dto.getForms().get(0).getText()).isEqualTo("went");
        assertThat(dto.getForms().get(0).getFeature()).isEqualTo("past");
        assertThat(dto.getTranslations()).hasSize(1);
        assertThat(dto.getTranslations().get(0).getText()).isEqualTo("идти");
        assertThat(dto.getTranslations().get(0).getLang()).isEqualTo("ru");
        assertThat(dto.getSynonyms()).hasSize(1);
        assertThat(dto.getSynonyms().get(0).getId()).isEqualTo("en:walk:VERB");
        assertThat(dto.getSynonyms().get(0).getLemma()).isEqualTo("walk");
        assertThat(dto.getSynonyms().get(0).getPos())
                .isEqualTo(com.eunoia.application.learning.model.PartOfSpeech.VERB);
        assertThat(dto.getAntonyms().get(0).getId()).isEqualTo("en:stop:VERB");
        assertThat(dto.getHypernyms().get(0).getId()).isEqualTo("en:move:VERB");
        assertThat(dto.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.LEARNING);
    }

    @Test
    void toLexemeCard_nullablePosAndCefrAndFreqRank_mappedAsNull_emptyLists() {
        Lexeme lexeme = new Lexeme("x:y:OTHER", "y", null, "en", null, null, List.of(), List.of());
        LexemeCard domain = new LexemeCard(lexeme, List.of(), List.of(), List.of(), MasteryStatus.UNKNOWN);

        var dto = mapper.toLexemeCard(domain);

        assertThat(dto.getPos()).isNull();
        assertThat(dto.getCefr()).isNull();
        assertThat(dto.getFreqRank()).isNull();
        assertThat(dto.getForms()).isEmpty();
        assertThat(dto.getTranslations()).isEmpty();
        assertThat(dto.getSynonyms()).isEmpty();
        assertThat(dto.getAntonyms()).isEmpty();
        assertThat(dto.getHypernyms()).isEmpty();
        assertThat(dto.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.UNKNOWN);
    }

    @Test
    void toRef_mapsIdLemmaPos_nullPosStaysNull() {
        var withPos = mapper.toRef(new LexemeRef("en:go:VERB", "go", PartOfSpeech.VERB));
        assertThat(withPos.getId()).isEqualTo("en:go:VERB");
        assertThat(withPos.getLemma()).isEqualTo("go");
        assertThat(withPos.getPos()).isEqualTo(com.eunoia.application.learning.model.PartOfSpeech.VERB);

        var noPos = mapper.toRef(new LexemeRef("misc:thing", "thing", null));
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
    void toTopicView_mapsTopicAndLeaves_leafCefrAlwaysNull() {
        Topic topic = new Topic("topic:travel", "Travel", "travel");
        GardenLeaf known = new GardenLeaf(new LexemeRef("en:go:VERB", "go", PartOfSpeech.VERB),
                MasteryStatus.KNOWN);
        GardenLeaf unknown = new GardenLeaf(new LexemeRef("en:run:VERB", "run", null),
                MasteryStatus.UNKNOWN);
        TopicView domain = new TopicView(topic, List.of(known, unknown));

        var dto = mapper.toTopicView(domain);

        assertThat(dto.getTopic().getId()).isEqualTo("topic:travel");
        assertThat(dto.getLexemes()).hasSize(2);
        var leaf0 = dto.getLexemes().get(0);
        assertThat(leaf0.getId()).isEqualTo("en:go:VERB");
        assertThat(leaf0.getLemma()).isEqualTo("go");
        assertThat(leaf0.getPos()).isEqualTo(com.eunoia.application.learning.model.PartOfSpeech.VERB);
        assertThat(leaf0.getCefr()).isNull();
        assertThat(leaf0.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.KNOWN);
        var leaf1 = dto.getLexemes().get(1);
        assertThat(leaf1.getPos()).isNull();
        assertThat(leaf1.getCefr()).isNull();
        assertThat(leaf1.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.UNKNOWN);
    }

    @Test
    void toGrammarView_mapsFields_withCefr() {
        var dto = mapper.toGrammarView(new Grammar("gr:present-simple", "Present Simple", Cefr.B1));

        assertThat(dto.getId()).isEqualTo("gr:present-simple");
        assertThat(dto.getName()).isEqualTo("Present Simple");
        assertThat(dto.getCefr()).isEqualTo(com.eunoia.application.learning.model.Cefr.B1);
    }

    @Test
    void toGrammarView_nullCefr_staysNull() {
        var dto = mapper.toGrammarView(new Grammar("gr:x", "X", null));

        assertThat(dto.getCefr()).isNull();
    }

    @Test
    void toMasteryView_mapsFields_andList() {
        Mastery mastery = new Mastery(USER, "en:go:VERB", MasteryStatus.KNOWN, UPDATED);

        var dto = mapper.toMasteryView(mastery);
        assertThat(dto.getLexemeId()).isEqualTo("en:go:VERB");
        assertThat(dto.getStatus()).isEqualTo(com.eunoia.application.learning.model.MasteryStatus.KNOWN);
        assertThat(dto.getUpdatedAt()).isEqualTo(UPDATED);

        var list = mapper.toMasteryViews(List.of(mastery));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getLexemeId()).isEqualTo("en:go:VERB");
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
