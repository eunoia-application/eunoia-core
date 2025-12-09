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
public class UpdateLastLoginRequestDto {

    private UUID userId;
    private LocalDateTime lastLoginAt;

}
