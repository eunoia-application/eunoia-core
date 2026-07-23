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
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;

/** Отметки мастерства: фиксируем факт + время и делегируем чтение в порт. */
@ExtendWith(MockitoExtension.class)
class MasteryUseCaseImplTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String LEXEME = "en:go:VERB";

    @Mock
    private MasteryRepositoryPort repository;

    private MasteryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new MasteryUseCaseImpl(repository);
    }

    @Test
    void setStatus_savesWithStampAndReturnsSaved() {
        Mastery saved = new Mastery(USER, LEXEME, MasteryStatus.KNOWN, LocalDateTime.now());
        when(repository.save(any())).thenReturn(saved);
        LocalDateTime before = LocalDateTime.now();

        Mastery result = useCase.setStatus(USER, LEXEME, MasteryStatus.KNOWN);

        ArgumentCaptor<Mastery> captor = ArgumentCaptor.forClass(Mastery.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER);
        assertThat(captor.getValue().lexemeId()).isEqualTo(LEXEME);
        assertThat(captor.getValue().status()).isEqualTo(MasteryStatus.KNOWN);
        assertThat(captor.getValue().updatedAt()).isAfterOrEqualTo(before);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void statuses_delegatesToPort() {
        Map<String, MasteryStatus> map = Map.of(LEXEME, MasteryStatus.LEARNING);
        when(repository.statusesFor(USER, List.of(LEXEME))).thenReturn(map);

        assertThat(useCase.statuses(USER, List.of(LEXEME))).isSameAs(map);
    }

    @Test
    void myMastery_delegatesToPort() {
        List<Mastery> list = List.of(new Mastery(USER, LEXEME, MasteryStatus.LEARNING, LocalDateTime.now()));
        when(repository.findByUser(USER)).thenReturn(list);

        assertThat(useCase.myMastery(USER)).isSameAs(list);
    }
}
