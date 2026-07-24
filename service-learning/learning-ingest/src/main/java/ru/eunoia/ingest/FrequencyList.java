package ru.eunoia.ingest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Частотный список google-10000-english: одно слово в строке (lowercase), отсортирован
 * по убыванию частоты. Ранг = номер непустой строки, 1-based (меньше ранг = выше частота).
 *
 * <p>Важно: это список <b>словоформ</b>, а не лемм — в нём отдельными строками лежат
 * {@code go / going / goes / went / gone}, {@code good / better / best} и т.п. Поэтому здесь
 * мы храним <b>весь</b> список с рангами и ничего не обрезаем: свёртку форм в лемму и отбор
 * топ-N лемм делает {@link LearningIngest} уже после разбора kaikki (иначе форма становится
 * отдельным «словом» — источник дублей).
 */
final class FrequencyList {

    private final Map<String, Integer> rankByWord;

    private FrequencyList(Map<String, Integer> rankByWord) {
        this.rankByWord = rankByWord;
    }

    /** Ранг словоформы, либо null — если её нет в частотнике. */
    Integer rankOf(String word) {
        return rankByWord.get(word);
    }

    /** Сколько словоформ загружено. */
    int size() {
        return rankByWord.size();
    }

    /**
     * Читает частотник целиком, нумеруя только непустые строки.
     * Первое вхождение слова выигрывает (меньший ранг = выше частота).
     */
    static FrequencyList load(Path path) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        int rank = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String word = line.trim().toLowerCase();
            if (word.isEmpty()) {
                continue;                 // пустые строки ранга не получают
            }
            rank++;
            map.putIfAbsent(word, rank);
        }
        return new FrequencyList(map);
    }
}
