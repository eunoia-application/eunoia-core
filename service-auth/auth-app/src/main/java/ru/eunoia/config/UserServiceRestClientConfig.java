package ru.eunoia.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserServiceRestClientConfig {

    @Bean(name = "userServiceRestClient")
    @LoadBalanced
    public RestClient userServiceRestClient() {
        return RestClient.builder()
                .baseUrl("http://service-user") // Имя в Eureka
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

}
