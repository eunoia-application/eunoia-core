package ru.eunoia.controllers;

import java.net.ConnectException;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Order(-2)
@Component
@Log4j2
public class ErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        var bufferFactory = exchange.getResponse().bufferFactory();
        var response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        if (ex instanceof ConnectException) {
            log.error("Target host connection error", ex);
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            var dataBuffer = bufferFactory.wrap("Target host connection error".getBytes());
            return response.writeWith(Mono.just(dataBuffer));
        } else {
            log.error("Unhandled exception", ex);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            var dataBuffer = bufferFactory.wrap("Unhandled error".getBytes());
            return response.writeWith(Mono.just(dataBuffer));
        }
    }
}
