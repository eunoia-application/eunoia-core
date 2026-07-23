package ru.eunoia.application.domain.model;

/** Настройки профиля: тема, язык интерфейса, приватность, уведомления, AI-подсказки. */
public record ProfileSettings(
        Theme theme,
        String interfaceLanguage,
        Visibility profileVisibility,
        boolean emailNotifications,
        boolean aiSuggestionsEnabled
) {

    public enum Theme { LIGHT, DARK, AUTO }

    public enum Visibility { PUBLIC, PRIVATE }

    /** По умолчанию для нового профиля: авто-тема, приватный, уведомления и AI включены. */
    public static ProfileSettings defaults() {
        return new ProfileSettings(Theme.AUTO, null, Visibility.PRIVATE, true, true);
    }
}
