package distributedSystem.UserManagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class HttpClientsConfig {

    @Bean
    public WebClient authWebClient(
            @Value("${services.auth.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl) // internal DNS in Docker
                .build();
    }
}