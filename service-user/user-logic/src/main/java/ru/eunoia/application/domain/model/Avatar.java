package ru.eunoia.application.domain.model;

/** Байты аватара + их MIME-тип. Хранилище абстрагировано портом (сейчас Postgres bytea). */
public record Avatar(byte[] content, String contentType) {
}
