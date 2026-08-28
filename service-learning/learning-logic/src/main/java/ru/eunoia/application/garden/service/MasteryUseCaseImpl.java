package ru.eunoia.application.garden.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.in.ActivityUseCase;
import ru.eunoia.application.garden.port.in.MasteryUseCase;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;

/** Отметки мастерства. Логики немного — фиксируем факт + время; «увядание» (FSRS) придёт в M5. */
public class MasteryUseCaseImpl implements MasteryUseCase {

    private final MasteryRepositoryPort mastery;
    private final ActivityUseCase activity;

    public MasteryUseCaseImpl(MasteryRepositoryPort mastery, ActivityUseCase activity) {
        this.mastery = mastery;
        this.activity = activity;
    }

    @Override
    public Mastery setStatus(UUID userId, String lexemeId, MasteryStatus status) {
        Mastery saved = mastery.save(new Mastery(userId, lexemeId, status, LocalDateTime.now()));
        activity.record(userId);   // фиксируем день занятий (для стриков/погоды/сезонов дерева)
        return saved;
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
