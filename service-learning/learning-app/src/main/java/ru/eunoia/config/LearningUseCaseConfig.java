package ru.eunoia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;
import ru.eunoia.application.knowledge.port.out.LexiconRepositoryPort;
import ru.eunoia.application.learning.port.in.LearningQueryUseCase;
import ru.eunoia.application.learning.service.LearningQueryUseCaseImpl;

/** Composition root фасада учёбы: собираем LearningQueryUseCase из out-портов двух контекстов. */
@Configuration
public class LearningUseCaseConfig {

    @Bean
    public LearningQueryUseCase learningQueryUseCase(LexiconRepositoryPort lexicon,
                                                     MasteryRepositoryPort mastery) {
        return new LearningQueryUseCaseImpl(lexicon, mastery);
    }
}
