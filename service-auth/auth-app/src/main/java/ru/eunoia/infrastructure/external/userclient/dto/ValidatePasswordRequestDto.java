package ru.eunoia.infrastructure.external.userclient.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePasswordRequestDto {

    private UUID userId;
    private String password;

}
