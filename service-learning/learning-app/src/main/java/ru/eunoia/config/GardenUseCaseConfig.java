package ru.eunoia.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eunoia.application.garden.port.in.ActivityUseCase;
import ru.eunoia.application.garden.port.in.MasteryUseCase;
import ru.eunoia.application.garden.port.out.ActivityRepositoryPort;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;
import ru.eunoia.application.garden.service.ActivityUseCaseImpl;
import ru.eunoia.application.garden.service.MasteryUseCaseImpl;

/** Composition root контекста garden: ядро без Spring, use case собираем из портов здесь. */
@Configuration
public class GardenUseCaseConfig {

    @Bean
    public ActivityUseCase activityUseCase(ActivityRepositoryPort activity) {
        return new ActivityUseCaseImpl(activity, Clock.systemDefaultZone());
    }

    @Bean
    public MasteryUseCase masteryUseCase(MasteryRepositoryPort mastery, ActivityUseCase activityUseCase) {
        return new MasteryUseCaseImpl(mastery, activityUseCase);
    }
}
