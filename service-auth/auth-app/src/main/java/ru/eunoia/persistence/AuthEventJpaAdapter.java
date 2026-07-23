package ru.eunoia.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.port.out.AuthEventRepositoryPort;
import ru.eunoia.persistence.mapper.AuthEventMapper;
import ru.eunoia.persistence.repository.AuthEventJpaRepository;

/** JPA-адаптер аудита: пишет события. id руками не ставим — генерит @GeneratedValue при insert. */
@Component
@RequiredArgsConstructor
public class AuthEventJpaAdapter implements AuthEventRepositoryPort {

    private final AuthEventJpaRepository jpaRepository;
    private final AuthEventMapper mapper;

    @Override
    @Transactional
    public AuthEvent save(AuthEvent event) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(event)));
    }
}
