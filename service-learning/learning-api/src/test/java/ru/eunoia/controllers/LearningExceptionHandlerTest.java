package ru.eunoia.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import ru.eunoia.application.learning.domain.exception.NotFoundException;

/**
 * Доменное исключение учёбы → HTTP-статус. Вызываем @ExceptionHandler напрямую и сверяем статус
 * и detail (сообщение исключения) в ProblemDetail.
 */
class LearningExceptionHandlerTest {

    private final LearningExceptionHandler handler = new LearningExceptionHandler();

    @Test
    void notFound_mapsTo404_carryingExceptionMessage() {
        ProblemDetail problem = handler.onNotFound(new NotFoundException("Слово не найдено: en:go:VERB"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("Слово не найдено: en:go:VERB");
    }
}
