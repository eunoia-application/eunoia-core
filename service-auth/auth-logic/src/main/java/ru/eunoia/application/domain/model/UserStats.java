package ru.eunoia.application.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {
    private Integer noteCount;
    private Integer tagCount;
    private Integer evergreenNoteCount;
    private LocalDateTime lastActiveAt;
}
