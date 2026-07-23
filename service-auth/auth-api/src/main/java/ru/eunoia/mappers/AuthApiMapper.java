package ru.eunoia.mappers;

import com.eunoia.application.auth.model.AuthResponse;
import com.eunoia.application.auth.model.AuthUser;
import com.eunoia.application.auth.model.LoginRequest;
import com.eunoia.application.auth.model.RegisterRequest;
import org.springframework.stereotype.Component;
import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.User;

/**
 * Перевод между DTO контракта и доменом. auth отдаёт только слим-идентичность (AuthUser);
 * полный профиль (имя, аватар, bio, настройки) — за service-user.
 */
@Component
public class AuthApiMapper {

    public LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.getEmail(), request.getPassword());
    }

    public RegisterCommand toCommand(RegisterRequest request) {
        return new RegisterCommand(request.getEmail(), request.getPassword(), request.getUsername());
    }

    public AuthResponse toResponse(Authentication authentication) {
        AuthTokens tokens = authentication.tokens();

        AuthResponse response = new AuthResponse();
        response.setAccessToken(tokens.accessToken());
        response.setRefreshToken(tokens.refreshToken());
        response.setTokenType(AuthResponse.TokenTypeEnum.BEARER);
        response.setExpiresIn(tokens.expiresIn());
        response.setUser(toAuthUser(authentication.user()));
        return response;
    }

    private AuthUser toAuthUser(User user) {
        AuthUser authUser = new AuthUser();
        authUser.setId(user.id());
        authUser.setEmail(user.email());
        authUser.setUsername(user.username());
        authUser.setEmailVerified(user.emailVerified());
        authUser.setCreatedAt(user.createdAt());
        authUser.setUpdatedAt(user.updatedAt());
        return authUser;
    }
}
