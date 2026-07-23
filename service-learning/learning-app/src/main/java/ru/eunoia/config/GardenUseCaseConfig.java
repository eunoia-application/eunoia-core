package ru.eunoia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eunoia.application.garden.port.in.MasteryUseCase;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;
import ru.eunoia.application.garden.service.MasteryUseCaseImpl;

/** Composition root контекста garden: ядро без Spring, use case собираем из портов здесь. */
@Configuration
public class GardenUseCaseConfig {

    @Bean
    public MasteryUseCase masteryUseCase(MasteryRepositoryPort mastery) {
        return new MasteryUseCaseImpl(mastery);
    }
}
