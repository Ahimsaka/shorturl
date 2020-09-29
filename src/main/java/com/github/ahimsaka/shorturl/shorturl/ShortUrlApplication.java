package com.github.ahimsaka.shorturl.shorturl;

import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class ShortUrlApplication implements CommandLineRunner {
	private static Logger log = LoggerFactory.getLogger(ShortUrlApplication.class);

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



