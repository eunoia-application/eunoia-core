package ru.eunoia.ingest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Курируемая таксономия тем сада: ветки (узлы Topic) + маппинг kaikki-категорий на ветку.
 * Читается из seed/topics.tsv. Темы в словарных дампах не заданы — их структуру задаём мы,
 * а вот раскладку слов по веткам тул выводит из {@code senses[].categories} каждой леммы.
 *
 * <p>Приоритет = порядок веток в файле: слово уходит в ПЕРВУЮ ветку, чью категорию оно несёт
 * (бытовое выше технического). Пустой каталог (--topics не задан) → темы не проставляются.
 */
final class TopicCatalog {

    /** Ветка сада: id/имя/slug, родитель (пусто = корень) и её kaikki-категории. */
    record Branch(String id, String name, String slug, String parentId, Set<String> categories) {
    }

    private final List<Branch> branches;   // в порядке приоритета

    private TopicCatalog(List<Branch> branches) {
        this.branches = branches;
    }

    static TopicCatalog empty() {
        return new TopicCatalog(List.of());
    }

    /** Все ветки (для создания узлов Topic и рёбер SUBTOPIC). */
    List<Branch> branches() {
        return branches;
    }

    /**
     * Ветка для слова по его kaikki-категориям: первая по приоритету ветка, чья категория
     * есть у слова. null — если ни одна не подошла (слово останется без темы).
     */
    String resolve(Set<String> wordCategories) {
        if (wordCategories.isEmpty()) {
            return null;
        }
        for (Branch b : branches) {
            for (String c : b.categories) {
                if (wordCategories.contains(c)) {
                    return b.id;
                }
            }
        }
        return null;
    }

    /** Читает topics.tsv: {@code id <TAB> name <TAB> slug <TAB> parentId <TAB> cat1,cat2,...} */
    static TopicCatalog load(Path tsv) throws IOException {
        List<Branch> out = new ArrayList<>();
        for (String line : Files.readAllLines(tsv, StandardCharsets.UTF_8)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;                       // пустые и комментарии
            }
            String[] col = line.split("\t", -1);
            if (col.length < 4 || col[0].isBlank()) {
                continue;                       // битая строка — пропускаем
            }
            String id = col[0].trim();
            String name = col[1].trim();
            String slug = col[2].trim();
            String parentId = col[3].trim();    // пусто = корень
            Set<String> categories = new LinkedHashSet<>();
            if (col.length > 4) {
                for (String c : col[4].split(",")) {
                    String cat = c.trim();
                    if (!cat.isEmpty()) {
                        categories.add(cat);
                    }
                }
            }
            out.add(new Branch(id, name, slug, parentId, categories));
        }
        return new TopicCatalog(out);
    }
}
