package ru.eunoia.ingest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Курируемый скелет грамматики (ствол сада): правила + порядок изучения. Читается из
 * seed/grammar.tsv. Грамматики в словарных дампах нет — её структуру задаём мы.
 *
 * <p>А вот связь правила со словами ({@code ILLUSTRATES}) выводится из данных: слово
 * «иллюстрирует» правило, если у него есть НЕПРАВИЛЬНАЯ форма по этому правилу (её и надо
 * учить). Три якоря: {@code past-simple} (неправ. прош. глагола), {@code plurals} (неправ.
 * мн. существительного), {@code comparatives} (неправ. степень прилагательного).
 */
final class GrammarCatalog {

    /** Якорные id правил, к которым тул сам цепляет слова по неправильным формам. */
    private static final String PAST_SIMPLE = "past-simple";
    private static final String PLURALS = "plurals";
    private static final String COMPARATIVES = "comparatives";

    /** Правило грамматики: id/имя/уровень + id предшествующего правила (или пусто). */
    record Rule(String id, String name, String cefr, String prereqId) {
    }

    private final List<Rule> rules;
    private final Set<String> ids;   // для проверки, что якорь вообще есть в каталоге
    /** лемма маркер-слова → правила, которые оно сигналит (already/yet → present-perfect). */
    private final Map<String, Set<String>> markerIndex;

    private GrammarCatalog(List<Rule> rules, Map<String, Set<String>> markerIndex) {
        this.rules = rules;
        this.markerIndex = markerIndex;
        this.ids = new LinkedHashSet<>();
        for (Rule r : rules) {
            ids.add(r.id());
        }
    }

    static GrammarCatalog empty() {
        return new GrammarCatalog(List.of(), Map.of());
    }

    /** Все правила (для создания узлов Grammar и рёбер PREREQUISITE). */
    List<Rule> rules() {
        return rules;
    }

    /**
     * Какие грамматические правила иллюстрирует слово — по его неправильным формам.
     * Пустое множество, если ничего примечательного (или якоря нет в каталоге).
     */
    Set<String> illustratedBy(LexemeData d) {
        Set<String> out = new LinkedHashSet<>();
        switch (d.pos) {
            case "VERB" -> {
                if (ids.contains(PAST_SIMPLE) && hasIrregular(d, "past", "participle",
                        GrammarCatalog::isRegularPast)) {
                    out.add(PAST_SIMPLE);
                }
            }
            case "NOUN" -> {
                if (ids.contains(PLURALS) && hasIrregular(d, "plural", null,
                        GrammarCatalog::isRegularPlural)) {
                    out.add(PLURALS);
                }
            }
            case "ADJECTIVE" -> {
                if (ids.contains(COMPARATIVES) && hasIrregular(d, "comparative", null,
                        GrammarCatalog::isRegularComparative)) {
                    out.add(COMPARATIVES);
                }
            }
            default -> { /* прочие части речи авто-правил по формам не имеют */ }
        }
        // маркер-слова: лемма сигналит правило независимо от части речи (already → PP)
        out.addAll(markerIndex.getOrDefault(d.lemma, Set.of()));
        return out;
    }

    /**
     * Есть ли у слова НЕПРАВИЛЬНАЯ форма нужного вида: среди форм ищем помеченную тегом
     * {@code needTag} (и без {@code excludeTag}, если он задан), чья текстовая форма не
     * сходится с регулярным образованием ({@code regular}).
     */
    private static boolean hasIrregular(LexemeData d, String needTag, String excludeTag,
                                        RegularRule regular) {
        for (Form f : d.forms) {
            List<String> tags = List.of(f.feature().split(" "));
            if (!tags.contains(needTag)) {
                continue;
            }
            if (excludeTag != null && tags.contains(excludeTag)) {
                continue;                       // напр. причастие (gone) — это не Past Simple
            }
            if (!regular.isRegular(d.lemma, f.text().toLowerCase())) {
                return true;                    // форма не по правилу → её надо учить
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface RegularRule {
        boolean isRegular(String lemma, String form);
    }

    // --- эвристики регулярного образования: близко, но без полной морфологии ---

    /** Регулярное прош. время: +ed / +d / y→ied / удвоение согласной + ed. */
    static boolean isRegularPast(String lemma, String form) {
        return form.equals(lemma + "ed")
                || form.equals(lemma + "d")
                || (lemma.endsWith("y") && form.equals(chop(lemma) + "ied"))
                || form.equals(doubleLast(lemma) + "ed");
    }

    /** Регулярное мн. число: +s / +es / y→ies. */
    static boolean isRegularPlural(String lemma, String form) {
        return form.equals(lemma + "s")
                || form.equals(lemma + "es")
                || (lemma.endsWith("y") && form.equals(chop(lemma) + "ies"));
    }

    /** Регулярная сравнит. степень: +er / +r / y→ier / удвоение согласной + er. */
    static boolean isRegularComparative(String lemma, String form) {
        return form.equals(lemma + "er")
                || form.equals(lemma + "r")
                || (lemma.endsWith("y") && form.equals(chop(lemma) + "ier"))
                || form.equals(doubleLast(lemma) + "er");
    }

    private static String chop(String s) {
        return s.substring(0, s.length() - 1);
    }

    private static String doubleLast(String s) {
        return s.isEmpty() ? s : s + s.charAt(s.length() - 1);
    }

    /** Читает grammar.tsv: {@code id <TAB> name <TAB> cefr <TAB> prereqId?} */
    static GrammarCatalog load(Path tsv) throws IOException {
        List<Rule> out = new ArrayList<>();
        Map<String, Set<String>> markers = new HashMap<>();
        for (String line : Files.readAllLines(tsv, StandardCharsets.UTF_8)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] col = line.split("\t", -1);
            if (col.length < 3 || col[0].isBlank()) {
                continue;
            }
            String id = col[0].trim();
            String prereq = col.length > 3 ? col[3].trim() : "";
            out.add(new Rule(id, col[1].trim(), col[2].trim(), prereq));
            if (col.length > 4) {                       // 5-я колонка — маркер-слова
                for (String m : col[4].split(",")) {
                    String marker = m.trim().toLowerCase();
                    if (!marker.isEmpty()) {
                        markers.computeIfAbsent(marker, k -> new LinkedHashSet<>()).add(id);
                    }
                }
            }
        }
        return new GrammarCatalog(out, markers);
    }
}
