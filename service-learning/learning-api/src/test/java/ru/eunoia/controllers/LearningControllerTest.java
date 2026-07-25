package ru.eunoia.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eunoia.application.learning.model.Band;
import com.eunoia.application.learning.model.GrammarView;
import com.eunoia.application.learning.model.MasteryRequest;
import com.eunoia.application.learning.model.MasteryStatus;
import com.eunoia.application.learning.model.MasteryView;
import com.eunoia.application.learning.model.TopicRef;
import com.eunoia.application.learning.model.TopicView;
import com.eunoia.application.learning.model.TreeSnapshot;
import com.eunoia.application.learning.model.WordCard;
import com.eunoia.application.learning.model.WordLeaf;
import com.eunoia.application.learning.model.WordPage;
import com.eunoia.application.learning.model.WordRef;
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
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Word;
import ru.eunoia.application.learning.domain.exception.NotFoundException;
import ru.eunoia.application.learning.port.in.LearningQueryUseCase;
import ru.eunoia.mappers.LearningApiMapper;
import ru.eunoia.security.CurrentUser;

/**
 * Тонкий веб-адаптер учёбы: каждый endpoint достаёт userId (для «своих» операций), делегирует в
 * use case и отдаёт 200 с замапленным телом; отсутствие узла графа (Optional.empty) → NotFoundException
 * (её exception-handler превратит в 404). Маппер и use case'ы замоканы — доменные объекты здесь
 * лишь opaque-токены. Единица — СЛОВО (лемма, id вида en:go).
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

    private static ru.eunoia.application.learning.domain.model.WordCard domainCard() {
        Word word = new Word("en:go", "go", null, null, List.of());
        return new ru.eunoia.application.learning.domain.model.WordCard(
                word, ru.eunoia.application.garden.domain.model.MasteryStatus.UNKNOWN);
    }

    @Test
    void getTree_usesUserId_maps_returns200() {
        var domain = new ru.eunoia.application.learning.domain.model.TreeSnapshot(
                new ru.eunoia.application.learning.domain.model.TreeVocabulary(0, 0, 0),
                List.of(), ru.eunoia.application.garden.domain.model.ActivityStats.empty());
        TreeSnapshot dto = new TreeSnapshot();
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.treeSnapshot(USER)).thenReturn(domain);
        when(mapper.toTreeSnapshot(domain)).thenReturn(dto);

        ResponseEntity<TreeSnapshot> response = controller.getTree();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        verify(learningQuery).treeSnapshot(USER);
    }

    @Test
    void getWord_found_usesUserId_maps_returns200() {
        var domain = domainCard();
        WordCard dto = new WordCard();
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.wordCard(USER, "en:go")).thenReturn(Optional.of(domain));
        when(mapper.toWordCard(domain)).thenReturn(dto);

        ResponseEntity<WordCard> response = controller.getWord("en:go");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        verify(learningQuery).wordCard(USER, "en:go");
    }

    @Test
    void getWord_notFound_throwsNotFound() {
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.wordCard(USER, "missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getWord("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getBands_usesUserId_maps_returns200() {
        List<ru.eunoia.application.learning.domain.model.Band> domain = List.of();
        List<Band> dtos = List.of(new Band());
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.bands(USER)).thenReturn(domain);
        when(mapper.toBands(domain)).thenReturn(dtos);

        ResponseEntity<List<Band>> response = controller.getBands();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dtos);
        verify(learningQuery).bands(USER);
    }

    @Test
    void listWords_nullParams_defaultTo0and100_maps_returns200() {
        var page = new ru.eunoia.application.learning.domain.model.WordPage(0, 0, 100, List.of());
        WordPage dto = new WordPage();
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.listWords(USER, null, 0, 100)).thenReturn(page);
        when(mapper.toWordPage(page)).thenReturn(dto);

        ResponseEntity<WordPage> response = controller.listWords(null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        verify(learningQuery).listWords(USER, null, 0, 100);
    }

    @Test
    void listWords_withBand_delegatesBand() {
        var page = new ru.eunoia.application.learning.domain.model.WordPage(0, 0, 100, List.of());
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.listWords(USER, "top-100", 0, 100)).thenReturn(page);
        when(mapper.toWordPage(page)).thenReturn(new WordPage());

        controller.listWords("top-100", 0, 100);

        verify(learningQuery).listWords(USER, "top-100", 0, 100);
    }

    @Test
    void getStudyList_usesUserId_maps_returns200() {
        List<ru.eunoia.application.learning.domain.model.WordLeaf> domain = List.of();
        List<WordLeaf> dtos = List.of(new WordLeaf());
        when(currentUser.id()).thenReturn(USER);
        when(learningQuery.study(USER)).thenReturn(domain);
        when(mapper.toWordLeaves(domain)).thenReturn(dtos);

        ResponseEntity<List<WordLeaf>> response = controller.getStudyList();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dtos);
        verify(learningQuery).study(USER);
    }

    @Test
    void searchWords_withLimit_delegates_returns200() {
        List<ru.eunoia.application.knowledge.domain.model.WordRef> hits = List.of();
        List<WordRef> dtos = List.of(new WordRef());
        when(learningQuery.searchWords("go", 5)).thenReturn(hits);
        when(mapper.toWordRefs(hits)).thenReturn(dtos);

        ResponseEntity<List<WordRef>> response = controller.searchWords("go", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dtos);
        verify(learningQuery).searchWords("go", 5);
    }

    @Test
    void searchWords_nullLimit_defaultsTo20() {
        List<ru.eunoia.application.knowledge.domain.model.WordRef> hits = List.of();
        when(learningQuery.searchWords("go", 20)).thenReturn(hits);
        when(mapper.toWordRefs(hits)).thenReturn(List.of());

        controller.searchWords("go", null);

        verify(learningQuery).searchWords("go", 20);
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
    void listGrammar_maps_returns200() {
        var trunk = List.of(new ru.eunoia.application.learning.domain.model.GrammarView(
                new Grammar("gr:present-simple", "Present Simple", null, List.of()), List.of()));
        List<GrammarView> dtos = List.of(new GrammarView());
        when(learningQuery.grammarTrunk()).thenReturn(trunk);
        when(mapper.toGrammarViews(trunk)).thenReturn(dtos);

        ResponseEntity<List<GrammarView>> response = controller.listGrammar();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dtos);
    }

    @Test
    void getGrammar_found_maps_returns200() {
        var domain = new ru.eunoia.application.learning.domain.model.GrammarView(
                new Grammar("gr:present-simple", "Present Simple", null, List.of()), List.of());
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
    void setMastery_byLemma_mapsStatusToDomain_upserts_returns200() {
        MasteryRequest request = new MasteryRequest();
        request.setStatus(MasteryStatus.KNOWN);
        var domainStatus = ru.eunoia.application.garden.domain.model.MasteryStatus.KNOWN;
        Mastery saved = new Mastery(USER, "en:go", domainStatus, LocalDateTime.now());
        MasteryView dto = new MasteryView();
        when(currentUser.id()).thenReturn(USER);
        when(mapper.toDomainStatus(MasteryStatus.KNOWN)).thenReturn(domainStatus);
        when(mastery.setStatus(USER, "en:go", domainStatus)).thenReturn(saved);
        when(mapper.toMasteryView(saved)).thenReturn(dto);

        ResponseEntity<MasteryView> response = controller.setMastery("en:go", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        verify(mastery).setStatus(USER, "en:go", domainStatus);
    }
}
