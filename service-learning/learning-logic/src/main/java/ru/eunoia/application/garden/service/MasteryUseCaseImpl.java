package ru.eunoia.application.garden.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.in.MasteryUseCase;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;

/** Отметки мастерства. Логики немного — фиксируем факт + время; «увядание» (FSRS) придёт в M5. */
public class MasteryUseCaseImpl implements MasteryUseCase {

    private final MasteryRepositoryPort mastery;

    public MasteryUseCaseImpl(MasteryRepositoryPort mastery) {
        this.mastery = mastery;
    }

    @Override
    public Mastery setStatus(UUID userId, String lexemeId, MasteryStatus status) {
        return mastery.save(new Mastery(userId, lexemeId, status, LocalDateTime.now()));
    }

    @Override
    public Map<String, MasteryStatus> statuses(UUID userId, Collection<String> lexemeIds) {
        return mastery.statusesFor(userId, lexemeIds);
    }

    @Override
    public List<Mastery> myMastery(UUID userId) {
        return mastery.findByUser(userId);
    }
}
