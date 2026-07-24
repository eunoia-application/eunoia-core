package ru.eunoia.application.learning.domain.model;

import java.util.List;

/** Страница списка всех слов (по частоте) — для бандов «топ-100 / 100–200 / …». */
public record WordPage(int total, int offset, int limit, List<WordLeaf> words) {
}
