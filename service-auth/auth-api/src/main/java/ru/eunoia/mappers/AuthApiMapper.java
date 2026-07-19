package ru.eunoia.mappers;

import com.eunoia.application.auth.model.AuthResponse;
import com.eunoia.application.auth.model.LoginRequest;
import com.eunoia.application.auth.model.RegisterRequest;
import com.eunoia.application.auth.model.UserProfile;
import org.springframework.stereotype.Component;
import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.User;

/**
 * Перевод между DTO контракта и доменом. Профильные поля (аватар, bio, stats, ...) auth не
 * заполняет — они принадлежат service-user; отдаём только то, что знает auth (id, email, username).
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
        response.setUser(toProfile(authentication.user()));
        return response;
    }

    private UserProfile toProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setId(user.id());
        profile.setEmail(user.email());
        profile.setUsername(user.username());
        profile.setEmailVerified(user.emailVerified());
        profile.setCreatedAt(user.createdAt());
        profile.setUpdatedAt(user.updatedAt());
        return profile;
    }
}
