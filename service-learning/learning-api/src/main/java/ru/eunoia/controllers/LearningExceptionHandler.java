package ru.eunoia.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.eunoia.application.learning.domain.exception.NotFoundException;

/** Доменные исключения учёбы → HTTP-статусы (ProblemDetail). Нет узла графа → 404. */
@RestControllerAdvice
public class LearningExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail onNotFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
