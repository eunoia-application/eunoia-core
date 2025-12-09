package ru.eunoia.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.eunoia.domain.model.enums.DefaultNoteStatus;
import ru.eunoia.domain.model.enums.Theme;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {
    private Theme theme;
    private DefaultNoteStatus defaultNoteStatus;
    private boolean emailNotifications;
    private boolean aiSuggestionsEnabled;
}
