package ru.eunoia.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Копилка лексем по id. Схема kaikki рыхлая, поэтому читаем дерево {@link JsonNode}, а не
 * типизированную модель. add() мержит формы/переводы/связи одной kaikki-записи в уже
 * накопленную лексему — одно слово встречается в файле несколько раз (разные этимологии),
 * и все они складываются в одну лексему.
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
     * Добавляет kaikki-запись слова. pos уже смапплен вызывающим в имя enum
     * (VERB/NOUN/ADJECTIVE/ADVERB), rank взят из частотника. id = "en:{lemma}:{POS}";
     * при повторном id всё домерживается в существующую запись.
     */
    void add(JsonNode entry, String pos, int rank) {
        String lemma = entry.path("word").asText("").toLowerCase();
        if (lemma.isEmpty()) {
            return;
        }
        String id = "en:" + lemma + ":" + pos;
        LexemeData data = byId.computeIfAbsent(id,
                k -> new LexemeData(id, lemma, pos, rank, cefrOf(rank)));

        collectForms(entry, data);
        collectTranslations(entry, data);
        collectRelations(entry, id, pos, data);
    }

    // --- формы: берём только принципиальные части речи, мусор таблиц спряжения отбрасываем ---

    private void collectForms(JsonNode entry, LexemeData data) {
        for (JsonNode f : entry.path("forms")) {
            if (f.has("source")) {
                continue;                       // source => форма из таблицы спряжения (мусор)
            }
            String text = f.path("form").asText("");
            if (text.isEmpty() || text.equals("no-table-tags") || text.equals("glossary")) {
                continue;                       // служебные «формы»-заглушки
            }
            List<String> tags = strings(f.path("tags"));
            if (tags.contains("table-tags") || tags.contains("inflection-template")
                    || tags.contains("obsolete") || tags.contains("nonstandard")) {
                continue;                       // мета-теги таблиц и «грязные» формы
            }
            data.forms.add(new Form(text, String.join(" ", tags)));   // дедуп по (text, feature)
        }
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
