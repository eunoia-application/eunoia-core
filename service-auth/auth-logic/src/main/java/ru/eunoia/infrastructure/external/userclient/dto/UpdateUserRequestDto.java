package ru.eunoia.infrastructure.external.userclient.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDto {

    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    private boolean emailVerified;
    private boolean active;
    private boolean locked;
    private LocalDateTime lastLoginAt;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;

}
