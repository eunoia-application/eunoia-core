package ru.eunoia.mappers;

import com.eunoia.application.auth.model.ForgotPasswordRequest;
import com.eunoia.application.auth.model.ResetPasswordRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.eunoia.domain.model.EmailVerificationToken;
import ru.eunoia.domain.model.PasswordResetToken;
import ru.eunoia.domain.model.RefreshToken;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TokenMapper {

    // =========== DTO -> Domain ===========

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "tokenHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "verifiedAt", ignore = true)
    @Mapping(target = "used", constant = "false")
    EmailVerificationToken toDomain(ForgotPasswordRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "tokenHash", source = "token")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "usedAt", ignore = true)
    @Mapping(target = "used", constant = "false")
    @Mapping(target = "newPasswordHash", source = "newPassword")
    PasswordResetToken toDomain(ResetPasswordRequest dto);

    // =========== Domain -> DTO (если нужно) ===========

    default ForgotPasswordRequest toDto(EmailVerificationToken token) {
        ForgotPasswordRequest dto = new ForgotPasswordRequest();
        dto.setEmail(token.getEmail());
        return dto;
    }

    default ResetPasswordRequest toDto(PasswordResetToken token) {
        ResetPasswordRequest dto = new ResetPasswordRequest();
        dto.setToken(token.getTokenHash());
        dto.setNewPassword(token.getNewPasswordHash());
        return dto;
    }

    // =========== Фабричные методы для создания токенов ===========

    default EmailVerificationToken createEmailVerificationToken(String email, UUID userId) {
        return EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .email(email)
                .tokenHash(generateTokenHash())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
    }

    default PasswordResetToken createPasswordResetToken(String email, UUID userId) {
        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .email(email)
                .tokenHash(generateTokenHash())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
    }

    default RefreshToken createRefreshToken(String token, UUID userId, String deviceInfo) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(hashToken(token))
                .deviceInfo(deviceInfo)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
    }

    // =========== Вспомогательные методы ===========

    private String generateTokenHash() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String hashToken(String token) {
        // В реальности используй BCrypt или аналоги
        return "hashed_" + token;
    }

}
