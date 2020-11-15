package com.github.ahimsaka.shorturl.shorturl.webconfig;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.*;
import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;

@Component
@EnableWebFlux
@ConfigurationProperties(prefix="webconfig")
@Setter
public class WebConfig implements WebFluxConfigurer {
    private final Logger log = LoggerFactory.getLogger(WebConfig.class);
    private final DatabaseHandler databaseHandler;
    private String putPath;
    private String getPath;

    WebConfig(DatabaseHandler databaseHandler){
        this.databaseHandler = databaseHandler;
    }

    Mono<SecurityContext> context = ReactiveSecurityContextHolder.getContext();

    @Bean
    public RouterFunction<?> router() {
        return route()
                .GET(getPath + "user", databaseHandler::getAllByUser)
                .PUT(getPath + "user", databaseHandler::putURLAsUser)
                .GET(getPath + "{extension}", databaseHandler::getURLByExtension)
                .PUT(putPath, databaseHandler::putURL)
                .build();
    }

    // Implement configuration methods...
}
