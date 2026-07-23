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
     --freq /path/google-10000-english.txt --limit 5000 \
     --uri bolt://localhost:7687 --user neo4j --pass password \
     /path/kaikki-verb.jsonl /path/kaikki-noun.jsonl
```

- `--freq` — частотник (одно слово в строке, lowercase, по убыванию частоты);
- `--limit` — сколько верхних лемм оставить (топ-N);
- `--uri / --user / --pass` — доступ к Neo4j (bolt);
- дальше — один или несколько kaikki-JSONL файлов (позиционные аргументы).

Прогресс печатается в `stderr`.

## Идемпотентность

Загрузка идемпотентна (`CREATE CONSTRAINT ... IF NOT EXISTS` + `MERGE` батчами через
`UNWIND`): тул можно гонять повторно — дублей не будет, догрузятся только новые данные.
