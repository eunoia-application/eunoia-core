// Курируемый сид педагогической структуры: темы (ветки сада) и грамматика (ствол).
// В словарных дампах этого нет — kaikki даёт только слова/формы/переводы/связи,
// а темы и порядок изучения грамматики мы задаём сами.
//
// Идемпотентно: узлы через MERGE, связи со словами — через MATCH, поэтому если слова
// нет в графе (не попало в топ-N частотника), связь просто не создастся, без ошибки.
//
// Запуск через cypher-shell:
//   cypher-shell -a bolt://127.0.0.1:7687 -u neo4j -p <пароль> -d eunoia -f topics-grammar.cypher
//
// В новом Neo4j Desktop терминала нет, но cypher-shell лежит внутри папки самой БД
// (путь к DBMS виден в карточке инстанса в Desktop):
//   "<путь к DBMS>/bin/cypher-shell"
// напр.: ~/Library/Application Support/neo4j-desktop/Application/Data/dbmss/dbms-<uuid>/bin/cypher-shell
//
// Альтернатива — скопировать содержимое в Neo4j Browser, выполнив сначала  :use eunoia
// (Browser выполняет команды по одной, так что придётся прогонять блоками).

// ─────────────────────────────── 1. Темы (ветки) ───────────────────────────────
MERGE (t:Topic {id:'everyday'})  SET t.name='Повседневность',       t.slug='everyday';
MERGE (t:Topic {id:'food'})      SET t.name='Еда',                  t.slug='food';
MERGE (t:Topic {id:'people'})    SET t.name='Люди и общение',       t.slug='people';
MERGE (t:Topic {id:'movement'})  SET t.name='Движение',             t.slug='movement';
MERGE (t:Topic {id:'learning'})  SET t.name='Учёба и работа',       t.slug='learning';

// Иерархия: «Еда» — подтема «Повседневности». Корни (без входящего SUBTOPIC) отдаёт GET /learning/topics.
MATCH (p:Topic {id:'everyday'}), (c:Topic {id:'food'})
MERGE (p)-[:SUBTOPIC]->(c);

// ──────────────────────── 2. Слова в темы (листья на ветках) ────────────────────────
UNWIND [
  // Повседневность
  {topic:'everyday', lemma:'time',   pos:'NOUN'},
  {topic:'everyday', lemma:'day',    pos:'NOUN'},
  {topic:'everyday', lemma:'water',  pos:'NOUN'},
  {topic:'everyday', lemma:'house',  pos:'NOUN'},
  {topic:'everyday', lemma:'home',   pos:'NOUN'},
  {topic:'everyday', lemma:'money',  pos:'NOUN'},
  {topic:'everyday', lemma:'people', pos:'NOUN'},
  {topic:'everyday', lemma:'thing',  pos:'NOUN'},
  {topic:'everyday', lemma:'life',   pos:'NOUN'},
  {topic:'everyday', lemma:'night',  pos:'NOUN'},
  {topic:'everyday', lemma:'week',   pos:'NOUN'},
  {topic:'everyday', lemma:'year',   pos:'NOUN'},

  // Еда
  {topic:'food', lemma:'food',   pos:'NOUN'},
  {topic:'food', lemma:'bread',  pos:'NOUN'},
  {topic:'food', lemma:'meat',   pos:'NOUN'},
  {topic:'food', lemma:'fruit',  pos:'NOUN'},
  {topic:'food', lemma:'milk',   pos:'NOUN'},
  {topic:'food', lemma:'coffee', pos:'NOUN'},
  {topic:'food', lemma:'tea',    pos:'NOUN'},
  {topic:'food', lemma:'rice',   pos:'NOUN'},
  {topic:'food', lemma:'fish',   pos:'NOUN'},
  {topic:'food', lemma:'egg',    pos:'NOUN'},
  {topic:'food', lemma:'eat',    pos:'VERB'},
  {topic:'food', lemma:'drink',  pos:'VERB'},
  {topic:'food', lemma:'cook',   pos:'VERB'},

  // Люди и общение
  {topic:'people', lemma:'friend', pos:'NOUN'},
  {topic:'people', lemma:'family', pos:'NOUN'},
  {topic:'people', lemma:'mother', pos:'NOUN'},
  {topic:'people', lemma:'father', pos:'NOUN'},
  {topic:'people', lemma:'child',  pos:'NOUN'},
  {topic:'people', lemma:'man',    pos:'NOUN'},
  {topic:'people', lemma:'woman',  pos:'NOUN'},
  {topic:'people', lemma:'name',   pos:'NOUN'},
  {topic:'people', lemma:'say',    pos:'VERB'},
  {topic:'people', lemma:'tell',   pos:'VERB'},
  {topic:'people', lemma:'ask',    pos:'VERB'},
  {topic:'people', lemma:'talk',   pos:'VERB'},

  // Движение
  {topic:'movement', lemma:'go',     pos:'VERB'},
  {topic:'movement', lemma:'come',   pos:'VERB'},
  {topic:'movement', lemma:'run',    pos:'VERB'},
  {topic:'movement', lemma:'walk',   pos:'VERB'},
  {topic:'movement', lemma:'move',   pos:'VERB'},
  {topic:'movement', lemma:'drive',  pos:'VERB'},
  {topic:'movement', lemma:'leave',  pos:'VERB'},
  {topic:'movement', lemma:'return', pos:'VERB'},
  {topic:'movement', lemma:'road',   pos:'NOUN'},
  {topic:'movement', lemma:'car',    pos:'NOUN'},
  {topic:'movement', lemma:'city',   pos:'NOUN'},
  {topic:'movement', lemma:'street', pos:'NOUN'},

  // Учёба и работа
  {topic:'learning', lemma:'book',    pos:'NOUN'},
  {topic:'learning', lemma:'school',  pos:'NOUN'},
  {topic:'learning', lemma:'student', pos:'NOUN'},
  {topic:'learning', lemma:'teacher', pos:'NOUN'},
  {topic:'learning', lemma:'job',     pos:'NOUN'},
  {topic:'learning', lemma:'office',  pos:'NOUN'},
  {topic:'learning', lemma:'word',    pos:'NOUN'},
  {topic:'learning', lemma:'question',pos:'NOUN'},
  {topic:'learning', lemma:'learn',   pos:'VERB'},
  {topic:'learning', lemma:'teach',   pos:'VERB'},
  {topic:'learning', lemma:'write',   pos:'VERB'},
  {topic:'learning', lemma:'read',    pos:'VERB'}
] AS row
MATCH (t:Topic {id: row.topic})
MATCH (l:Lexeme {id: 'en:' + row.lemma + ':' + row.pos})
MERGE (l)-[:IN_TOPIC]->(t);

// ─────────────────────────── 3. Грамматика (ствол) ───────────────────────────
MERGE (g:Grammar {id:'present-simple'})     SET g.name='Present Simple',     g.cefr='A1';
MERGE (g:Grammar {id:'past-simple'})        SET g.name='Past Simple',        g.cefr='A1';
MERGE (g:Grammar {id:'present-continuous'}) SET g.name='Present Continuous', g.cefr='A1';
MERGE (g:Grammar {id:'articles'})           SET g.name='Articles (a/an/the)',g.cefr='A1';
MERGE (g:Grammar {id:'plurals'})            SET g.name='Plural nouns',       g.cefr='A1';
MERGE (g:Grammar {id:'present-perfect'})    SET g.name='Present Perfect',    g.cefr='B1';

// Порядок изучения: что перед чем.
MATCH (a:Grammar {id:'present-simple'}), (b:Grammar {id:'past-simple'})        MERGE (a)-[:PREREQUISITE]->(b);
MATCH (a:Grammar {id:'present-simple'}), (b:Grammar {id:'present-continuous'}) MERGE (a)-[:PREREQUISITE]->(b);
MATCH (a:Grammar {id:'past-simple'}),    (b:Grammar {id:'present-perfect'})    MERGE (a)-[:PREREQUISITE]->(b);
MATCH (a:Grammar {id:'plurals'}),        (b:Grammar {id:'articles'})           MERGE (a)-[:PREREQUISITE]->(b);

// Неправильные глаголы иллюстрируют Past Simple (у них в графе уже есть формы went/came/…).
UNWIND ['go','come','run','eat','drive','leave','write','read','say','tell','drink'] AS lemma
MATCH (g:Grammar {id:'past-simple'})
MATCH (l:Lexeme {id: 'en:' + lemma + ':VERB'})
MERGE (l)-[:ILLUSTRATES]->(g);
