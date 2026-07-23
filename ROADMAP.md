# Eunoia — план и решения

Живой документ: куда идём, что решили, что сделано. Держим в актуальном виде по ходу работы.

## Продукт
Knowledge Garden — пользователь «выращивает» граф своих знаний. Первая вертикаль — языки (английский).
Ключевой принцип — **два разных графа**, которые нельзя смешивать:
- **канонический граф знаний** — общий контент (слова, грамматика, связи `go→went`, `postpone↔delay`);
- **персональный оверлей мастерства** — что человек знает, SRS-«увядание» (цвет листа = due-to-review).

Метафора сада = реальная механика: лист = слово, цвет = retrievability (FSRS), ветка = тема,
ствол = грамматика, плод = достижение. AI-«садовник» целится в конкретный пробел.

## Архитектурные решения
- Java 25 + Spring Boot 4.1 + Spring Cloud 2025.1.2 (Oakwood).
- **Гексагоналка, границы enforced компилятором**: ядро (`*-logic`) без web/jpa/spring; инфра — в `*-app`;
  boot-plugin только в app-модуле; родительский pom без `<dependencies>` (только управление версиями).
- **Identity в service-auth (Design B)**: auth владеет учётками/паролями/токенами; service-user — только профиль.
- **RS256 + JWKS**: auth подписывает токены приватным RSA-ключом, публичный отдаёт по `/.well-known/jwks.json`;
  gateway и сам auth валидируют по нему.
- **Contract-first**: OpenAPI из GitHub Packages (`com.eunoia.application:shared-contract` + `auth-contract:1.0.0`),
  генерация в `auth-api` (пакеты `com.eunoia.application.auth.*`).
- Учебное ядро (knowledge + garden + tutor) — **модулит** (не отдельные микросервисы); граф знаний — **Neo4j**.

## Милстоуны
- **M0 — фундамент** ✅ Boot 4.1/Java 25, границы модулей, records, identity Design B.
- **M1 — auth работает** ✅ register/login end-to-end, RS256+JWKS, Liquibase, Testcontainers-тест.
- **M2 — доводка auth** ✅ (собрано зелёным):
  - ✅ refresh с ротацией (храним хеш токена в `refresh_tokens`, старый гасим при обновлении)
  - ✅ logout + auth как resource-server (валидирует свои токены)
  - ✅ enforce локаута — 5 неудачных логинов → блок на 15 мин (конфиг `auth.lockout.*`)
  - ✅ аудит — `auth_events` + `AuthEventRecorder` пишет REGISTRATION_SUCCESS, LOGIN_SUCCESS/FAILED, TOKEN_REFRESH, LOGOUT
  - ✅ security ужат — публичны только auth/JWKS/health/`/error`, остальное под токеном
  - ✅ gateway-интеграция — `/api/v1/auth/**`→`lb://SERVICE-AUTH` (срезаем только версию), CORS для фронта, имена выровнены; фронт ходит на базу `http://localhost:7777/api/v1`, origin `:9000`
  - ✅ email-флоу — verify-email + forgot/reset: одноразовые токены (`one_time_tokens`, унифицированы как `OneTimeToken`), `EmailSenderPort` (лог-заглушка, SMTP позже); при регистрации шлём письмо подтверждения
- **Полировка auth** ✅ (перед M3): удалён мёртвый код (16 файлов + распухшие порты `AuthEventRepositoryPort`/`TokenProviderPort`/`UserRepositoryPort` + битая OIDC-заглушка); **100% покрытие тестами** (JaCoCo — юниты в `auth-logic`/`auth-api`, интеграция на Testcontainers в `auth-app`); комментарии причёсаны на русский.
- **M3 — service-user** ✅ — профиль пользователя + событийная модель на Kafka (Design B). Полная сборка + CI зелёные.
  **Контракты (OAS 2.0.0, ОПУБЛИКОВАНЫ):** user-contract (профиль: get/update, аватар upload/delete, settings, экспорт, публичный `GET /users/{id}`); auth-contract ужат до identity (`UserProfile`→`AuthUser`, `+DELETE /auth/account`); notes/tags удалены; npm api-types починен (bundle auth+user).
  **eunoia-core сделано:**
  - ✅ версия контрактов централизована в корневом pom: `<eunoia-oas.version>2.0.0</eunoia-oas.version>` + depMgmt shared/auth/user
  - ✅ `service-note` удалён (рудимент: pom-модуль + gateway-роут `/api/v1/note`)
  - ✅ auth на 2.0.0: `AuthApiMapper` → `AuthUser`; `deleteAccount` реализован — `DeleteAccountUseCase` (revoke refresh-токенов + удаление identity + аудит `ACCOUNT_DELETED` + publish `UserDeleted`), `DELETE /auth/account` под токеном → 204. Каскад Design B замкнут в обе стороны (register→профиль, delete→удаление профиля)
  - ✅ `service-user` скелет: 3 модуля (`user-logic`/`user-api`/`user-app`), user = resource-server (jwk-set-uri на auth, без jjwt), +spring-kafka, :8082, `SecurityConfig` (публичны `/error`/health + `GET /users/{id}`; `/users/me/**` под токеном), пустой changelog
  - ✅ `user-logic` ядро: домен `Profile`/`ProfileSettings`(+enums) + `ProfileUseCase`/`ProfileLifecycleUseCase` (идемпотентные) + `ProfileRepositoryPort` + исключения (`ProfileNotFound`/`ProfilePrivate`)
  - ✅ Kafka-продюсер (auth): `DomainEvent`(sealed)/`UserRegistered`/`UserDeleted` + `EventPublisherPort` + `KafkaEventPublisherAdapter` (топики `user.registered`/`user.deleted`, ключ=userId, JSON); `RegisterUseCaseImpl` публикует `UserRegistered`; +spring-kafka + продюсер-конфиг; тесты обновлены (no-op publisher в интеграции, мок в юните)
  - ✅ **Kafka-блокер решён:** `spring-kafka` → `spring-boot-kafka` в обоих app-pom (Boot 4 держит автоконфиг в отдельном модуле; сырой spring-kafka даёт только классы → не было бина `KafkaTemplate`). Сборка зелёная.
  - ✅ **user-консюмер + персистентность:** `UserEventsListener` (@KafkaListener `user.registered`/`user.deleted` → `ProfileLifecycleUseCase`; value как String + `StringJsonMessageConverter` разбирает по типу метода — один консюмер, разные события); `profiles` changeset + `ProfileEntity`/JpaRepo/`ProfileRepositoryAdapter` (userId присвоенный, без `@GeneratedValue`); `UseCaseConfig` (wiring). Флоу register→`UserRegistered`→создание профиля замкнут.
  - ✅ **user-api REST-слой:** `UserController` (реализует `UsersApi`: get/update/settings/export/публичный `GET /users/{id}`; `uploadAvatar` — заглушка 501, пока аватар задаётся URL'ом через update) + `UserApiMapper` (домен↔DTO, enum по имени) + `CurrentUser`/`SecurityContextCurrentUser` (userId из JWT-claim) + `UserExceptionHandler` (`ProfileNotFound`→404, `ProfilePrivate`→403).
  - ✅ **тесты + CI:** user-юниты по трём модулям (logic: домен + use case'ы; api: маппер/контроллер/handler; app: адаптер/листенер/current-user) + интеграция `ProfileEventFlowTest` (Testcontainers Postgres+Kafka: событие→профиль→REST→публичность→удаление; JWT подменён тестовым декодером bearer=userId); auth-покрытие возвращено (`DeleteAccountUseCaseImplTest`, `KafkaEventPublisherAdapterTest`, +delete в контроллер-тесте); `ci-service-user.yml` (per-module, path-фильтр).
  **M3 закрыт** — полная сборка `mvn -fae install` и per-module CI зелёные.
  - 🔧 **follow-up: загрузка аватара (bytea)** (ждёт публикации OAS 2.1.0 + пересборку): контракт → **2.1.0** (добавлен `GET /users/{userId}/avatar`, binary; `<eunoia-oas.version>` в корневом pom → 2.1.0); `AvatarStoragePort` (out-port — миграция на MinIO/S3 = новый адаптер, ядро не трогаем) + таблица `avatars` (Postgres **bytea**, отдельная от profiles); `uploadAvatar` реальный (валидация тип png/jpg/webp, multipart-лимит 2MB), `GET /users/{id}/avatar` — публичный (permitAll); заглушка 501 убрана. `avatarUrl` загруженного файла = `/users/{id}/avatar` (относительно API-базы). Тесты — 100% (юниты + расширен `ProfileEventFlowTest`).
  Гоча для user: `eureka.instance.hostname: localhost` (✅ в yml), `/error` в permitAll (✅), id при `@GeneratedValue` руками не ставить.
- **M4 — service-learning (модулит)** 🔜 (проектируется) — учебное ядро: канонический граф языка (Neo4j) + тонкий персональный оверлей «сад» (Postgres). Первый бандед-контекст модулита; garden(полный)/tutor дорастают модулями/вехами.
  **Решения:** модулит `service-learning` (контексты `knowledge`/`garden` как пакеты; hexagonal `learning-logic`/`-api`/`-app`); два хранилища — Neo4j (канон) + Postgres `learning_db` (оверлей), Spring Data со скоупом по пакетам (`@EnableNeo4jRepositories` на knowledge, `@EnableJpaRepositories` на garden); данные — **реальный импорт** (OEWN + Wiktionary/kaikki + частотный список); единица обучения — `Lexeme` (лемма+POS), `Sense` позже; статус мастерства M4 — `KNOWN/LEARNING/UNKNOWN` (FSRS — M5); чтение канона и запись мастерства — под токеном; Kafka в M4 нет.
  **Принцип двух графов:** `garden.mastery(userId, lexemeId, status)` ссылается на `knowledge.Lexeme.id` по id, не встраивая; garden-view джойнит структуру (Neo4j) × статус (Postgres) в приложении.
  **Модель графа (Neo4j):** узлы `Lexeme`/`Form`/`Translation`/`Topic`/`Grammar`/`Example` (+`Sense` позже); связи `HAS_FORM`, `TRANSLATION`, `SYNONYM`, `ANTONYM`, `HYPERNYM`, `IN_TOPIC`, `ILLUSTRATES`, `PREREQUISITE`, `SUBTOPIC`, `USES`. Стабильный id у `Lexeme`/`Grammar`.
  **Задачи:** M4.1 скелет модулита (Neo4j+Postgres конфиг, docker-compose +Neo4j, gateway-роут `/api/v1/learning/**`) → M4.2 knowledge-домен + SDN-адаптер + neo4j-migrations → M4.3 импорт (OEWN+kaikki+частотник → bulk-load, идемпотентно; **дампы качает и прогон делает владелец**, песочница офлайн) → M4.4 garden-контекст (таблица `mastery`, POST мастерства) → M4.5 контракт `learning-api.yaml` + read + **garden-view** → M4.6 тесты (Testcontainers Neo4j+Postgres) + `ci-service-learning.yml`.
  **Сделано:** ✅ M4.1 — скелет модулита `service-learning` (3 модуля, порт 8083), Neo4j+Postgres со скоупом репозиториев (`@EnableNeo4jRepositories`/`@EnableJpaRepositories`), resource-server, docker-compose +Neo4j, gateway-роут `/api/v1/learning/**`; сборка зелёная.
  ✅ M4.2 — knowledge-контекст: домен графа (`Lexeme`/`Form`/`Translation`/`LexemeRef`/`Topic`/`Grammar` + enum'ы) + порт `LexiconRepositoryPort`; SDN-персистентность (node'ы, репозитории с Cypher, `LexiconRepositoryAdapter`); `KnowledgeSchemaInitializer` (констрейнты+индекс на старте через `Neo4jClient`, без neo4j-migrations — драйвер 6.x свежий); смоук-тест на Testcontainers Neo4j 5. Гоча: в Boot 4 Neo4j-автоконфиги тоже в отдельных пакетах (`org.springframework.boot.neo4j.autoconfigure`, `...data.neo4j.autoconfigure`), а `@DataNeo4jTest` уехал в `spring-boot-data-neo4j-test` → в тесте собран эквивалентный срез вручную. Зелёное.
  ✅ M4.3 — импорт: standalone-тул `learning-ingest` (частотник + kaikki-JSONL Wiktionary → топ-N лемм → `Lexeme`+формы/переводы/связи; батчевый `MERGE`, идемпотентно, флаг `--database`). Граф залит в Neo4j Desktop (база `eunoia`, версия 2026.06). Гочи: Neo4j lowercase'ит имя базы (`Eunoia`→`eunoia`); `spring-boot-parent` конфигурит shade в pluginManagement → в pom тула нужен `combine.self=override` (иначе `<resource>` из родительского `AppendingTransformer` бьётся с нашим `ManifestResourceTransformer`).
  **Риск:** M4.3 импорт — самая тяжёлая часть (источники/лицензии/маппинг/объём); ограничиваем частотным списком (топ ~N лемм). Возможно вынести импорт в отдельный тул, сервис только читает.
- **M5 — сад (полный)** ⬜ FSRS-retrievability вместо статуса: «увядание» листьев, цвет = вероятность вспомнить, планировщик повторений.
- **M6 — tutor (AI-садовник)** ⬜ находит слабое/увядающее место в персональном графе и предлагает, что подтянуть.
- **M7 — харднинг** ⬜ load-balanced JWKS через Eureka (сейчас прямой адрес auth); аудит best-effort; проверка типа токена.

## Известные хвосты
- **Локальная регистрация в Eureka = `localhost`**: без `eureka.instance.hostname` сервис регает LAN-IP (10.x),
  и gateway (`lb://SERVICE-X`) до него не достукивается. Настроено у auth и user; для новых сервисов — не забыть.
- **Аудит не best-effort**: сбой записи в `auth_events` роняет флоу в 500. Удобно для проверки сейчас, но в M5
  сделать запись необязательной — логин/регистрация не должны падать из-за аудита.
- **Ключ RS256 генерится на старте** (single-instance dev). Для деплоя — стабильный ключ из конфига/keystore.
- **Liquibase 5.0.3** на Postgres — при регрессе `CatalogSnapshotGenerator` запинить `4.31.1`.
- **Тип токена (access/refresh)** resource-server не проверяет при валидации — при желании добавить.
- **`ddl-auto=none`** — схемой владеет Liquibase; при добавлении сущностей не забывать changeSet.
- **Аватары в Postgres `bytea`** — для пета ок; при росте (CDN, размер) → MinIO/S3. Порт `AvatarStoragePort` уже готов, миграция = только новый адаптер в user-app.
- **Lockstep-версии контрактов** (бэклог): один `<revision>` на shared/auth/user → правка одного контракта требует передеплоя всех трёх на новую версию (ловили на 2.1.0 — auth/shared не были опубликованы). Терпимо для пета; если надоест — перейти на независимые версии по контракту.
- **Gateway-роут `from` = ресурсный сегмент пути сервиса** (не имя сервиса): было `api/v1/user`, а user отдаёт `/users/**` → не роутилось (у auth совпало случайно: `auth`=`/auth`). Исправлено на `api/v1/users`. Не всплывало раньше, т.к. профиль создаётся Kafka-событием, а не HTTP; проверено курлом `GET /api/v1/users/me` → 200. Gateway валидирует JWT только на не-`/auth/**` путях, а `anyExchange().authenticated()` срабатывает ДО роутинга (плохой токен→401, плохой роут→404).

## Как здесь работать
- Сборку запускает владелец (доступ к Nexus): `mvn -fae install` (с тестами, нужен Docker). `mvn clean` — нельзя.
- Покрытие: JaCoCo подключён в корневом pom, отчёт — `<модуль>/target/site/jacoco/index.html` после `mvn verify`/`install`.
- CI: GitHub Actions, **per-module** — `.github/workflows/ci-<модуль>.yml` (path-фильтр: гоняется только затронутый модуль либо при правке корневого `pom.xml`) поверх общего `_build.yml` (`mvn -pl <module> -am verify` + JaCoCo). Раннер тянет Central + публичные GitHub Packages (`GITHUB_TOKEN`), офлайн-хак не нужен. Для service-user/note — добавить свои `ci-*.yml`, когда появится код.
- Комментарии и доки — по-русски, просто, «что/зачем/для чего», без пошаговых портянок.
