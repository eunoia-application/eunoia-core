package ru.eunoia.application.domain.event;

/** Доменное событие auth, публикуемое наружу (в Kafka). Sealed — адаптер разбирает исчерпывающе. */
public sealed interface DomainEvent permits UserRegistered, UserDeleted {
}
