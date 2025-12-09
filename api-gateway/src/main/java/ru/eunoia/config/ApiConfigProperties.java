package ru.eunoia.config;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application")
@RequiredArgsConstructor
@Data
public class ApiConfigProperties {

    private final String authHost;

    private final List<ApiRoute> apiRoutes;

}
