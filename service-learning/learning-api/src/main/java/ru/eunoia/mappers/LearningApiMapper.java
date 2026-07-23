package ru.eunoia.mappers;

import com.eunoia.application.learning.model.Cefr;
import com.eunoia.application.learning.model.Form;
import com.eunoia.application.learning.model.GardenLeaf;
import com.eunoia.application.learning.model.GrammarView;
import com.eunoia.application.learning.model.LexemeCard;
import com.eunoia.application.learning.model.LexemeRef;
import com.eunoia.application.learning.model.MasteryStatus;
import com.eunoia.application.learning.model.MasteryView;
import com.eunoia.application.learning.model.PartOfSpeech;
import com.eunoia.application.learning.model.TopicRef;
import com.eunoia.application.learning.model.TopicView;
import com.eunoia.application.learning.model.Translation;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Перевод доменных view/моделей учёбы в DTO контракта. Домен и контракт держат одинаковые
 * простые имена (LexemeCard, LexemeRef, Form, MasteryStatus…), поэтому DTO импортируем, а
 * доменные типы называем полным путём. Enum'ы переносим по имени константы: имена в контракте
 * и в домене совпадают (NOUN, A1, KNOWN…), значит valueOf(name()) безопасен.
 */
@Component
public class LearningApiMapper {

    /** Карточка слова: атрибуты + формы/переводы + связи + мой статус. */
    public LexemeCard toLexemeCard(ru.eunoia.application.learning.domain.model.LexemeCard card) {
        var lexeme = card.lexeme();
        LexemeCard dto = new LexemeCard();
        dto.setId(lexeme.id());
        dto.setLemma(lexeme.lemma());
        dto.setPos(toPos(lexeme.pos()));
        dto.setCefr(toCefr(lexeme.cefr()));
        dto.setFreqRank(lexeme.freqRank());
        dto.setForms(toForms(lexeme.forms()));
        dto.setTranslations(toTranslations(lexeme.translations()));
        dto.setSynonyms(toRefs(card.synonyms()));
        dto.setAntonyms(toRefs(card.antonyms()));
        dto.setHypernyms(toRefs(card.hypernyms()));
        dto.setStatus(toStatus(card.status()));
        return dto;
    }

    /** Лёгкая ссылка на слово (связи, поиск, слова темы). */
    public LexemeRef toRef(ru.eunoia.application.knowledge.domain.model.LexemeRef ref) {
        LexemeRef dto = new LexemeRef();
        dto.setId(ref.id());
        dto.setLemma(ref.lemma());
        dto.setPos(toPos(ref.pos()));
        return dto;
    }

    public List<LexemeRef> toRefs(List<ru.eunoia.application.knowledge.domain.model.LexemeRef> refs) {
        return refs.stream().map(this::toRef).toList();
    }

    /** Корневая тема. */
    public TopicRef toTopicRef(ru.eunoia.application.knowledge.domain.model.Topic topic) {
        TopicRef dto = new TopicRef();
        dto.setId(topic.id());
        dto.setName(topic.name());
        dto.setSlug(topic.slug());
        return dto;
    }

    public List<TopicRef> toTopicRefs(List<ru.eunoia.application.knowledge.domain.model.Topic> topics) {
        return topics.stream().map(this::toTopicRef).toList();
    }

    /** Ветка сада: тема + её слова с раскраской по мастерству. */
    public TopicView toTopicView(ru.eunoia.application.learning.domain.model.TopicView view) {
        TopicView dto = new TopicView();
        dto.setTopic(toTopicRef(view.topic()));
        dto.setLexemes(view.leaves().stream().map(this::toLeaf).toList());
        return dto;
    }

    /** Грамматическое правило. */
    public GrammarView toGrammarView(ru.eunoia.application.knowledge.domain.model.Grammar grammar) {
        GrammarView dto = new GrammarView();
        dto.setId(grammar.id());
        dto.setName(grammar.name());
        dto.setCefr(toCefr(grammar.cefr()));
        return dto;
    }

    /** Отметка мастерства (мой прогресс). */
    public MasteryView toMasteryView(ru.eunoia.application.garden.domain.model.Mastery mastery) {
        MasteryView dto = new MasteryView();
        dto.setLexemeId(mastery.lexemeId());
        dto.setStatus(toStatus(mastery.status()));
        dto.setUpdatedAt(mastery.updatedAt());
        return dto;
    }

    public List<MasteryView> toMasteryViews(List<ru.eunoia.application.garden.domain.model.Mastery> list) {
        return list.stream().map(this::toMasteryView).toList();
    }

    /** DTO-статус из запроса → доменный (для upsert мастерства). */
    public ru.eunoia.application.garden.domain.model.MasteryStatus toDomainStatus(MasteryStatus status) {
        return ru.eunoia.application.garden.domain.model.MasteryStatus.valueOf(status.name());
    }

    /** Слово-лист темы: id/лемма/часть речи из ссылки + мой статус. CEFR у листа нет — знает карточка. */
    private GardenLeaf toLeaf(ru.eunoia.application.learning.domain.model.GardenLeaf leaf) {
        var ref = leaf.lexeme();
        GardenLeaf dto = new GardenLeaf();
        dto.setId(ref.id());
        dto.setLemma(ref.lemma());
        dto.setPos(toPos(ref.pos()));
        dto.setCefr(null);
        dto.setStatus(toStatus(leaf.status()));
        return dto;
    }

    private List<Form> toForms(List<ru.eunoia.application.knowledge.domain.model.Form> forms) {
        return forms.stream().map(this::toForm).toList();
    }

    private Form toForm(ru.eunoia.application.knowledge.domain.model.Form form) {
        Form dto = new Form();
        dto.setText(form.text());
        dto.setFeature(form.feature());
        return dto;
    }

    private List<Translation> toTranslations(List<ru.eunoia.application.knowledge.domain.model.Translation> translations) {
        return translations.stream().map(this::toTranslation).toList();
    }

    private Translation toTranslation(ru.eunoia.application.knowledge.domain.model.Translation translation) {
        Translation dto = new Translation();
        dto.setText(translation.text());
        dto.setLang(translation.lang());
        return dto;
    }

    /** Часть речи nullable — пробрасываем null как есть. */
    private PartOfSpeech toPos(ru.eunoia.application.knowledge.domain.model.PartOfSpeech pos) {
        return pos == null ? null : PartOfSpeech.valueOf(pos.name());
    }

    /** CEFR nullable — пробрасываем null как есть. */
    private Cefr toCefr(ru.eunoia.application.knowledge.domain.model.Cefr cefr) {
        return cefr == null ? null : Cefr.valueOf(cefr.name());
    }

    private MasteryStatus toStatus(ru.eunoia.application.garden.domain.model.MasteryStatus status) {
        return MasteryStatus.valueOf(status.name());
    }
}
