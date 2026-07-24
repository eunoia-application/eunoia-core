# learning-ingest

Оффлайн-тул наполнения канонического графа Neo4j для сервиса `learning`.

## Что делает

Берёт частотный список английских слов и дампы Wiktionary в формате
[kaikki](https://kaikki.org/) (JSONL, по одному слову на строку), оставляет только
**топ-N самых частотных лемм** и грузит их в Neo4j как граф знаний:

- узлы `Lexeme {id, lemma, pos, lang, freqRank, cefr}` — слово (лемма + часть речи);
- `(Lexeme)-[:HAS_FORM]->(Form {text, feature})` — словоформы (принципиальные части, без таблиц спряжения);
- `(Lexeme)-[:TRANSLATION]->(Translation {text, lang})` — русские переводы;
- `(Lexeme)-[:SYNONYM|ANTONYM|HYPERNYM]->(Lexeme)` — связи между словами
  (только между реально импортированными — без висячих рёбер).

Схема узлов совпадает с той, что читает сервис `learning`, — id стабильный
(`en:{lemma}:{POS}`), CEFR проставляется по бакету частоты.

## Зачем отдельный тул

Сервис `learning` граф только **читает**. Запись — разовая/повторяемая ops-задача,
поэтому это отдельный тул, а не часть сервиса. Он **намеренно не прописан** в `<modules>`
и **не деплоится**; никакого Spring — голый `neo4j-java-driver` + Jackson.

## Как собрать

```bash
mvn -f service-learning/learning-ingest/pom.xml package
```

Получится runnable fat-jar: `service-learning/learning-ingest/target/learning-ingest.jar`.

## Как запустить

Перед запуском поднимите Neo4j:

```bash
docker compose up -d neo4j
```

Затем:

```bash
java -jar service-learning/learning-ingest/target/learning-ingest.jar \
     --freq ~/Documents/data/google-10000-english.txt --limit 5000 \
     --uri bolt://localhost:7687 --user neo4j --pass password --database eunoia \
     --dict ~/Documents/data/en-ru.tsv \
     --topics service-learning/learning-ingest/seed/topics.tsv \
     --grammar service-learning/learning-ingest/seed/grammar.tsv \
     ~/Documents/data/kaikki.org-dictionary-English-by-pos-verb.jsonl \
     ~/Documents/data/kaikki.org-dictionary-English-by-pos-noun.jsonl \
     ~/Documents/data/kaikki.org-dictionary-English-by-pos-adj.jsonl \
     ~/Documents/data/kaikki.org-dictionary-English-by-pos-adv.jsonl
```

- `--freq` — частотник (одно слово в строке, lowercase, по убыванию частоты);
- `--limit` — сколько верхних лемм оставить (топ-N; частота формы сворачивается в лемму);
- `--uri / --user / --pass / --database` — доступ к Neo4j (bolt) и имя базы;
- `--dict` — второй источник переводов EN→RU (WikDict TSV, см. ниже); опционально;
- `--topics` — таксономия тем `seed/topics.tsv` (ветки + маппинг категорий); опционально;
- `--grammar` — скелет грамматики `seed/grammar.tsv` (правила + порядок); опционально;
- дальше — один или несколько kaikki-JSONL файлов (позиционные аргументы).

Прогресс печатается в `stderr`.

## Уникальность слов (леммы, а не словоформы)

Частотник — это список **словоформ** (`go / going / went / gone`), а не лемм. Тул:

- пропускает записи-словоформы kaikki (form-of, напр. `went` = прош. от `go`) — они не
  становятся отдельным словом, а приезжают как `HAS_FORM` у своей леммы;
- частоту словоформы **сворачивает в лемму** (лемма получает лучший ранг среди своих форм);
- берёт топ-N уже по леммам.

Так в графе нет дублей вида `go / going / went` и мусорных «слов» без переводов.

## Темы и грамматика (строит сам тул)

Раньше темы и грамматику накатывали вручную через `cypher-shell` — теперь их строит тул
из курируемых файлов в `seed/`, в тот же прогон (ручной шаг убран):

- **Темы** (`--topics seed/topics.tsv`): ветки сада (узлы `Topic`) + маппинг
  `kaikki-категория → ветка`. Тул создаёт узлы, иерархию `SUBTOPIC` и раскладывает слова
  по веткам (`IN_TOPIC`) из их `senses[].categories`. Приоритет веток = порядок строк
  (бытовое выше технического).
- **Грамматика** (`--grammar seed/grammar.tsv`): скелет правил (узлы `Grammar`) + порядок
  изучения (`PREREQUISITE`). Связь со словами (`ILLUSTRATES`) тул выводит из неправильных
  форм: неправильный глагол → Past Simple, нестандартное мн. число → Plural nouns,
  `better/best` → Comparatives & superlatives.

Оба файла — обычный TSV, правятся руками без пересборки тула. Часть слов темы не получит
(абстрактная/общая лексика) — это нормально: они всё равно в графе и в поиске.

## Второй источник переводов (WikDict)

Английский Wiktionary даёт русский перевод не всем частотным словам. Дыры добираем из
[WikDict](https://www.wikdict.com/) (собран из dbnary — нескольких изданий Wiktionary),
файл `en-ru.sqlite3`. Тул читает не сам sqlite, а TSV-выгрузку (чтобы не тащить JDBC).
Выгрузку делаем один раз (часть речи достаём из `lexentry` вида `eng/go__Verb__1`):

```bash
sqlite3 -separator $'\t' ~/Documents/data/en-ru.sqlite3 \
"SELECT lower(written_rep),
        CASE WHEN lexentry LIKE '%__Verb__%'      THEN 'VERB'
             WHEN lexentry LIKE '%__Noun__%'      THEN 'NOUN'
             WHEN lexentry LIKE '%__Adjective__%' THEN 'ADJECTIVE'
             WHEN lexentry LIKE '%__Adverb__%'    THEN 'ADVERB' END,
        trans_list
 FROM translation
 WHERE lexentry LIKE '%__Verb__%' OR lexentry LIKE '%__Noun__%'
    OR lexentry LIKE '%__Adjective__%' OR lexentry LIKE '%__Adverb__%'
 ORDER BY lower(written_rep), CAST(score AS REAL) DESC;" > ~/Documents/data/en-ru.tsv
```

Затем добавляем `--dict ~/Documents/data/en-ru.tsv` к запуску. kaikki-переводы остаются в
приоритете (они точнее — под конкретный смысл), WikDict дописывается следом; на слово
оставляем не больше шести переводов.

## Идемпотентность

Загрузка идемпотентна (`CREATE CONSTRAINT ... IF NOT EXISTS` + `MERGE` батчами через
`UNWIND`): тул можно гонять повторно — дублей не будет, догрузятся только новые данные.
