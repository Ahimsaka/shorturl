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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

	@Override
	public void run(String... args) {
		log.info("EXECUTING : command line runner");

		UrlRecord urlRecord = new UrlRecord();
		urlRecord.setHits(42);
		urlRecord.setFinalUrl("finalUrl");
		urlRecord.setExtension(extensionGenerator.generate());


		ConnectionFactory connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///test?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

		DatabaseClient client = DatabaseClient.create(connectionFactory);

		client.execute("CREATE TABLE url_record" +
				"(final_url VARCHAR(255) PRIMARY KEY," +
				"extension VARCHAR(255)," +
				"hits INT)")
				.fetch()
				.rowsUpdated()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		client.insert()
				.into(UrlRecord.class)
				.using(urlRecord)
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		client.select()
				.from(UrlRecord.class)
				.fetch()
				.first()
				.doOnNext(it -> log.info(it.toString()))
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();
	}

}



