# HANDOFF — где мы и что дальше

> Живая записка для продолжения в новой сессии, чтобы не потерять нить.
> Полная история вех — в [ROADMAP.md](ROADMAP.md). Здесь — только «здесь и сейчас».
> Ветка: `feature/service-learning`.

## TL;DR

**M4 закрыт; идёт M5 (сад с FSRS) — Ф0 готова, сборка и тесты зелёные.** Учебное ядро работает
end-to-end (граф языка в Neo4j × «сад» в Postgres × REST): word-модель, блоки топ-слов, точные
темы (три слоя: сид `word-topics.tsv` + категории). Начали **M5** — дерево знаний как лицо сада:
**Ф0** дала снапшот `GET /learning/tree` (словарь+ветки+активность одним вызовом) и **журнал
занятий** (пишется на каждое действие — фундамент под погоду/сезоны/стрики). Контракт — **3.2.0**.
Полный план вех — в **[M5-PLAN.md](M5-PLAN.md)**.

## 🟢 Что готово (последний заход)

- **Импорт `learning-ingest` переработан:** дедуп по лемме (словоформы `went/goes` не отдельные
  слова, частота свёрнута), **второй источник переводов WikDict** (TSV, `--dict`; fallback на лемму +
  чистка wiki-разметки `[[...]]`), **темы из kaikki-категорий** (курируемый allow-list `seed/topics.tsv`,
  primary-sense чтобы меньше шума), **грамматика** (`seed/grammar.tsv` по Core Inventory + маркер-слова +
  авто-`ILLUSTRATES` из неправильных форм), **IPA** (`sounds[].ipa`), отсев alt-форм/мультислов/аббревиатур.
  Темы и грамматику теперь **строит сам тул** из `seed/*.tsv` — ручной `cypher-shell` убран,
  `topics-grammar.cypher` удалён.
- **Word-модель (контракт 3.0.0, ломающий):** карточка = **лемма** с `variants[]` (по частям речи:
  переводы/формы/связи); мастерство **по лемме** (`en:go`); `WordCard`/`WordVariant`/`WordRef`/`WordLeaf`/
  `WordPage`; `GET /learning/words/{id}` (было `/lexemes`), `/search` → `WordRef`. `RelationType` удалён.
- **Блоки топ-слов (контракт 3.1.0, additive):** `GET /learning/bands` — уровни (топ-100/…/5001–10000,
  эксклюзивные, по рангу частоты) с прогрессом (`total/known/learning`); `GET /learning/words?band=`
  (слова блока) с `WordLeaf.topics` (разбивка по категориям, пусто → «Разное»); `GET /learning/study`
  (очередь «Учить» = мои `LEARNING`). Мастерство на фронте — **2 кнопки Знаю/Учить** (KNOWN/LEARNING;
  не отмечено = UNKNOWN); 3-статусная модель остаётся под M5/FSRS.

## 🔴 Прямо сейчас — что дальше

**Идёт M5 (полный план — [M5-PLAN.md](M5-PLAN.md)). Ф0 закрыта, следующая — Ф1.**

1. **✅ Ф0 (снапшот дерева + журнал занятий) — сборка/тесты зелёные, контракт 3.2.0.**
   - `GET /learning/tree` — агрегат для дерева одним вызовом: `vocabulary` (листья: known/learning/
     total), `topics[]` (рост ветвей: прогресс по ветке), `activity` (погода/сезоны: streak/
     lastActiveDate/daysActive30). Джойн Neo4j × Postgres в фасаде; числа непрерывные — стадии/высоту/
     цвет считает фронт.
   - **Журнал занятий** `activity_log(userId, day, actions)` — пишется на КАЖДОЕ действие мастерства
     (атомарный upsert; `MasteryUseCaseImpl` → `ActivityUseCase.record`). Историю копим уже сейчас,
     чтобы к погоде/сезонам была не пустая.
   - **Фронту:** дерево рисовать из одного `/learning/tree`; листья — ПО СЛОВУ (лист/слово), а не по
     агрегату (это чинит «ствол без листьев»); птицы/золото — клиентские пороги на реальных числах.
2. **▶ Ф1 (следующая) — прогресс грамматики → высота дерева.** Мастерство на правилах (те же 2 кнопки
   Знаю/Учу, `nodeKind=GRAMMAR`); `GET /learning/grammar` + статус per-user, `PUT …/grammar/mastery/{id}`,
   грамматика в снапшоте. Контракт → 3.3.0.
3. **▶ Ф2 — ядро FSRS** (retrievability→увядание/цвет/опадание, очередь повторений→плотность кроны,
   стрики/сезоны из журнала). Решения зафиксированы: полный FSRS-5, 2 кнопки повторения (Забыл/Помню),
   одна `review_state`+`nodeKind`. Контракт → 4.0.0.

**Данные:** граф актуален (ре-импорт с `--stopwords/--word-topics` сделан; ~5863 слова, блоки ровные,
темы точные). Мелочь на потом: пара шумных синонимов из Викисловаря (`time`↔`bird` = тюремный сленг).

**Дальше по вехам:** M6 — tutor/AI-садовник, M7 — харднинг.

## ⚙️ Локальный запуск (важные детали)

- **База Neo4j — `neo4j` (дефолт), НЕ `eunoia`.** Владелец переехал на дефолтную базу (проще, убирает
  гочу «не та база»). Сервис без `NEO4J_DATABASE` берёт `neo4j` (`.env.example` привести к `neo4j`).
- Инфра: Postgres — локально (`auth_db`/`user_db`/`learning_db`); Kafka + Neo4j — Desktop/compose.
- Порядок: `eureka` → `service-auth` → `service-user` → `service-learning` → `api-gateway`.
- Порты: eureka 9999, auth 8081, user 8082, learning 8083, gateway 7777, фронт 9000.
- Фронт ходит на gateway `http://localhost:7777/api/v1`; `/learning/*` — под токеном.

## 📏 Правила работы (не забыть)

- **Сборку и импорт запускает владелец** (доступ к Nexus; песочница офлайн). **Не `mvn clean`,
  не читать `~/.m2/settings.xml`.** Claude не гоняет `mvn` сам — правит код, владелец собирает.
- **OAS-репо** (`../eunoia-oas/eunoia-oas`) — Claude правит, но **не коммитит**: готовые commit-message
  (Conventional Commits, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`). Контракты lockstep
  `<revision>` (правка одного = передеплой всех). Опубликован/целевой — **3.1.0**.
- Доки и комментарии — **по-русски**, просто, «что/зачем». **HANDOFF/ROADMAP держать актуальными.**
- Покрытие тестами — **100%** (JaCoCo), как в auth/user.

## 📌 Ключевые точки архитектуры

- **Два графа:** канон — Neo4j (узлы `Lexeme` id `en:go:VERB`, `Form`/`Translation`/`Topic`/`Grammar`;
  рёбра `HAS_FORM`/`TRANSLATION`/`SYNONYM`/`ANTONYM`/`HYPERNYM`/`IN_TOPIC`/`SUBTOPIC`/`ILLUSTRATES`/`PREREQUISITE`).
  Оверлей — Postgres `mastery(userId, lexemeId, status)`, где `lexemeId` теперь **ключ леммы** `en:go`.
- **Word = лемма:** адаптер склеивает все POS-узлы одной леммы (`id STARTS WITH "en:go:"`) в `Word` с
  `variants`; списки (поиск/тема/блок/все) — distinct по лемме через **Neo4jClient** (агрегаты min/collect).
  Блоки — по диапазону рангов; прогресс блока = джойн ранга (Neo4j) × статуса (Postgres) в фасаде.
- **`learning-ingest`** — standalone Maven-модуль (НЕ в aggregator): `mvn -f service-learning/learning-ingest/pom.xml package`;
  дампы kaikki + `en-ru.sqlite3` у владельца в `~/Documents/data`; курируемые `seed/topics.tsv` + `seed/grammar.tsv`.
- **Гочи Boot 4** (повторяются): автоконфиги по пакетам (kafka/neo4j/data-neo4j), тест-срезы
  `@DataJpaTest`/`@DataNeo4jTest` в отдельных модулях (собираем срез вручную), `mockito-junit-jupiter`
  отдельно, shade `combine.self=override`, Neo4j lowercase'ит имя базы. Модулит с двумя Spring Data — оба
  tx-менеджера объявлены явно в `TransactionConfig` (JPA `@Primary`, Neo4j с `DatabaseSelectionProvider`).
