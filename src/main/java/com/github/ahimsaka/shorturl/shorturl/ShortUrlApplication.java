package com.github.ahimsaka.shorturl.shorturl;

import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(ExtensionGenerator.class)
public class ShortUrlApplication implements CommandLineRunner {
	private static Logger log = LoggerFactory.getLogger(ShortUrlApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ShortUrlApplication.class, args);
		log.info("Server is up and running, big hoss.");
	}

	@Override
	public void run(String... args) {
	}

}





