package ru.eunoia.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eunoia.application.learning.model.GrammarView;
import com.eunoia.application.learning.model.LexemeCard;
import com.eunoia.application.learning.model.LexemeRef;
import com.eunoia.application.learning.model.MasteryRequest;
import com.eunoia.application.learning.model.MasteryStatus;
import com.eunoia.application.learning.model.MasteryView;
import com.eunoia.application.learning.model.TopicRef;
import com.eunoia.application.learning.model.TopicView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.port.in.MasteryUseCase;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.Lexeme;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.learning.domain.exception.NotFoundException;
import ru.eunoia.application.learning.port.in.LearningQueryUseCase;
import ru.eunoia.mappers.LearningApiMapper;
import ru.eunoia.security.CurrentUser;

/**
 * Тонкий веб-адаптер учёбы: каждый endpoint достаёт userId (для «своих» операций), делегирует в
 * use case и отдаёт 200 с замапленным телом; отсутствие узла графа (Optional.empty) → NotFoundException
 * (её exception-handler превратит в 404). Маппер и use case'ы замоканы — доменные объекты здесь
 * лишь opaque-токены, их разбор проверяется в LearningApiMapperTest.
 */
@ExtendWith(MockitoExtension.class)
class LearningControllerTest {

    private static final UUID USER = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private LearningQueryUseCase learningQuery;
    @Mock
    private MasteryUseCase mastery;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private LearningApiMapper mapper;

    @InjectMocks
    private LearningController controller;

    private static ru.eunoia.application.learning.domain.model.LexemeCard domainCard() {
        Lexeme lexeme = new Lexeme("en:go:VERB", "go", null, "en", null, null, List.of(), List.of());
        return new ru.eunoia.application.learning.domain.model.LexemeCard(
                lexeme, List.of(), List.of(), List.of(),
                ru.eunoia.application.garden.domain.model.MasteryStatus.UNKNOWN);
    }

    @Test
    void getLexeme_found_usesUserId_maps_returns200() {
        var domain = domainCard();
        LexemeCard dto = new LexemeCard();
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.lexemeCard(USER, "en:go:VERB")).thenReturn(Optional.of(domain));
        when(mapper.toLexemeCard(domain)).thenReturn(dto);

        ResponseEntity<LexemeCard> response = controller.getLexeme("en:go:VERB");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        verify(learningQuery).lexemeCard(USER, "en:go:VERB");
        verify(mapper).toLexemeCard(domain);
    }

    @Test
    void getLexeme_notFound_throwsNotFound() {
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.lexemeCard(USER, "missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getLexeme("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void searchLexemes_withLimit_delegates_returns200() {
        List<ru.eunoia.application.knowledge.domain.model.LexemeRef> hits = List.of();
        List<LexemeRef> dtos = List.of(new LexemeRef());
        when(learningQuery.search("go", 5)).thenReturn(hits);
        when(mapper.toRefs(hits)).thenReturn(dtos);

        ResponseEntity<List<LexemeRef>> response = controller.searchLexemes("go", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dtos);
        verify(learningQuery).search("go", 5);
    }

    @Test
    void searchLexemes_nullLimit_defaultsTo20() {
        List<ru.eunoia.application.knowledge.domain.model.LexemeRef> hits = List.of();
        when(learningQuery.search("go", 20)).thenReturn(hits);
        when(mapper.toRefs(hits)).thenReturn(List.of());

        controller.searchLexemes("go", null);

        verify(learningQuery).search("go", 20);
    }

    @Test
    void getTopicRoots_delegates_returns200() {
        List<ru.eunoia.application.knowledge.domain.model.Topic> roots = List.of();
        List<TopicRef> dtos = List.of(new TopicRef());
        when(learningQuery.topicRoots()).thenReturn(roots);
        when(mapper.toTopicRefs(roots)).thenReturn(dtos);

        ResponseEntity<List<TopicRef>> response = controller.getTopicRoots();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dtos);
    }

    @Test
    void getTopicView_found_usesUserId_maps_returns200() {
        var domain = new ru.eunoia.application.learning.domain.model.TopicView(
                new Topic("topic:travel", "Travel", "travel"), List.of());
        TopicView dto = new TopicView();
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.topicView(USER, "topic:travel")).thenReturn(Optional.of(domain));
        when(mapper.toTopicView(domain)).thenReturn(dto);

        ResponseEntity<TopicView> response = controller.getTopicView("topic:travel");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        verify(learningQuery).topicView(USER, "topic:travel");
    }

    @Test
    void getTopicView_notFound_throwsNotFound() {
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.topicView(USER, "missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getTopicView("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getGrammar_found_maps_returns200() {
        Grammar domain = new Grammar("gr:present-simple", "Present Simple", null);
        GrammarView dto = new GrammarView();
        when(learningQuery.grammar("gr:present-simple")).thenReturn(Optional.of(domain));
        when(mapper.toGrammarView(domain)).thenReturn(dto);

        ResponseEntity<GrammarView> response = controller.getGrammar("gr:present-simple");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    void getGrammar_notFound_throwsNotFound() {
        when(learningQuery.grammar("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getGrammar("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getMyMastery_usesUserId_maps_returns200() {
        List<Mastery> domainList = List.of();
        List<MasteryView> dtos = List.of(new MasteryView());
        when(currentUser.id()).thenReturn(USER);
        when(mastery.myMastery(USER)).thenReturn(domainList);
        when(mapper.toMasteryViews(domainList)).thenReturn(dtos);

        ResponseEntity<List<MasteryView>> response = controller.getMyMastery();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dtos);
        verify(mastery).myMastery(USER);
    }

    @Test
    void setMastery_usesUserId_mapsStatusToDomain_upserts_returns200() {
        MasteryRequest request = new MasteryRequest();
        request.setStatus(MasteryStatus.KNOWN);
        var domainStatus = ru.eunoia.application.garden.domain.model.MasteryStatus.KNOWN;
        Mastery saved = new Mastery(USER, "en:go:VERB", domainStatus, LocalDateTime.now());
        MasteryView dto = new MasteryView();
        when(currentUser.id()).thenReturn(USER);
        when(mapper.toDomainStatus(MasteryStatus.KNOWN)).thenReturn(domainStatus);
        when(mastery.setStatus(USER, "en:go:VERB", domainStatus)).thenReturn(saved);
        when(mapper.toMasteryView(saved)).thenReturn(dto);

        ResponseEntity<MasteryView> response = controller.setMastery("en:go:VERB", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        verify(mapper).toDomainStatus(MasteryStatus.KNOWN);
        verify(mastery).setStatus(USER, "en:go:VERB", domainStatus);
    }
}
