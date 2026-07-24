package ru.eunoia.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Оффлайн-тул наполнения графа Neo4j: частотник + kaikki-JSONL (Wiktionary) →
 * топ-N лемм → канонический граф (Lexeme + формы/переводы/связи). Идемпотентно.
 *
 * <p>Не сервис и не деплоится: сервис learning граф только ЧИТАЕТ, а пишет в него —
 * этот тул, руками, как разовая/повторяемая ops-задача.
 *
 * <p>Прогресс печатаем в stderr (stdout оставляем чистым).
 */
public final class LearningIngest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Максимум переводов на слово: kaikki (точный, в приоритете) + добор из WikDict. */
    private static final int TRANSLATION_CAP = 6;

    public static void main(String[] args) throws Exception {
        Args a = Args.parse(args);
        if (a == null) {
            usage();
            System.exit(1);
            return;
        }

        // 1. Частотник (весь, с рангами). Это список словоформ, а не лемм — отбор топ-N
        //    лемм делаем ниже, уже свернув частоту форм в лемму.
        FrequencyList freq = FrequencyList.load(a.freq());
        err("Частотник загружен: словоформ %d. Цель — топ-%d лемм.", freq.size(), a.limit());

        // 2. Стрим kaikki-файлов: фильтр (язык/часть речи/частота) → аккумулятор.
        LexemeAccumulator acc = new LexemeAccumulator();
        long linesRead = 0;
        long kept = 0;
        for (Path file : a.kaikki()) {
            err("Читаю %s ...", file);
            try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    linesRead++;
                    JsonNode entry = JSON.readTree(line);

                    // только английские статьи
                    if (!"en".equals(entry.path("lang_code").asText("en"))) {
                        continue;
                    }
                    // только 4 поддерживаемые части речи (иначе слово пропускаем)
                    String pos = posName(entry.path("pos").asText(""));
                    if (pos == null) {
                        continue;
                    }
                    // словоформы (went/goes/better) отдельным словом в граф не идут —
                    // они приедут как HAS_FORM у своей леммы
                    if (!LexemeAccumulator.isLemmaEntry(entry)) {
                        continue;
                    }
                    // многословные и аббревиатуры (a.m., anno domini, time of the month) —
                    // это не карточка слова, пропускаем
                    String lemma = entry.path("word").asText("").toLowerCase();
                    if (lemma.contains(" ") || lemma.contains(".")) {
                        continue;
                    }
                    // частота леммы = минимум рангов её форм (сворачиваем go/going/went/gone
                    // в одну лемму); нет ни одной формы в частотнике → лемма не частотная
                    Integer rank = lemma.isEmpty() ? null : aggregatedRank(entry, lemma, freq);
                    if (rank == null) {
                        continue;
                    }

                    acc.add(entry, pos, rank);
                    kept++;
                    if (linesRead % 100_000 == 0) {
                        err("  ... строк %d, отобрано %d", linesRead, kept);
                    }
                }
            }
        }
        err("Парсинг: строк %d, отобрано записей %d, кандидатов-лемм %d.",
                linesRead, kept, acc.lexemes().size());

        // 2b. Отбор топ-N самых частотных лемм (остальные — вместе с висячими рёбрами — прочь).
        acc.retainTop(a.limit());
        err("Оставили топ-%d лемм: %d.", a.limit(), acc.lexemes().size());

        // 2c. Добор переводов из второго источника (WikDict), если задан --dict.
        //     kaikki-переводы уже в лексемах и остаются в приоритете; WikDict закрывает дыры.
        WikDictTranslations dict = a.dict() == null
                ? WikDictTranslations.empty()
                : WikDictTranslations.load(a.dict());
        mergeTranslations(acc, dict);

        // 2d. Раскладка по темам: категории слова → ветка сада (если задан --topics).
        TopicCatalog topics = a.topics() == null
                ? TopicCatalog.empty()
                : TopicCatalog.load(a.topics());
        assignTopics(acc, topics);

        // 2e. Грамматика: скелет правил + связь ILLUSTRATES из неправильных форм (если --grammar).
        GrammarCatalog grammar = a.grammar() == null
                ? GrammarCatalog.empty()
                : GrammarCatalog.load(a.grammar());
        assignGrammar(acc, grammar);

        // 3. Загрузка в Neo4j (идемпотентно).
        try (Neo4jLoader loader = new Neo4jLoader(a.uri(), a.user(), a.pass(), a.database())) {
            loader.load(acc, topics, grammar);
        }
        err("Готово.");
    }

    /** kaikki pos → имя enum части речи; null = не поддерживается (слово пропускаем). */
    static String posName(String kaikkiPos) {
        return switch (kaikkiPos) {
            case "verb" -> "VERB";
            case "noun" -> "NOUN";
            case "adj" -> "ADJECTIVE";
            case "adv" -> "ADVERB";
            default -> null;
        };
    }

    /**
     * Свёрнутая частота леммы: минимальный ранг среди самой леммы и всех её словоформ.
     * Так {@code go}(123)/{@code going}(500)/{@code went}(1179)/... дают лемме её лучший ранг,
     * а не плодят отдельные «слова». null — если ни лемма, ни её формы в частотник не попали.
     */
    private static Integer aggregatedRank(JsonNode entry, String lemma, FrequencyList freq) {
        Integer best = freq.rankOf(lemma);
        for (String form : LexemeAccumulator.formTexts(entry)) {
            Integer r = freq.rankOf(form.toLowerCase());
            if (r != null && (best == null || r < best)) {
                best = r;
            }
        }
        return best;
    }

    /**
     * Добор переводов из второго источника: kaikki-переводы уже в лексеме и идут первыми
     * (они точнее — под конкретный смысл), WikDict дописываем следом, дедуп по тексту, и всё
     * подрезаем до {@link #TRANSLATION_CAP}. Пустой словарь → no-op (работает только kaikki).
     */
    private static void mergeTranslations(LexemeAccumulator acc, WikDictTranslations dict) {
        int withTranslation = 0;
        for (LexemeData d : acc.lexemes()) {
            for (String ru : dict.forLexeme(d.lemma, d.pos)) {
                if (d.translations.size() >= TRANSLATION_CAP) {
                    break;
                }
                d.translations.add(new Translation(ru, "ru"));   // Set дедупит по (text, lang)
            }
            retainFirst(d.translations, TRANSLATION_CAP);
            if (!d.translations.isEmpty()) {
                withTranslation++;
            }
        }
        err("Переводы: лемм со словом-переводом %d/%d (второй источник: %d записей).",
                withTranslation, acc.lexemes().size(), dict.size());
    }

    /**
     * Раскладка слов по веткам сада: каждой лемме проставляем ветку по её kaikki-категориям
     * (первая подошедшая по приоритету каталога). Часть слов темы не получит — это нормально
     * (абстрактная/общая лексика вне тем; она всё равно в графе и в поиске).
     */
    private static void assignTopics(LexemeAccumulator acc, TopicCatalog topics) {
        int withTopic = 0;
        for (LexemeData d : acc.lexemes()) {
            d.topicId = topics.resolve(d.categories);
            if (d.topicId != null) {
                withTopic++;
            }
        }
        err("Темы: слов с веткой %d/%d.", withTopic, acc.lexemes().size());
    }

    /**
     * Связь слов с грамматикой: каждой лемме проставляем правила, которые она иллюстрирует
     * своими неправильными формами (напр. go→went ⇒ Past Simple). Загрузка сделает ILLUSTRATES.
     */
    private static void assignGrammar(LexemeAccumulator acc, GrammarCatalog grammar) {
        int withGrammar = 0;
        for (LexemeData d : acc.lexemes()) {
            d.grammarIds.addAll(grammar.illustratedBy(d));
            if (!d.grammarIds.isEmpty()) {
                withGrammar++;
            }
        }
        err("Грамматика: слов с ILLUSTRATES %d/%d (правил в скелете %d).",
                withGrammar, acc.lexemes().size(), grammar.rules().size());
    }

    /** Обрезает множество до первых n элементов (порядок вставки сохраняется). */
    private static void retainFirst(Set<Translation> set, int n) {
        if (set.size() <= n) {
            return;
        }
        List<Translation> keep = new ArrayList<>(new ArrayList<>(set).subList(0, n));
        set.clear();
        set.addAll(keep);
    }

    private static void err(String fmt, Object... args) {
        System.err.println(String.format(fmt, args));
    }

    private static void usage() {
        System.err.println("""
                learning-ingest — наполнение графа Neo4j из частотника + kaikki-JSONL.

                Использование:
                  java -jar learning-ingest.jar \\
                       --freq <path/google-10000-english.txt> --limit <N> \\
                       --uri bolt://localhost:7687 --user neo4j --pass <pwd> \\
                       [--database <name>] [--dict <path/en-ru.tsv>] \\
                       [--topics <path/topics.tsv>] [--grammar <path/grammar.tsv>] \\
                       <kaikki-1.jsonl> [<kaikki-2.jsonl> ...]

                Обязательны все флаги, кроме --database (по умолчанию neo4j), --dict
                (второй источник переводов, WikDict TSV: word<TAB>POS<TAB>перевод|перевод),
                --topics (таксономия тем: id<TAB>name<TAB>slug<TAB>parent<TAB>категории)
                и --grammar (скелет грамматики: id<TAB>name<TAB>cefr<TAB>prereq),
                и хотя бы один kaikki-файл (позиционные аргументы).
                """);
    }

    /** Разобранные аргументы: флаги + позиционные пути kaikki-файлов. */
    private record Args(Path freq, int limit, String uri, String user, String pass,
                        String database, Path dict, Path topics, Path grammar, List<Path> kaikki) {

        static Args parse(String[] argv) {
            Path freq = null;
            Integer limit = null;
            String uri = null;
            String user = null;
            String pass = null;
            String database = "neo4j";   // база по умолчанию; переопределяется --database
            Path dict = null;            // второй источник переводов (WikDict TSV); опционально
            Path topics = null;          // таксономия тем (topics.tsv); опционально
            Path grammar = null;         // скелет грамматики (grammar.tsv); опционально
            List<Path> kaikki = new ArrayList<>();
            try {
                for (int i = 0; i < argv.length; i++) {
                    switch (argv[i]) {
                        case "--freq" -> freq = Path.of(argv[++i]);
                        case "--limit" -> limit = Integer.parseInt(argv[++i]);
                        case "--uri" -> uri = argv[++i];
                        case "--user" -> user = argv[++i];
                        case "--pass" -> pass = argv[++i];
                        case "--database" -> database = argv[++i];
                        case "--dict" -> dict = Path.of(argv[++i]);
                        case "--topics" -> topics = Path.of(argv[++i]);
                        case "--grammar" -> grammar = Path.of(argv[++i]);
                        default -> kaikki.add(Path.of(argv[i]));  // позиционные = kaikki-файлы
                    }
                }
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                return null;   // кривые аргументы → покажем usage
            }
            if (freq == null || limit == null || uri == null
                    || user == null || pass == null || kaikki.isEmpty()) {
                return null;
            }
            return new Args(freq, limit, uri, user, pass, database, dict, topics, grammar, kaikki);
        }
    }
}
