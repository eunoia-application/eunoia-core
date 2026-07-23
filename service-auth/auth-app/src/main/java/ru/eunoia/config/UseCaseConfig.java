package ru.eunoia.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eunoia.application.port.in.ForgotPasswordUseCase;
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.in.LogoutUseCase;
import ru.eunoia.application.port.in.RefreshTokenUseCase;
import ru.eunoia.application.port.in.RegisterUseCase;
import ru.eunoia.application.port.in.ResetPasswordUseCase;
import ru.eunoia.application.port.in.VerifyEmailUseCase;
import ru.eunoia.application.port.out.AuthEventRepositoryPort;
import ru.eunoia.application.port.out.EmailSenderPort;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;
import ru.eunoia.application.services.AuthEventRecorder;
import ru.eunoia.application.services.ForgotPasswordUseCaseImpl;
import ru.eunoia.application.services.LoginUseCaseImpl;
import ru.eunoia.application.services.LogoutUseCaseImpl;
import ru.eunoia.application.services.RefreshTokenUseCaseImpl;
import ru.eunoia.application.services.RegisterUseCaseImpl;
import ru.eunoia.application.services.ResetPasswordUseCaseImpl;
import ru.eunoia.application.services.VerifyEmailUseCaseImpl;

/**
 * Composition root: ядро (auth-logic) без Spring, поэтому его use case'ы собираем здесь из портов.
 */
@Configuration
public class UseCaseConfig {

    /** Общий писатель аудита — прокидываем во все use case'ы, чтобы события собирались в одном месте. */
    @Bean
    public AuthEventRecorder authEventRecorder(AuthEventRepositoryPort authEventRepository) {
        return new AuthEventRecorder(authEventRepository);
    }

    @Bean
    public LoginUseCase loginUseCase(UserRepositoryPort userRepository,
                                     PasswordEncoderPort passwordEncoder,
                                     TokenProviderPort tokenProvider,
                                     RefreshTokenRepositoryPort refreshTokenRepository,
                                     AuthEventRecorder recorder,
                                     @Value("${auth.lockout.max-attempts:5}") int maxAttempts,
                                     @Value("${auth.lockout.lock-duration-minutes:15}") long lockMinutes) {
        return new LoginUseCaseImpl(userRepository, passwordEncoder, tokenProvider, refreshTokenRepository,
                recorder, maxAttempts, Duration.ofMinutes(lockMinutes));
    }

    @Bean
    public RegisterUseCase registerUseCase(UserRepositoryPort userRepository,
                                           PasswordEncoderPort passwordEncoder,
                                           TokenProviderPort tokenProvider,
                                           RefreshTokenRepositoryPort refreshTokenRepository,
                                           OneTimeTokenRepositoryPort oneTimeTokenRepository,
                                           EmailSenderPort emailSender,
                                           AuthEventRecorder recorder,
                                           @Value("${auth.token.email-verification-hours:24}") long verifyHours) {
        return new RegisterUseCaseImpl(userRepository, passwordEncoder, tokenProvider, refreshTokenRepository,
                oneTimeTokenRepository, emailSender, recorder, Duration.ofHours(verifyHours));
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(UserRepositoryPort userRepository,
                                                   TokenProviderPort tokenProvider,
                                                   RefreshTokenRepositoryPort refreshTokenRepository,
                                                   AuthEventRecorder recorder) {
        return new RefreshTokenUseCaseImpl(userRepository, tokenProvider, refreshTokenRepository, recorder);
    }

    @Bean
    public LogoutUseCase logoutUseCase(RefreshTokenRepositoryPort refreshTokenRepository,
                                       AuthEventRecorder recorder) {
        return new LogoutUseCaseImpl(refreshTokenRepository, recorder);
    }

    @Bean
    public VerifyEmailUseCase verifyEmailUseCase(OneTimeTokenRepositoryPort oneTimeTokenRepository,
                                                 UserRepositoryPort userRepository,
                                                 AuthEventRecorder recorder) {
        return new VerifyEmailUseCaseImpl(oneTimeTokenRepository, userRepository, recorder);
    }

    @Bean
    public ForgotPasswordUseCase forgotPasswordUseCase(UserRepositoryPort userRepository,
                                                       OneTimeTokenRepositoryPort oneTimeTokenRepository,
                                                       EmailSenderPort emailSender,
                                                       AuthEventRecorder recorder,
                                                       @Value("${auth.token.password-reset-minutes:30}") long resetMinutes) {
        return new ForgotPasswordUseCaseImpl(userRepository, oneTimeTokenRepository, emailSender, recorder,
                Duration.ofMinutes(resetMinutes));
    }

    @Bean
    public ResetPasswordUseCase resetPasswordUseCase(OneTimeTokenRepositoryPort oneTimeTokenRepository,
                                                     UserRepositoryPort userRepository,
                                                     PasswordEncoderPort passwordEncoder,
                                                     RefreshTokenRepositoryPort refreshTokenRepository,
                                                     AuthEventRecorder recorder) {
        return new ResetPasswordUseCaseImpl(oneTimeTokenRepository, userRepository, passwordEncoder,
                refreshTokenRepository, recorder);
    }
}
