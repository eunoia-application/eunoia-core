package ru.eunoia.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Log4j2
public class XrequestFilter implements GatewayFilter {

    private static final String HEADER_X_REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var guid = java.util.UUID.randomUUID().toString();
        log.info("requestId:{}", guid);

        var request = exchange.getRequest().mutate()
                .header(HEADER_X_REQUEST_ID, guid)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }
}
