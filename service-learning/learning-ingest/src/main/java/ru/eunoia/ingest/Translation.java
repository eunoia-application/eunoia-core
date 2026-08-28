package ru.eunoia.ingest;

/**
 * Перевод — «лист» слова: текст перевода на языке lang (у нас всегда "ru").
 * Романизацию (roman) намеренно не тянем — в модели графа только text + lang.
 */
public record Translation(String text, String lang) {
}
