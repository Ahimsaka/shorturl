package com.github.ahimsaka.shorturl.shorturl.webconfig;

import com.github.ahimsaka.shorturl.shorturl.r2dbc.URLRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class DatabaseHandler {
    Logger log = LoggerFactory.getLogger(DatabaseHandler.class);
    private final DatabaseClient databaseClient;
    private final ExtensionGenerator extensionGenerator;
    private final WebClient webClient;

    DatabaseHandler(DatabaseClient databaseClient, ExtensionGenerator extensionGenerator){
        this.databaseClient = databaseClient;
        this.extensionGenerator = extensionGenerator;
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().followRedirect(false)
                )).build();

        // Attempt to create table. If it already exists, log the error and move on.
        databaseClient.execute(
                "CREATE TABLE url_record(\n" +
                        "extension CHAR(" + extensionGenerator.getLength() + ") PRIMARY KEY,\n" +
                        "url VARCHAR(255) UNIQUE,\n" +
                        "hits INT)")
                .fetch()
                .rowsUpdated()
                // Throws error if table already exists, so log the error and move on.
                .doOnError(e -> log.error(e.getMessage()))
                .subscribe();
    }

    /* return ok() + url if existing record
     or created() + url if new record created. */
    public Mono<ServerResponse> putURL(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(url -> checkRedirects(standardizeURL(url)))
                .flatMap(this::getOrInsertByURL)
                .flatMap(pair -> switch (pair.getFirst()) {
                    case "insert" -> created(URI.create(pair.getSecond().getExtension())).build();
                    case "select" -> ok().bodyValue(pair.getSecond().getExtension());
                    case "bad request" -> badRequest().bodyValue("Unable to resolve provided URL");
                    default -> status(500).build();
                });
    }

    public Mono<ServerResponse> getURLByExtension(ServerRequest request) {
        String extension = request.pathVariable("extension");
        return databaseClient.execute(
                "UPDATE url_record\n" +
                        "SET hits = hits + 1\n" +
                        "WHERE extension = '" + extension + "' \n" +
                        "RETURNING *")
                .fetch()
                .one()
                .switchIfEmpty(Mono.error(Error::new))
                .flatMap(result -> temporaryRedirect(URI.create(result.get("url").toString())).build())
                .onErrorResume(e -> status(HttpStatus.BAD_REQUEST)
                        .bodyValue(String.format("No record found for '%s'.", extension)));
    }

    private Mono<Pair<String, URLRecord>> getOrInsertByURL(String url){
        if (url.equals("bad request")) return Mono.just(Pair.of(url, new URLRecord()));
        return databaseClient.select()
                .from(URLRecord.class)
                .matching(where("url").is(url))
                .fetch()
                .one()
                .map(result -> Pair.of("select", result))
                .filter(pair -> pair.getSecond().getExtension().length() == extensionGenerator.getLength())
                .switchIfEmpty(insertURL(url));
    }

    private Mono<Pair<String, URLRecord>> insertURL(String url){
        return databaseClient.insert()
                .into(URLRecord.class)
                .using(new URLRecord(extensionGenerator.generate(), url, 0))
                .fetch()
                .one()
                .map(resultMap -> Pair.of("insert", new URLRecord(resultMap.get("extension").toString(),
                        resultMap.get("url").toString(),
                        Integer.parseInt(resultMap.get("hits").toString()))));
    }

    // Make sure that URL formatting is valid
    private String standardizeURL(String url) {
        log.info(url);
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        URL checkUrl;
        try {
            URI checkUri = new URI(url).normalize();
            if (!checkUri.isAbsolute()) checkUri = new URI("https://" + url);
            checkUrl = checkUri.toURL();
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            return "bad request";
        }
        return checkUrl.toString();
    }

    // If temporary redirect, store requested URL. if permanent, store final Location.
    private Mono<String> checkRedirects(String standardURL) {
        if (standardURL.equals("bad request")) return Mono.just("bad request");
        return webClient.get()
                .uri(standardURL)
                .exchange()
                .onErrorReturn(ClientResponse.create(HttpStatus.NOT_FOUND).build())
                .map(response -> Pair.of(response.statusCode(), response.headers().asHttpHeaders()))
                .flatMap(pair -> {
                    if (pair.getFirst().equals(HttpStatus.TEMPORARY_REDIRECT))
                        return Mono.just(standardURL);
                    else if (pair.getFirst().is3xxRedirection())
                        return checkRedirects(pair.getSecond().getLocation().toString());
                    else if (pair.getFirst().isError())
                        return Mono.just("bad request");
                    else return Mono.just(standardURL);
                });

    }
}

