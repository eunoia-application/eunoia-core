package ru.eunoia.ingest;

/**
 * Словоформа — «лист» слова: сам текст формы + грамматический признак
 * (feature = теги kaikki через пробел, напр. "past participle"). Как record
 * даёт equals/hashCode по полям — этого хватает для дедупа в множестве.
 */
public record Form(String text, String feature) {
}
