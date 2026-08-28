package ru.eunoia.controllers;

import com.eunoia.application.learning.api.LearningApi;
import com.eunoia.application.learning.model.Band;
import com.eunoia.application.learning.model.GrammarView;
import com.eunoia.application.learning.model.MasteryRequest;
import com.eunoia.application.learning.model.MasteryView;
import com.eunoia.application.learning.model.TopicRef;
import com.eunoia.application.learning.model.TopicView;
import com.eunoia.application.learning.model.TreeSnapshot;
import com.eunoia.application.learning.model.WordCard;
import com.eunoia.application.learning.model.WordLeaf;
import com.eunoia.application.learning.model.WordPage;
import com.eunoia.application.learning.model.WordRef;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.eunoia.application.garden.port.in.GrammarMasteryUseCase;
import ru.eunoia.application.garden.port.in.MasteryUseCase;
import ru.eunoia.application.learning.domain.exception.NotFoundException;
import ru.eunoia.application.learning.port.in.LearningQueryUseCase;
import ru.eunoia.mappers.LearningApiMapper;
import ru.eunoia.security.CurrentUser;

/**
 * REST-адаптер учёбы: реализует сгенерированный из контракта {@code LearningApi}. Единица — СЛОВО
 * (лемма): карточка с частями речи внутри, мастерство по лемме. Тонкий слой: достаёт userId,
 * делегирует в use case'ы, мапит домен в DTO. Отсутствие узла графа → 404.
 */
@RestController
public class LearningController implements LearningApi {

    private final LearningQueryUseCase learningQuery;
    private final MasteryUseCase mastery;
    private final GrammarMasteryUseCase grammarMastery;
    private final CurrentUser currentUser;
    private final LearningApiMapper mapper;

    public LearningController(LearningQueryUseCase learningQuery, MasteryUseCase mastery,
                              GrammarMasteryUseCase grammarMastery, CurrentUser currentUser,
                              LearningApiMapper mapper) {
        this.learningQuery = learningQuery;
        this.mastery = mastery;
        this.grammarMastery = grammarMastery;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<TreeSnapshot> getTree() {
        return ResponseEntity.ok(mapper.toTreeSnapshot(learningQuery.treeSnapshot(currentUser.id())));
    }

    @Override
    public ResponseEntity<WordCard> getWord(String id) {
        return learningQuery.wordCard(currentUser.id(), id)
                .map(mapper::toWordCard)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Слово не найдено: " + id));
    }

    @Override
    public ResponseEntity<List<Band>> getBands() {
        return ResponseEntity.ok(mapper.toBands(learningQuery.bands(currentUser.id())));
    }

    @Override
    public ResponseEntity<WordPage> listWords(String band, Integer offset, Integer limit) {
        int off = offset == null ? 0 : offset;
        int lim = limit == null ? 100 : limit;
        return ResponseEntity.ok(mapper.toWordPage(learningQuery.listWords(currentUser.id(), band, off, lim)));
    }

    @Override
    public ResponseEntity<List<WordLeaf>> getStudyList() {
        return ResponseEntity.ok(mapper.toWordLeaves(learningQuery.study(currentUser.id())));
    }

    @Override
    public ResponseEntity<List<WordRef>> searchWords(String q, Integer limit) {
        int lim = limit == null ? 20 : limit;
        return ResponseEntity.ok(mapper.toWordRefs(learningQuery.searchWords(q, lim)));
    }

    @Override
    public ResponseEntity<List<TopicRef>> getTopicRoots() {
        return ResponseEntity.ok(mapper.toTopicRefs(learningQuery.topicRoots()));
    }

    @Override
    public ResponseEntity<TopicView> getTopicView(String id) {
        return learningQuery.topicView(currentUser.id(), id)
                .map(mapper::toTopicView)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Тема не найдена: " + id));
    }

    @Override
    public ResponseEntity<List<GrammarView>> listGrammar() {
        return ResponseEntity.ok(mapper.toGrammarViews(learningQuery.grammarTrunk(currentUser.id())));
    }

    @Override
    public ResponseEntity<GrammarView> getGrammar(String id) {
        return learningQuery.grammar(currentUser.id(), id)
                .map(mapper::toGrammarView)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Правило не найдено: " + id));
    }

    @Override
    public ResponseEntity<GrammarView> setGrammarMastery(String id, MasteryRequest request) {
        // отмечаем правило (знаю/учу), затем отдаём его с обновлённым статусом; нет правила → 404
        grammarMastery.setStatus(currentUser.id(), id, mapper.toDomainStatus(request.getStatus()));
        return learningQuery.grammar(currentUser.id(), id)
                .map(mapper::toGrammarView)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Правило не найдено: " + id));
    }

    @Override
    public ResponseEntity<List<MasteryView>> getMyMastery() {
        return ResponseEntity.ok(mapper.toMasteryViews(mastery.myMastery(currentUser.id())));
    }

    @Override
    public ResponseEntity<MasteryView> setMastery(String id, MasteryRequest request) {
        return ResponseEntity.ok(mapper.toMasteryView(
                mastery.setStatus(currentUser.id(), id, mapper.toDomainStatus(request.getStatus()))));
    }
}
