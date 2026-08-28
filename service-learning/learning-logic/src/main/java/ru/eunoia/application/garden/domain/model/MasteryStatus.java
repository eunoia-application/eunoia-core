package ru.eunoia.application.garden.domain.model;

/**
 * Статус владения словом у пользователя. Пока грубо (M4); в M5 заменится FSRS-retrievability
 * («увядание» листа со временем). Отсутствие записи = слово ещё не тронуто.
 */
public enum MasteryStatus {
    KNOWN,
    LEARNING,
    UNKNOWN
}
