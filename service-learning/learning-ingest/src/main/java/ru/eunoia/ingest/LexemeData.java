package ru.eunoia.ingest;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Накопленные данные одной лексемы (lemma + часть речи) перед загрузкой в граф.
 * Одно слово встречается в kaikki несколько раз (разные этимологии) — формы, переводы
 * и связи копим в множества, поэтому дедуп «из коробки»: Form/Translation сравниваются
 * по полям, targetId связей — строки. Порядок вставки сохраняем (LinkedHashSet) —
 * повторные прогоны дают стабильный результат.
 *
 * <p>id связей считаем сразу ("en:{targetLemma}:{POS}"), а вот проверку «а импортирован ли
 * такой target вообще» откладываем на этап загрузки рёбер — чтобы не плодить висячие связи.
 */
final class LexemeData {

    /** Стабильный бизнес-id узла: "en:" + lemma + ":" + имя enum части речи. */
    final String id;
    final String lemma;
    /** Имя enum части речи: VERB / NOUN / ADJECTIVE / ADVERB. */
    final String pos;
    final int freqRank;
    /** Уровень CEFR по бакету частоты: A1..C2. */
    final String cefr;

    final Set<Form> forms = new LinkedHashSet<>();
    final Set<Translation> translations = new LinkedHashSet<>();
    final Set<String> synonymIds = new LinkedHashSet<>();
    final Set<String> antonymIds = new LinkedHashSet<>();
    final Set<String> hypernymIds = new LinkedHashSet<>();

    LexemeData(String id, String lemma, String pos, int freqRank, String cefr) {
        this.id = id;
        this.lemma = lemma;
        this.pos = pos;
        this.freqRank = freqRank;
        this.cefr = cefr;
    }
}
