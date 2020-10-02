package com.github.ahimsaka.shorturl.shorturl.webconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Component
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {
    private final Logger log = LoggerFactory.getLogger(WebConfig.class);
    private final DatabaseHandler databaseHandler;

    WebConfig(DatabaseHandler databaseHandler){
        this.databaseHandler = databaseHandler;
    }

    @Bean
    public RouterFunction<?> router() {
        return route()
                .GET("/{extension}", databaseHandler::getUrl)
                .GET("/",  req -> ok().bodyValue("test success"))
                .PUT("/", databaseHandler::putURL)
                .build();
    }
    // Implement configuration methods...
}
