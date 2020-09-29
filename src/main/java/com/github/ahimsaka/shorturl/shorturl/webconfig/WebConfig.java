package com.github.ahimsaka.shorturl.shorturl.webconfig;

import com.github.ahimsaka.shorturl.shorturl.r2dbc.R2DBCConfiguration;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@EnableWebFlux
@RequiredArgsConstructor
@EnableConfigurationProperties({ExtensionGenerator.class, R2DBCConfiguration.class})
public class WebConfig implements WebFluxConfigurer {
    private final Logger log = LoggerFactory.getLogger(WebConfig.class);
    private final DatabaseHandler databaseHandler;

    @Bean
    public RouterFunction<?> router() {
        return route()
                .GET("/{extension}", databaseHandler::getUrl)
                .GET("/",  req -> ok().bodyValue("test success"))
                .POST("/", databaseHandler::postUrl)
                .build();
    }
    // Implement configuration methods...
}
