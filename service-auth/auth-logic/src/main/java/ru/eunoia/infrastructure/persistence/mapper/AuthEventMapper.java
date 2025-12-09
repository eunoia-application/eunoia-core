package ru.eunoia.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.eunoia.domain.model.AuthEvent;
import ru.eunoia.domain.model.entity.AuthEventEntity;

@Mapper(componentModel = "spring")
public interface AuthEventMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "details", source = "details")
    @Mapping(target = "success", source = "success")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "responseTimeMs", source = "responseTimeMs")
    @Mapping(target = "resource", source = "resource")
    @Mapping(target = "httpMethod", source = "httpMethod")
    @Mapping(target = "correlationId", source = "correlationId")
    @Mapping(target = "serviceVersion", source = "serviceVersion")
    @Mapping(target = "environment", source = "environment")
    @Mapping(target = "createdAt", source = "createdAt")
    AuthEvent toDomain(AuthEventEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "details", source = "details")
    @Mapping(target = "success", source = "success")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "responseTimeMs", source = "responseTimeMs")
    @Mapping(target = "resource", source = "resource")
    @Mapping(target = "httpMethod", source = "httpMethod")
    @Mapping(target = "correlationId", source = "correlationId")
    @Mapping(target = "serviceVersion", source = "serviceVersion")
    @Mapping(target = "environment", source = "environment")
    @Mapping(target = "createdAt", source = "createdAt")
    AuthEventEntity toEntity(AuthEvent domain);

    // Маппинг enum EventType
    default AuthEvent.EventType mapEventType(AuthEventEntity.EventType entityType) {
        if (entityType == null) return null;
        return AuthEvent.EventType.valueOf(entityType.name());
    }

    default AuthEventEntity.EventType mapEventType(AuthEvent.EventType domainType) {
        if (domainType == null) return null;
        return AuthEventEntity.EventType.valueOf(domainType.name());
    }

}
