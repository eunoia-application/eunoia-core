package ru.eunoia.application.learning.domain.model;

/**
 * Блок топ-слов по частоте (уровень) с моим прогрессом: диапазон рангов + сколько всего слов
 * в блоке и сколько я отметил «знаю»/«учить». Блоки эксклюзивны (1–100, 101–500, …).
 */
public record Band(String id, String label, int fromRank, int toRank, int total, int known, int learning) {
}
