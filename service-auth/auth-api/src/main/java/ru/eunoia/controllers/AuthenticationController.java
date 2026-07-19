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
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.in.RegisterUseCase;
import ru.eunoia.mappers.AuthApiMapper;

/**
 * Веб-адаптер auth: реализует сгенерённый из контракта AuthApi и делегирует в use case'ы.
 * На M1 работают login и register; остальные операции — заглушки (501) до M2.
 */
@RestController
@RequiredArgsConstructor
public class AuthenticationController implements AuthApi {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
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

    // --- ниже — заглушки на M2 ---

    @Override
    public ResponseEntity<Void> logout() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Void> forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Void> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Void> verifyEmail(String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
