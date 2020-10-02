package com.github.ahimsaka.shorturl.shorturl.webconfig;

import com.github.ahimsaka.shorturl.shorturl.r2dbc.URLRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

        // Attempt to create table. If it already exists, log the error and move on.
        databaseClient.execute(
                "CREATE TABLE url_record(\n" +
                        "extension CHAR(" + extensionGenerator.getLength() + ") PRIMARY KEY,\n" +
                        "url VARCHAR(255) UNIQUE,\n" +
                        "hits INT)").fetch().rowsUpdated().doOnError(e -> log.info(e.getMessage())).subscribe();

    }

    // should return ok() + url if existing record
    // or created() + url if new record created.
    public Mono<ServerResponse> putURL(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(url -> getByURL(url))
                .flatMap(pair -> {
                    if (pair.getFirst().equals("insert"))
                        return created(URI.create(pair.getSecond().getExtension())).build();
                    else if (pair.getFirst().equals("select"))
                        return ok().bodyValue(pair.getSecond().getExtension());
                    else
                        return status(500).build();
                });
                /*.onErrorResume(Exception.class,
                        req -> getByURL(request)
                                .flatMap(result ->
                                        ok().header("Location", result.getExtension()).bodyValue("Body for showin'"))));


        request)
                .log()
                .flatMap(result -> {
                    log.info(result.toString());
                    return result.getHits() == 0
                            ? created(URI.create(result.getExtension())).build()
                            : ok().header("Location", result.getExtension()).build();
                });*/
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
                        "RETURNING *")
                .fetch()
                .one()
                .switchIfEmpty(Mono.error(Error::new))
                .flatMap(result -> temporaryRedirect(URI.create(result.get("url").toString())).build())
                .onErrorResume(e -> status(HttpStatus.BAD_REQUEST).bodyValue(String.format("No record found for '%s'.", extension)));
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

    private Mono<Pair<String, URLRecord>> getByURL(String url){
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
                        Integer.valueOf(resultMap.get("hits").toString()))));
    }
}

