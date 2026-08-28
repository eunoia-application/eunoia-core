package ru.eunoia.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Второй источник EN→RU-переводов — WikDict (собран из dbnary, мёржит несколько изданий
 * Wiktionary, поэтому покрытие шире английского). Английский Wiktionary даёт русский перевод
 * не всем частотным словам; этим источником добираем дыры.
 *
 * <p>Читаем TSV-выгрузку {@code word <TAB> POS <TAB> "перевод1 | перевод2 | ..."}. Две тонкости
 * реальных данных WikDict:
 * <ul>
 *   <li>часть речи там нередко расходится с kaikki (напр. {@code often} помечен VERB, а у нас
 *       это наречие; наречий в WikDict почти нет вовсе) → ищем сперва по (лемма+POS), а не
 *       нашли — <b>по одной лемме</b> ({@link #forLexeme});</li>
 *   <li>переводы приходят с wiki-разметкой ({@code [[слово]]}, {@code [[цель|показ]]}) — её
 *       чистим, а разделитель элементов — только пробел-пайп-пробел, чтобы внутренний {@code |}
 *       ссылки не рвал перевод.</li>
 * </ul>
 */
final class WikDictTranslations {

    /** ключ "lemma POS" → русские переводы в порядке важности (дедуп, порядок сохранён). */
    private final Map<String, List<String>> byLexeme;
    /** резерв по одной лемме (любая часть речи) — когда POS у источников разошёлся. */
    private final Map<String, List<String>> byLemma;

    private WikDictTranslations(Map<String, List<String>> byLexeme, Map<String, List<String>> byLemma) {
        this.byLexeme = byLexeme;
        this.byLemma = byLemma;
    }

    /** Пустой источник — когда --dict не задан (тул работает как раньше, только kaikki). */
    static WikDictTranslations empty() {
        return new WikDictTranslations(Map.of(), Map.of());
    }

    /**
     * Русские переводы для леммы данной части речи: сначала точный (лемма+POS), затем — резерв
     * по лемме (POS у kaikki и WikDict часто не совпадает). Пустой список, если нет вовсе.
     */
    List<String> forLexeme(String lemma, String pos) {
        List<String> exact = byLexeme.get(key(lemma, pos));
        if (exact != null) {
            return exact;
        }
        return byLemma.getOrDefault(lemma, List.of());
    }

    /** Сколько (лемма+POS) знает словарь. */
    int size() {
        return byLexeme.size();
    }

    private static String key(String lemma, String pos) {
        return lemma + " " + pos;
    }

    /** Читает TSV-выгрузку WikDict, чистя wiki-разметку и собирая оба индекса. */
    static WikDictTranslations load(Path tsv) throws IOException {
        Map<String, Set<String>> byLexeme = new LinkedHashMap<>();
        Map<String, Set<String>> byLemma = new LinkedHashMap<>();
        try (BufferedReader r = Files.newBufferedReader(tsv, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] col = line.split("\t", -1);
                if (col.length < 3) {
                    continue;                       // битая строка — пропускаем
                }
                String lemma = col[0].trim().toLowerCase();
                String pos = col[1].trim();
                if (lemma.isEmpty() || pos.isEmpty()) {
                    continue;
                }
                Set<String> exact = byLexeme.computeIfAbsent(key(lemma, pos), k -> new LinkedHashSet<>());
                Set<String> anyPos = byLemma.computeIfAbsent(lemma, k -> new LinkedHashSet<>());
                // разделитель элементов — пробел-пайп-пробел (внутренний | ссылки [[a|b]] не трогаем)
                for (String raw : col[2].split("\\s+\\|\\s+")) {
                    String ru = cleanWikitext(raw);
                    if (!ru.isEmpty()) {
                        exact.add(ru);
                        anyPos.add(ru);
                    }
                }
            }
        }
        return new WikDictTranslations(freeze(byLexeme), freeze(byLemma));
    }

    /** Чистит wiki-разметку: {@code [[цель|показ]]→показ}, {@code [[слово]]→слово}. */
    static String cleanWikitext(String s) {
        s = s.replaceAll("\\[\\[[^\\]|]*\\|([^\\]]*)\\]\\]", "$1");   // [[цель|показ]] -> показ
        s = s.replaceAll("\\[\\[([^\\]]*)\\]\\]", "$1");             // [[слово]] -> слово
        return s.replace("[[", "").replace("]]", "").trim();
    }

    private static Map<String, List<String>> freeze(Map<String, Set<String>> src) {
        Map<String, List<String>> out = new LinkedHashMap<>(src.size() * 2);
        for (Map.Entry<String, Set<String>> e : src.entrySet()) {
            out.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return out;
    }
}
