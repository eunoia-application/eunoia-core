package ru.eunoia.controllers;

import com.eunoia.application.learning.api.LearningApi;
import com.eunoia.application.learning.model.GrammarView;
import com.eunoia.application.learning.model.LexemeCard;
import com.eunoia.application.learning.model.LexemeRef;
import com.eunoia.application.learning.model.MasteryRequest;
import com.eunoia.application.learning.model.MasteryView;
import com.eunoia.application.learning.model.TopicRef;
import com.eunoia.application.learning.model.TopicView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.eunoia.application.garden.port.in.MasteryUseCase;
import ru.eunoia.application.learning.domain.exception.NotFoundException;
import ru.eunoia.application.learning.port.in.LearningQueryUseCase;
import ru.eunoia.mappers.LearningApiMapper;
import ru.eunoia.security.CurrentUser;

/**
 * REST-адаптер учёбы: реализует сгенерированный из контракта {@code LearningApi}. Чтение графа
 * знаний с наложением моего прогресса (garden-view) и отметка владения словами. Тонкий слой:
 * достаёт userId, делегирует в use case'ы и мапит домен в DTO. Отсутствие узла графа → 404.
 */
@RestController
public class LearningController implements LearningApi {

    private final LearningQueryUseCase learningQuery;
    private final MasteryUseCase mastery;
    private final CurrentUser currentUser;
    private final LearningApiMapper mapper;

    public LearningController(LearningQueryUseCase learningQuery, MasteryUseCase mastery,
                              CurrentUser currentUser, LearningApiMapper mapper) {
        this.learningQuery = learningQuery;
        this.mastery = mastery;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<LexemeCard> getLexeme(String id) {
        return learningQuery.lexemeCard(currentUser.id(), id)
                .map(mapper::toLexemeCard)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Слово не найдено: " + id));
    }

    @Override
    public ResponseEntity<List<LexemeRef>> searchLexemes(String q, Integer limit) {
        int limitValue = limit == null ? 20 : limit;
        return ResponseEntity.ok(mapper.toRefs(learningQuery.search(q, limitValue)));
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
    public ResponseEntity<GrammarView> getGrammar(String id) {
        return learningQuery.grammar(id)
                .map(mapper::toGrammarView)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Правило не найдено: " + id));
    }

    @Override
    public ResponseEntity<List<MasteryView>> getMyMastery() {
        return ResponseEntity.ok(mapper.toMasteryViews(mastery.myMastery(currentUser.id())));
    }

    @Override
    public ResponseEntity<MasteryView> setMastery(String lexemeId, MasteryRequest request) {
        return ResponseEntity.ok(mapper.toMasteryView(
                mastery.setStatus(currentUser.id(), lexemeId, mapper.toDomainStatus(request.getStatus()))));
    }
}
