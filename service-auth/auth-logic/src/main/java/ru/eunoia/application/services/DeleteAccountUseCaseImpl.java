package ru.eunoia.application.services;

import java.time.LocalDateTime;
import java.util.UUID;
import ru.eunoia.application.domain.event.UserDeleted;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.port.in.DeleteAccountUseCase;
import ru.eunoia.application.port.out.EventPublisherPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/**
 * Удаление аккаунта (Design B: identity у auth). Гасим refresh-токены, убираем учётку,
 * пишем аудит и публикуем UserDeleted — по нему service-user удалит профиль. Без Spring.
 */
public class DeleteAccountUseCaseImpl implements DeleteAccountUseCase {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final EventPublisherPort eventPublisher;
    private final AuthEventRecorder recorder;

    public DeleteAccountUseCaseImpl(UserRepositoryPort userRepository,
                                    RefreshTokenRepositoryPort refreshTokenRepository,
                                    EventPublisherPort eventPublisher,
                                    AuthEventRecorder recorder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.eventPublisher = eventPublisher;
        this.recorder = recorder;
    }

    @Override
    public void delete(UUID userId) {
        refreshTokenRepository.revokeByUserId(userId);                        // разлогиниваем везде
        userRepository.deleteById(userId);                                    // email снова свободен
        recorder.record(userId, AuthEvent.EventType.ACCOUNT_DELETED, true);   // аудит переживает удаление (FK нет)
        eventPublisher.publish(new UserDeleted(userId, LocalDateTime.now()));  // каскад на service-user
    }
}
