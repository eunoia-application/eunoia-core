package ru.eunoia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eunoia.application.port.in.ProfileLifecycleUseCase;
import ru.eunoia.application.port.in.ProfileUseCase;
import ru.eunoia.application.port.out.ProfileRepositoryPort;
import ru.eunoia.application.services.ProfileLifecycleUseCaseImpl;
import ru.eunoia.application.services.ProfileUseCaseImpl;

/** Composition root: ядро (user-logic) без Spring, поэтому use case'ы собираем здесь из портов. */
@Configuration
public class UseCaseConfig {

    @Bean
    public ProfileUseCase profileUseCase(ProfileRepositoryPort profiles) {
        return new ProfileUseCaseImpl(profiles);
    }

    @Bean
    public ProfileLifecycleUseCase profileLifecycleUseCase(ProfileRepositoryPort profiles) {
        return new ProfileLifecycleUseCaseImpl(profiles);
    }
}
