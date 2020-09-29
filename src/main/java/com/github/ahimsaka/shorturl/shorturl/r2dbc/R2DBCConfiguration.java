package com.github.ahimsaka.shorturl.shorturl.r2dbc;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@Configuration
@ConfigurationProperties(prefix = "r2dbc")
@EnableR2dbcRepositories
@Data
public class R2DBCConfiguration extends AbstractR2dbcConfiguration {
    private static Logger log = LoggerFactory.getLogger(R2DBCConfiguration.class);
    String host;
    int port;
    String user;
    String password;
    String database;

    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(DRIVER, "postgresql")
                .option(HOST, host)
                .option(PORT, port)  // optional, defaults to 5432
                .option(USER, user)
                .option(PASSWORD, password)
                .option(DATABASE, database)  // optional
                .build();

        log.info(options.toString());
        return ConnectionFactories.get(options);
    }
}