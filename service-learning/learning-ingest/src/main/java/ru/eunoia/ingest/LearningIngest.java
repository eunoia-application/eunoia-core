package ru.eunoia.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public static void main(String[] args) throws Exception {
        Args a = Args.parse(args);
        if (a == null) {
            usage();
            System.exit(1);
            return;
        }

        // 1. Частотник → топ-N лемм (по нему решаем, какие статьи брать).
        FrequencyList freq = FrequencyList.load(a.freq(), a.limit());
        err("Частотник: топ-%d, уникальных лемм %d.", a.limit(), freq.size());

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
                    // только топ-N по частоте
                    String lemma = entry.path("word").asText("").toLowerCase();
                    Integer rank = lemma.isEmpty() ? null : freq.rankOf(lemma);
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
        err("Парсинг: строк %d, отобрано записей %d, уникальных лексем %d.",
                linesRead, kept, acc.lexemes().size());

        // 3. Загрузка в Neo4j (идемпотентно).
        try (Neo4jLoader loader = new Neo4jLoader(a.uri(), a.user(), a.pass(), a.database())) {
            loader.load(acc);
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
                       [--database <name>] \\
                       <kaikki-1.jsonl> [<kaikki-2.jsonl> ...]

                Обязательны все флаги, кроме --database (по умолчанию neo4j), и хотя бы
                один kaikki-файл (позиционные аргументы).
                """);
    }

    /** Разобранные аргументы: флаги + позиционные пути kaikki-файлов. */
    private record Args(Path freq, int limit, String uri, String user, String pass,
                        String database, List<Path> kaikki) {

        static Args parse(String[] argv) {
            Path freq = null;
            Integer limit = null;
            String uri = null;
            String user = null;
            String pass = null;
            String database = "neo4j";   // база по умолчанию; переопределяется --database
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
            return new Args(freq, limit, uri, user, pass, database, kaikki);
        }
    }
}
