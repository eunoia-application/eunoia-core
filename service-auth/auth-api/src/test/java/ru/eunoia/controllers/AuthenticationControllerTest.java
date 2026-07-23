package ru.eunoia.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eunoia.application.auth.model.AuthResponse;
import com.eunoia.application.auth.model.ForgotPasswordRequest;
import com.eunoia.application.auth.model.LoginRequest;
import com.eunoia.application.auth.model.RefreshTokenRequest;
import com.eunoia.application.auth.model.RegisterRequest;
import com.eunoia.application.auth.model.ResetPasswordRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.model.Authentication;
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
 * Тонкий веб-адаптер: проверяем, что каждый endpoint делегирует в нужный use case
 * и возвращает корректный HTTP-статус с замапленным телом. Маппер замокан, чтобы
 * тест видел именно делегирование, а не логику преобразования (она в AuthApiMapperTest).
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private LoginUseCase loginUseCase;
    @Mock
    private RegisterUseCase registerUseCase;
    @Mock
    private RefreshTokenUseCase refreshTokenUseCase;
    @Mock
    private LogoutUseCase logoutUseCase;
    @Mock
    private VerifyEmailUseCase verifyEmailUseCase;
    @Mock
    private ForgotPasswordUseCase forgotPasswordUseCase;
    @Mock
    private ResetPasswordUseCase resetPasswordUseCase;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuthApiMapper mapper;

    @InjectMocks
    private AuthenticationController controller;

    @Test
    void login_delegatesToLoginUseCase_andReturns200WithMappedBody() {
        LoginRequest request = new LoginRequest("user@example.com", "secret");
        LoginCommand command = new LoginCommand("user@example.com", "secret");
        Authentication authentication = new Authentication(null, null);
        AuthResponse mapped = new AuthResponse();

        when(mapper.toCommand(request)).thenReturn(command);
        when(loginUseCase.login(command)).thenReturn(authentication);
        when(mapper.toResponse(authentication)).thenReturn(mapped);

        ResponseEntity<AuthResponse> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(mapper).toCommand(request);
        verify(loginUseCase).login(command);
        verify(mapper).toResponse(authentication);
    }

    @Test
    void register_delegatesToRegisterUseCase_andReturns201WithMappedBody() {
        RegisterRequest request = new RegisterRequest("user@example.com", "secret12", "eunoia_user");
        RegisterCommand command = new RegisterCommand("user@example.com", "secret12", "eunoia_user");
        Authentication authentication = new Authentication(null, null);
        AuthResponse mapped = new AuthResponse();

        when(mapper.toCommand(request)).thenReturn(command);
        when(registerUseCase.register(command)).thenReturn(authentication);
        when(mapper.toResponse(authentication)).thenReturn(mapped);

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(registerUseCase).register(command);
        verify(mapper).toResponse(authentication);
    }

    @Test
    void refreshToken_delegatesRawTokenToUseCase_andReturns200WithMappedBody() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-value");
        Authentication authentication = new Authentication(null, null);
        AuthResponse mapped = new AuthResponse();

        when(refreshTokenUseCase.refresh("refresh-token-value")).thenReturn(authentication);
        when(mapper.toResponse(authentication)).thenReturn(mapped);

        ResponseEntity<AuthResponse> response = controller.refreshToken(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(refreshTokenUseCase).refresh("refresh-token-value");
        verify(mapper).toResponse(authentication);
    }

    @Test
    void logout_usesCurrentUserId_andReturns204() {
        UUID userId = UUID.randomUUID();
        when(currentUser.id()).thenReturn(userId);

        ResponseEntity<Void> response = controller.logout();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(currentUser).id();
        verify(logoutUseCase).logout(userId);
    }

    @Test
    void verifyEmail_delegatesToken_andReturns204() {
        ResponseEntity<Void> response = controller.verifyEmail("verify-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(verifyEmailUseCase).verify("verify-token");
    }

    @Test
    void forgotPassword_delegatesEmail_andAlwaysReturns204() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");

        ResponseEntity<Void> response = controller.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(forgotPasswordUseCase).requestReset("user@example.com");
    }

    @Test
    void resetPassword_delegatesTokenAndPassword_andReturns204() {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "newSecret1");

        ResponseEntity<Void> response = controller.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(resetPasswordUseCase).reset("reset-token", "newSecret1");
    }
}
