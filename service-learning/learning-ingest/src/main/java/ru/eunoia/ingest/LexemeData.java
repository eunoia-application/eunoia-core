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
 * <p>freqRank — <b>минимальный</b> ранг среди всех форм леммы (свёрнутая частота): разные
 * этимологии дают разные формы, поэтому при мерже держим наименьший ранг ({@link #mergeRank}).
 * CEFR здесь не храним — он выводится из финального ранга уже на загрузке.
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
    /** Свёрнутый ранг частоты: минимум среди форм леммы (меньше = частотнее). */
    int freqRank;
    /** Транскрипция IPA (из kaikki sounds), предпочтительно американская. null = нет. */
    String ipa;

    final Set<Form> forms = new LinkedHashSet<>();
    final Set<Translation> translations = new LinkedHashSet<>();
    final Set<String> synonymIds = new LinkedHashSet<>();
    final Set<String> antonymIds = new LinkedHashSet<>();
    final Set<String> hypernymIds = new LinkedHashSet<>();

    /** kaikki-категории слова (из senses[].categories) — по ним каталог выберет ветку. */
    final Set<String> categories = new LinkedHashSet<>();
    /** id ветки сада (Topic); проставляется каталогом после отбора топ-N. null = без темы. */
    String topicId;
    /** id грамматических правил, которые слово иллюстрирует (по неправильным формам). */
    final Set<String> grammarIds = new LinkedHashSet<>();

    LexemeData(String id, String lemma, String pos, int freqRank) {
        this.id = id;
        this.lemma = lemma;
        this.pos = pos;
        this.freqRank = freqRank;
    }

    /** Мерж этимологии той же леммы: держим наименьший (самый частотный) ранг. */
    void mergeRank(int rank) {
        if (rank < freqRank) {
            this.freqRank = rank;
        }
    }
}
