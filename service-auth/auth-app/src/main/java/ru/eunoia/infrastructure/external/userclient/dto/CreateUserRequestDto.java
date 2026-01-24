package ru.eunoia.infrastructure.external.userclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequestDto {

    private String email;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;

}
