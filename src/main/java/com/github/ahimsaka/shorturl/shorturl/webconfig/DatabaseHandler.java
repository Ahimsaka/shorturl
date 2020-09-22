package com.github.ahimsaka.shorturl.shorturl.webconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class DatabaseHandler {
    Logger log = LoggerFactory.getLogger(DatabaseHandler.class);
    // should return ok() + url if existing record
    // or created() + url if new record created.
    public Mono<ServerResponse> postUrl(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(this::checkUrlAndReturn);
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
        // check if url exists
        // if exists:
            //   ServerResponse.temporaryRedirect(URI.create(TargetUrl)).build()
        log.info("Not Found goin up");
        return notFound().build();
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
