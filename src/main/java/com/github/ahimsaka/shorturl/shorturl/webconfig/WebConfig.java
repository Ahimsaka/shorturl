package com.github.ahimsaka.shorturl.shorturl.webconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {
    Logger log = LoggerFactory.getLogger(WebConfig.class);

    DatabaseHandler databaseHandler = new DatabaseHandler();

    @Bean
    public RouterFunction<?> router() {
        return route()
                .GET("/{extension}", databaseHandler::getUrl)
                .GET("/",  req -> ok().bodyValue("test success"))
                .POST("/", databaseHandler::postURL)
                .build();
    }
    // Implement configuration methods...
}
