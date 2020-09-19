package com.github.ahimsaka.shorturl.shorturl.webconfig;

import com.github.ahimsaka.shorturl.shorturl.dataobjects.UrlRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.ServerResponse.notFound;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

public class DatabaseHandler {
    Logger log = LoggerFactory.getLogger(DatabaseHandler.class);
    // should return ok() + url if existing record
    // or created() + url if new record created.
    public Mono<ServerResponse> postURL(ServerRequest request) {
        Mono<UrlRecord> urlRecord = request.bodyToMono(UrlRecord.class);
        // check for url existing
        // if record exists:
            // get record from db and
            // return ok().body(Mono.just(returnedRecord, String.class));
        // else:
            // return created().build(repository.saveRecord(urlRecord))
        return ok().bodyValue("Post Test"); // or ok().build() when more complex.
    }

    public Mono<ServerResponse> getUrl(ServerRequest request) {
        Mono<UrlRecord> urlRecord = request.bodyToMono(UrlRecord.class);
        // check if url exists
        // if exists:
            //   ServerResponse.temporaryRedirect(URI.create(TargetUrl)).build()
        log.info("Not Found goin up");
        return notFound().build();
    }



}
