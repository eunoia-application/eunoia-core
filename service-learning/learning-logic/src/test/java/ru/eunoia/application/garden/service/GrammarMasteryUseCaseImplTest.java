package ru.eunoia.application.garden.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.garden.domain.model.GrammarMastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.in.ActivityUseCase;
import ru.eunoia.application.garden.port.out.GrammarMasteryRepositoryPort;

/** Отметки владения грамматикой: фиксируем факт + время, пишем активность, делегируем чтение. */
@ExtendWith(MockitoExtension.class)
class GrammarMasteryUseCaseImplTest {

    private static final UUID USER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String RULE = "past-simple";

    @Mock
    private GrammarMasteryRepositoryPort repository;
    @Mock
    private ActivityUseCase activity;

    private GrammarMasteryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GrammarMasteryUseCaseImpl(repository, activity);
    }

    @Test
    void setStatus_savesStampsRecordsActivityAndReturnsSaved() {
        GrammarMastery saved = new GrammarMastery(USER, RULE, MasteryStatus.KNOWN, LocalDateTime.now());
        when(repository.save(any())).thenReturn(saved);
        LocalDateTime before = LocalDateTime.now();

        GrammarMastery result = useCase.setStatus(USER, RULE, MasteryStatus.KNOWN);

        ArgumentCaptor<GrammarMastery> captor = ArgumentCaptor.forClass(GrammarMastery.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER);
        assertThat(captor.getValue().grammarId()).isEqualTo(RULE);
        assertThat(captor.getValue().status()).isEqualTo(MasteryStatus.KNOWN);
        assertThat(captor.getValue().updatedAt()).isAfterOrEqualTo(before);
        verify(activity).record(USER);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void statuses_delegatesToPort() {
        Map<String, MasteryStatus> map = Map.of(RULE, MasteryStatus.LEARNING);
        when(repository.statusesFor(USER, List.of(RULE))).thenReturn(map);

        assertThat(useCase.statuses(USER, List.of(RULE))).isSameAs(map);
    }

    @Test
    void findByUser_delegatesToPort() {
        List<GrammarMastery> list = List.of(new GrammarMastery(USER, RULE, MasteryStatus.KNOWN, LocalDateTime.now()));
        when(repository.findByUser(USER)).thenReturn(list);

        assertThat(useCase.findByUser(USER)).isSameAs(list);
    }
}
