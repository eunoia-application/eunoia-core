package ru.eunoia.controllers;

import com.eunoia.application.auth.api.AuthApi;
import com.eunoia.application.auth.model.AuthResponse;
import com.eunoia.application.auth.model.ForgotPasswordRequest;
import com.eunoia.application.auth.model.LoginRequest;
import com.eunoia.application.auth.model.RefreshTokenRequest;
import com.eunoia.application.auth.model.RegisterRequest;
import com.eunoia.application.auth.model.ResetPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.eunoia.application.port.in.ForgotPasswordUseCase;
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.in.LogoutUseCase;
import ru.eunoia.application.port.in.RefreshTokenUseCase;
import ru.eunoia.application.port.in.RegisterUseCase;
import ru.eunoia.application.port.in.ResetPasswordUseCase;
import ru.eunoia.application.port.in.VerifyEmailUseCase;
import ru.eunoia.mappers.AuthApiMapper;
import ru.eunoia.security.CurrentUser;

/**
 * Веб-адаптер auth: реализует сгенерённый AuthApi и делегирует в use case'ы.
 */
@RestController
@RequiredArgsConstructor
public class AuthenticationController implements AuthApi {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final CurrentUser currentUser;
    private final AuthApiMapper mapper;

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
        var authentication = loginUseCase.login(mapper.toCommand(loginRequest));
        return ResponseEntity.ok(mapper.toResponse(authentication));
    }

    @Override
    public ResponseEntity<AuthResponse> register(RegisterRequest registerRequest) {
        var authentication = registerUseCase.register(mapper.toCommand(registerRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(authentication));
    }

    @Override
    public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        var authentication = refreshTokenUseCase.refresh(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(mapper.toResponse(authentication));
    }

    @Override
    public ResponseEntity<Void> logout() {
        logoutUseCase.logout(currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> verifyEmail(String token) {
        verifyEmailUseCase.verify(token);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        forgotPasswordUseCase.requestReset(forgotPasswordRequest.getEmail());
        return ResponseEntity.noContent().build(); // 204 всегда — не раскрываем, есть ли аккаунт
    }

    @Override
    public ResponseEntity<Void> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        resetPasswordUseCase.reset(resetPasswordRequest.getToken(), resetPasswordRequest.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAccount() {
        // TODO M3: удалить identity + токены и опубликовать UserDeleted (Kafka) для каскада на service-user
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
