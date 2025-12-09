package ru.eunoia.infrastructure.external.userclient.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UpdatePasswordRequestDto {

    private UUID userId;
    private String newPasswordHash;
    private LocalDateTime updatedAt;

}
