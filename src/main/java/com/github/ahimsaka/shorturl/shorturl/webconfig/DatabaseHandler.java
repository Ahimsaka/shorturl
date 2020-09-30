package com.github.ahimsaka.shorturl.shorturl.webconfig;

import com.github.ahimsaka.shorturl.shorturl.r2dbc.URLRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class DatabaseHandler {
    Logger log = LoggerFactory.getLogger(DatabaseHandler.class);
    private final DatabaseClient databaseClient;
    private final ExtensionGenerator extensionGenerator;

    DatabaseHandler(DatabaseClient databaseClient, ExtensionGenerator extensionGenerator){
        this.databaseClient = databaseClient;
        this.extensionGenerator = extensionGenerator;
    }

    // should return ok() + url if existing record
    // or created() + url if new record created.
    public Mono<ServerResponse> postUrl(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(requestedURL -> databaseClient.insert()
                        .into(URLRecord.class)
                        .using(new URLRecord(extensionGenerator.generate(), requestedURL, 0))
                        .fetch()
                        .one()
                ).flatMap(result ->
                        created(URI.create(result.get("extension").toString()))
                                .bodyValue(String.format("shortURL for %s created at extension '/%s'.",
                                        result.get("url"), result.get("extension")))
                );
        // check for url existing
        // if record exists:
            // get record from db and
            // return ok().body(Mono.just(returnedRecord, String.class));
        // else:
            // return created().build(repository.saveRecord(urlRecord))
        // ; // or ok().build() when more complex.
    }

    public Mono<ServerResponse> getUrl(ServerRequest request) {
        String extension = request.pathVariable("extension");
        log.info(extension);
        return databaseClient.execute(
                "UPDATE url_record\n" +
                        "SET hits = hits + 1\n" +
                        "WHERE extension = '" + extension + "' \n" +
                        //"WHERE extension = :extension\n" +
                        "RETURNING *")
                //.bind("extension", extension)
                .fetch()
                .one()
                .switchIfEmpty(Mono.error(Error::new))
                .flatMap(result -> temporaryRedirect(URI.create(result.get("url").toString())).build())
                .onErrorResume(e -> status(HttpStatus.I_AM_A_TEAPOT).bodyValue(String.format("No record found for '%s'.", extension)));
        // check if url exists
        // if exists:
            //   ServerResponse.temporaryRedirect(URI.create(TargetUrl)).build()
        //return notFound().build();
    }

    // For de-duplication, strip leading http or https input and trailing / from urls.
    private Mono<ServerResponse> checkUrlAndReturn(String url) {
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        URL checkUrl;
        try {
            URI checkUri = new URI(url).normalize();
            if (!checkUri.isAbsolute()) checkUri = new URI("https://" + url);

            checkUrl = checkUri.toURL();
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            return badRequest().bodyValue(String.format("Error: %s; Message: %s", e.getClass(), e.getMessage()));
        }

        String[] splitUrl = checkUrl.toString().split("\\.");

        if (splitUrl[0].equals("http://")
                || splitUrl[0].equals("http://www")
                || splitUrl[0].equals("https://www")) splitUrl[0] = "https://";

        return ok().bodyValue(String.join(".", splitUrl));
    }
}
