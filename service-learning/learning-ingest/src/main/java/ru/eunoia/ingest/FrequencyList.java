package ru.eunoia.ingest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Частотный список google-10000-english: одно слово в строке (lowercase), отсортирован
 * по убыванию частоты. Ранг = номер непустой строки, 1-based. Оставляем только топ-N
 * (аргумент --limit) — по этому множеству и решаем, какие статьи kaikki вообще брать.
 */
final class FrequencyList {

    private final Map<String, Integer> rankByLemma;

    private FrequencyList(Map<String, Integer> rankByLemma) {
        this.rankByLemma = rankByLemma;
    }

    /** Ранг слова, либо null — если слово не входит в топ-N (значит, слово пропускаем). */
    Integer rankOf(String lemma) {
        return rankByLemma.get(lemma);
    }

    /** Сколько лемм реально попало в топ-N. */
    int size() {
        return rankByLemma.size();
    }

    /**
     * Читает частотник, нумеруя только непустые строки, и обрезает на limit.
     * Первое вхождение слова выигрывает (меньший ранг = выше частота).
     */
    static FrequencyList load(Path path, int limit) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        int rank = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String word = line.trim().toLowerCase();
            if (word.isEmpty()) {
                continue;                 // пустые строки ранга не получают
            }
            rank++;
            if (rank > limit) {
                break;                    // топ-N набран — дальше не читаем
            }
            map.putIfAbsent(word, rank);
        }
        return new FrequencyList(map);
    }
}
