package ru.eunoia.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Копилка лексем по id. Схема kaikki рыхлая, поэтому читаем дерево {@link JsonNode}, а не
 * типизированную модель. add() мержит формы/переводы/связи одной kaikki-записи в уже
 * накопленную лексему — одно слово встречается в файле несколько раз (разные этимологии),
 * и все они складываются в одну лексему.
 *
 * <p>Сюда попадают только <b>леммы</b>: записи-словоформы (form-of, напр. went/goes/better)
 * отсекает {@link #isLemmaEntry} ещё до add() — иначе форма стала бы отдельным «словом»
 * (источник дублей и слов без перевода). Сами формы приходят как HAS_FORM у своей леммы.
 */
final class LexemeAccumulator {

    private final Map<String, LexemeData> byId = new LinkedHashMap<>();

    /** Все накопленные лексемы (в порядке первого появления). */
    Collection<LexemeData> lexemes() {
        return byId.values();
    }

    /** Множество импортированных id — по нему отсекаем висячие рёбра на загрузке связей. */
    Set<String> importedIds() {
        return byId.keySet();
    }

    /**
     * Добавляет kaikki-запись <b>леммы</b>. pos уже смапплен вызывающим в имя enum
     * (VERB/NOUN/ADJECTIVE/ADVERB), rank — свёрнутая частота (минимум по формам). id =
     * "en:{lemma}:{POS}"; при повторном id домерживаем данные и держим наименьший ранг.
     */
    void add(JsonNode entry, String pos, int rank) {
        String lemma = entry.path("word").asText("").toLowerCase();
        if (lemma.isEmpty()) {
            return;
        }
        String id = "en:" + lemma + ":" + pos;
        LexemeData data = byId.get(id);
        if (data == null) {
            data = new LexemeData(id, lemma, pos, rank);
            byId.put(id, data);
        } else {
            data.mergeRank(rank);   // другая этимология той же леммы → берём частоту получше
        }

        collectForms(entry, data);
        collectTranslations(entry, data);
        collectRelations(entry, id, pos, data);
        collectCategories(entry, data);
        collectIpa(entry, data);
    }

    /**
     * Оставляет только топ-N самых частотных лексем (по свёрнутому рангу), остальные
     * выбрасывает вместе с их id — так на загрузке рёбер не будет висячих связей на
     * выкинутые слова. Вызывать один раз, после разбора всех kaikki-файлов.
     */
    void retainTop(int limit) {
        if (byId.size() <= limit) {
            return;
        }
        List<LexemeData> sorted = new ArrayList<>(byId.values());
        sorted.sort(Comparator.comparingInt(d -> d.freqRank));
        Map<String, LexemeData> kept = new LinkedHashMap<>();
        for (int i = 0; i < limit; i++) {
            LexemeData d = sorted.get(i);
            kept.put(d.id, d);
        }
        byId.clear();
        byId.putAll(kept);
    }

    // --- распознавание леммы vs словоформы ---

    /**
     * false — если запись это словоформа (все её смыслы — form-of, напр. "went" = прош. от
     * "go"). Такие записи в граф отдельным словом не идут: форма живёт как HAS_FORM у леммы.
     * true — если есть хотя бы один «настоящий» смысл (или смыслов нет вовсе — пусть решают
     * остальные фильтры).
     */
    static boolean isLemmaEntry(JsonNode entry) {
        JsonNode senses = entry.path("senses");
        if (!senses.isArray() || senses.isEmpty()) {
            return true;
        }
        for (JsonNode sense : senses) {
            if (!isInflectionSense(sense)) {
                return true;           // нашёлся не-словоформенный смысл → это лемма
            }
        }
        return false;                  // все смыслы — form-of → это словоформа
    }

    /** Смысл-словоформа: есть form_of-ссылка на лемму либо тег "form-of". */
    private static boolean isInflectionSense(JsonNode sense) {
        JsonNode formOf = sense.path("form_of");
        if (formOf.isArray() && !formOf.isEmpty()) {
            return true;
        }
        for (JsonNode tag : sense.path("tags")) {
            if ("form-of".equals(tag.asText())) {
                return true;
            }
        }
        return false;
    }

    // --- формы: берём только принципиальные части речи, мусор таблиц спряжения отбрасываем ---

    private void collectForms(JsonNode entry, LexemeData data) {
        for (JsonNode f : entry.path("forms")) {
            String text = cleanFormText(f);
            if (text == null) {
                continue;
            }
            List<String> tags = strings(f.path("tags"));
            data.forms.add(new Form(text, String.join(" ", tags)));   // дедуп по (text, feature)
        }
    }

    /**
     * Текст словоформы, годной для графа, либо null — если это мусор таблиц спряжения или
     * служебная заглушка. Общая точка для двух задач: сбора HAS_FORM и свёртки частоты формы
     * в лемму (см. {@link #formTexts}).
     */
    static String cleanFormText(JsonNode f) {
        if (f.has("source")) {
            return null;                        // source => форма из таблицы спряжения (мусор)
        }
        String text = f.path("form").asText("");
        if (text.isEmpty() || text.equals("no-table-tags") || text.equals("glossary")) {
            return null;                        // служебные «формы»-заглушки
        }
        List<String> tags = strings(f.path("tags"));
        if (tags.contains("table-tags") || tags.contains("inflection-template")
                || tags.contains("obsolete") || tags.contains("nonstandard")
                || tags.contains("alternative") || tags.contains("abbreviation")
                || tags.contains("initialism")) {
            return null;                        // мета-теги таблиц, «грязные» и НЕ-словоизменительные
        }
        return text;                            // (alternative/abbreviation тащат чужую частоту — напр. a→a.m.)
    }

    /** Годные словоформы записи (тексты) — для свёртки их частоты в лемму. */
    static List<String> formTexts(JsonNode entry) {
        List<String> out = new ArrayList<>();
        for (JsonNode f : entry.path("forms")) {
            String text = cleanFormText(f);
            if (text != null) {
                out.add(text);
            }
        }
        return out;
    }

    // --- переводы: только русские ---

    private void collectTranslations(JsonNode entry, LexemeData data) {
        for (JsonNode t : entry.path("translations")) {
            if (!"ru".equals(t.path("lang_code").asText(""))) {
                continue;
            }
            String word = t.path("word").asText("");
            if (!word.isEmpty()) {
                data.translations.add(new Translation(word, "ru"));
            }
        }
    }

    // --- IPA: одна транскрипция на слово, предпочтительно американская (General-American) ---

    private void collectIpa(JsonNode entry, LexemeData data) {
        if (data.ipa != null) {
            return;                          // уже взяли из предыдущей этимологии
        }
        String rp = null;
        String first = null;
        for (JsonNode s : entry.path("sounds")) {
            String ipa = s.path("ipa").asText("");
            if (ipa.isEmpty()) {
                continue;                    // это не транскрипция (аудио/рифма/омофон)
            }
            List<String> tags = strings(s.path("tags"));
            if (tags.contains("General-American") || tags.contains("US")) {
                data.ipa = ipa;              // американское — берём сразу
                return;
            }
            if (rp == null && (tags.contains("Received-Pronunciation")
                    || tags.contains("British") || tags.contains("UK"))) {
                rp = ipa;
            }
            if (first == null) {
                first = ipa;
            }
        }
        if (rp != null) {
            data.ipa = rp;                   // иначе британское
        } else if (first != null) {
            data.ipa = first;                // иначе первое попавшееся
        }
    }

    // --- категории: имена из senses[].categories — для раскладки слова по темам ---

    private void collectCategories(JsonNode entry, LexemeData data) {
        // Только ГЛАВНОЕ (первое) значение: у вторичных значений часто висит паразитная
        // топик-категория (у throw/birth есть сенс с "Time", хотя это не про время) — она бы
        // утащила слово не в ту ветку. По главному значению — заметно чище.
        JsonNode senses = entry.path("senses");
        if (!senses.isArray() || senses.isEmpty()) {
            return;
        }
        for (JsonNode c : senses.get(0).path("categories")) {
            String name = c.path("name").asText("");
            if (!name.isEmpty()) {
                data.categories.add(name);
            }
        }
    }

    // --- связи: синонимы/антонимы/гиперонимы с верхнего уровня и из каждого sense ---

    private void collectRelations(JsonNode entry, String selfId, String pos, LexemeData data) {
        addTargets(entry.path("synonyms"), pos, selfId, data.synonymIds);
        addTargets(entry.path("antonyms"), pos, selfId, data.antonymIds);
        addTargets(entry.path("hypernyms"), pos, selfId, data.hypernymIds);
        for (JsonNode sense : entry.path("senses")) {
            addTargets(sense.path("synonyms"), pos, selfId, data.synonymIds);
            addTargets(sense.path("antonyms"), pos, selfId, data.antonymIds);
            addTargets(sense.path("hypernyms"), pos, selfId, data.hypernymIds);
        }
    }

    /**
     * Из массива [{"word":"move"},...] собирает targetId той же части речи.
     * Петли (target == сам себе) не добавляем; фильтр «импортирован ли target» — позже.
     */
    private void addTargets(JsonNode arr, String pos, String selfId, Set<String> into) {
        for (JsonNode rel : arr) {
            String word = rel.path("word").asText("").toLowerCase();
            if (word.isEmpty()) {
                continue;
            }
            String targetId = "en:" + word + ":" + pos;
            if (!targetId.equals(selfId)) {
                into.add(targetId);
            }
        }
    }

    // --- helpers ---

    /** CEFR по бакетам ранга частоты — грубая оценка сложности слова. */
    static String cefrOf(int rank) {
        if (rank <= 500) {
            return "A1";
        }
        if (rank <= 1000) {
            return "A2";
        }
        if (rank <= 2000) {
            return "B1";
        }
        if (rank <= 4000) {
            return "B2";
        }
        if (rank <= 7000) {
            return "C1";
        }
        return "C2";
    }

    /** JSON-массив строк → List<String> (пустые значения выкидываем). */
    private static List<String> strings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        for (JsonNode n : arr) {
            String s = n.asText("");
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }
}
