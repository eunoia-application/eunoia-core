package ru.eunoia.application.port.out;

import ru.eunoia.application.domain.event.DomainEvent;

/** Публикация доменных событий наружу (адаптер шлёт в Kafka). */
public interface EventPublisherPort {

    void publish(DomainEvent event);
}
