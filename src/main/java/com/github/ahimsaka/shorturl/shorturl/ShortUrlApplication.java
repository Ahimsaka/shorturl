package com.github.ahimsaka.shorturl.shorturl;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.github.ahimsaka.shorturl.shorturl.dataobjects.UrlRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;


import java.util.Random;

@SpringBootApplication
@EnableConfigurationProperties(ExtensionGenerator.class)
public class ShortUrlApplication implements CommandLineRunner {
	private static Logger log = LoggerFactory.getLogger(ShortUrlApplication.class);
	@Autowired
	ExtensionGenerator extensionGenerator;

	public static void main(String[] args) {
		SpringApplication.run(ShortUrlApplication.class, args);
		log.info("Started");

	}

    /*@Configuration
    @ConfigurationProperties(prefix = "r2dbc")
    public class ApplicationConfiguration extends AbstractR2dbcConfiguration {

        @Override
        @Bean
        public ConnectionFactory connectionFactory() {
            return…;
        }
    }*/

	@Override
	public void run(String... args) {
		log.info("EXECUTING : command line runner");

	}

}



