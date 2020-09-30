package com.github.ahimsaka.shorturl.shorturl;

import com.github.ahimsaka.shorturl.shorturl.r2dbc.URLRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.test.StepVerifier;

@SpringBootApplication
@EnableConfigurationProperties(ExtensionGenerator.class)
public class ShortUrlApplication implements CommandLineRunner {
	private static Logger log = LoggerFactory.getLogger(ShortUrlApplication.class);
	private DatabaseClient databaseClient;

	@Value("${jnanoid.length}")
	String extensionLength;

	ShortUrlApplication(DatabaseClient databaseClient){
		this.databaseClient = databaseClient;
	}

	public static void main(String[] args) {
		SpringApplication.run(ShortUrlApplication.class, args);
		log.info("Started");
	}

	@Override
	public void run(String... args) {
		log.info("EXECUTING : command line runner");

		databaseClient.execute(
				"CREATE TABLE url_record(\n" +
				"extension CHAR(" + extensionLength + ") PRIMARY KEY,\n" +
				"url VARCHAR(255) UNIQUE,\n" +
				"hits INT)").fetch().rowsUpdated().doOnError(e -> log.info(e.getMessage())).subscribe();
	}

}





