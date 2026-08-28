package ru.eunoia.application.garden.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.GrammarMastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.in.ActivityUseCase;
import ru.eunoia.application.garden.port.in.GrammarMasteryUseCase;
import ru.eunoia.application.garden.port.out.GrammarMasteryRepositoryPort;

/** Отметки владения грамматикой. Как и словесное — фиксируем факт + время + день занятий. */
public class GrammarMasteryUseCaseImpl implements GrammarMasteryUseCase {

    private final GrammarMasteryRepositoryPort mastery;
    private final ActivityUseCase activity;

    public GrammarMasteryUseCaseImpl(GrammarMasteryRepositoryPort mastery, ActivityUseCase activity) {
        this.mastery = mastery;
        this.activity = activity;
    }

    @Override
    public GrammarMastery setStatus(UUID userId, String grammarId, MasteryStatus status) {
        GrammarMastery saved = mastery.save(new GrammarMastery(userId, grammarId, status, LocalDateTime.now()));
        activity.record(userId);   // отметка правила — тоже день занятий
        return saved;
    }

    @Override
    public Map<String, MasteryStatus> statuses(UUID userId, Collection<String> grammarIds) {
        return mastery.statusesFor(userId, grammarIds);
    }

    @Override
    public List<GrammarMastery> findByUser(UUID userId) {
        return mastery.findByUser(userId);
    }
}
