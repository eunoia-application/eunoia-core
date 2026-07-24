package ru.eunoia.mappers;

import com.eunoia.application.learning.model.Band;
import com.eunoia.application.learning.model.Cefr;
import com.eunoia.application.learning.model.Form;
import com.eunoia.application.learning.model.GrammarView;
import com.eunoia.application.learning.model.MasteryStatus;
import com.eunoia.application.learning.model.MasteryView;
import com.eunoia.application.learning.model.PartOfSpeech;
import com.eunoia.application.learning.model.TopicRef;
import com.eunoia.application.learning.model.TopicView;
import com.eunoia.application.learning.model.Translation;
import com.eunoia.application.learning.model.WordCard;
import com.eunoia.application.learning.model.WordLeaf;
import com.eunoia.application.learning.model.WordPage;
import com.eunoia.application.learning.model.WordRef;
import com.eunoia.application.learning.model.WordVariant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Перевод доменных view/моделей учёбы в DTO контракта. Единица — СЛОВО (лемма): карточка = лемма
 * + части речи (variants). Домен и контракт держат одинаковые простые имена (WordCard, WordRef,
 * Form…), поэтому DTO импортируем, а доменные типы называем полным путём. Enum'ы переносим по
 * имени константы: имена в контракте и домене совпадают (NOUN, A1, KNOWN…) → valueOf(name()) безопасен.
 */
@Component
public class LearningApiMapper {

    /** Карточка слова: лемма + части речи (variants) + мой статус (по лемме). */
    public WordCard toWordCard(ru.eunoia.application.learning.domain.model.WordCard card) {
        var word = card.word();
        WordCard dto = new WordCard();
        dto.setId(word.id());
        dto.setLemma(word.lemma());
        dto.setIpa(word.ipa());
        dto.setFreqRank(word.freqRank());
        dto.setStatus(toStatus(card.status()));
        dto.setVariants(word.variants().stream().map(this::toVariant).toList());
        return dto;
    }

    /** Одна часть речи слова: её атрибуты, формы, переводы и связи. */
    private WordVariant toVariant(ru.eunoia.application.knowledge.domain.model.LexemeVariant v) {
        WordVariant dto = new WordVariant();
        dto.setPos(toPos(v.pos()));
        dto.setCefr(toCefr(v.cefr()));
        dto.setFreqRank(v.freqRank());
        dto.setTranslations(toTranslations(v.translations()));
        dto.setForms(toForms(v.forms()));
        dto.setSynonyms(toWordRefs(v.synonyms()));
        dto.setAntonyms(toWordRefs(v.antonyms()));
        dto.setHypernyms(toWordRefs(v.hypernyms()));
        return dto;
    }

    /** Лёгкая ссылка на слово (связи, поиск). id — ключ леммы. */
    public WordRef toWordRef(ru.eunoia.application.knowledge.domain.model.WordRef ref) {
        WordRef dto = new WordRef();
        dto.setId(ref.id());
        dto.setLemma(ref.lemma());
        dto.setPos(toPos(ref.pos()));
        return dto;
    }

    public List<WordRef> toWordRefs(List<ru.eunoia.application.knowledge.domain.model.WordRef> refs) {
        return refs.stream().map(this::toWordRef).toList();
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

    /** Ветка сада: тема + её слова (леммы) с раскраской по мастерству. */
    public TopicView toTopicView(ru.eunoia.application.learning.domain.model.TopicView view) {
        TopicView dto = new TopicView();
        dto.setTopic(toTopicRef(view.topic()));
        dto.setWords(view.words().stream().map(this::toLeaf).toList());
        return dto;
    }

    /** Страница списка всех слов (по частоте). */
    public WordPage toWordPage(ru.eunoia.application.learning.domain.model.WordPage page) {
        WordPage dto = new WordPage();
        dto.setTotal(page.total());
        dto.setOffset(page.offset());
        dto.setLimit(page.limit());
        dto.setWords(page.words().stream().map(this::toLeaf).toList());
        return dto;
    }

    /** Грамматическое правило: атрибуты + порядок (prerequisites) + слова-примеры. */
    public GrammarView toGrammarView(ru.eunoia.application.learning.domain.model.GrammarView view) {
        var grammar = view.grammar();
        GrammarView dto = new GrammarView();
        dto.setId(grammar.id());
        dto.setName(grammar.name());
        dto.setCefr(toCefr(grammar.cefr()));
        dto.setPrerequisites(grammar.prerequisites());
        dto.setIllustratedBy(toWordRefs(view.illustratedBy()));
        return dto;
    }

    public List<GrammarView> toGrammarViews(List<ru.eunoia.application.learning.domain.model.GrammarView> views) {
        return views.stream().map(this::toGrammarView).toList();
    }

    /** Отметка мастерства (мой прогресс). wordId — ключ леммы. */
    public MasteryView toMasteryView(ru.eunoia.application.garden.domain.model.Mastery mastery) {
        MasteryView dto = new MasteryView();
        dto.setWordId(mastery.lexemeId());   // lexemeId теперь хранит ключ леммы (en:go)
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

    /** Блоки топ-слов (уровни) с прогрессом. */
    public Band toBand(ru.eunoia.application.learning.domain.model.Band band) {
        Band dto = new Band();
        dto.setId(band.id());
        dto.setLabel(band.label());
        dto.setFromRank(band.fromRank());
        dto.setToRank(band.toRank());
        dto.setTotal(band.total());
        dto.setKnown(band.known());
        dto.setLearning(band.learning());
        return dto;
    }

    public List<Band> toBands(List<ru.eunoia.application.learning.domain.model.Band> bands) {
        return bands.stream().map(this::toBand).toList();
    }

    /** Список слов-листьев (напр. очередь «Учить»). */
    public List<WordLeaf> toWordLeaves(List<ru.eunoia.application.learning.domain.model.WordLeaf> leaves) {
        return leaves.stream().map(this::toLeaf).toList();
    }

    /** Слово-лист: id (ключ леммы) + лемма + части речи + темы + мой статус. */
    private WordLeaf toLeaf(ru.eunoia.application.learning.domain.model.WordLeaf leaf) {
        WordLeaf dto = new WordLeaf();
        dto.setId(leaf.id());
        dto.setLemma(leaf.lemma());
        dto.setPos(leaf.pos().stream().map(this::toPos).toList());
        dto.setCefr(toCefr(leaf.cefr()));
        dto.setTopics(leaf.topics().stream().map(this::toTopicRef).toList());
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
