package ru.eunoia.application.learning.domain.exception;

/** Запрошенный узел графа (слово/тема/правило) не найден → 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
