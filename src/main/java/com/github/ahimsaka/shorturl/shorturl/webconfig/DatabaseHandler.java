package com.github.ahimsaka.shorturl.shorturl.webconfig;

import com.github.ahimsaka.shorturl.shorturl.r2dbc.URLRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import com.github.ahimsaka.shorturl.shorturl.utils.URLTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;

import static com.github.ahimsaka.shorturl.shorturl.utils.URLTools.checkRedirects;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class DatabaseHandler {
    Logger log = LoggerFactory.getLogger(DatabaseHandler.class);
    private final JdbcTemplate jdbcTemplate;
    private final ExtensionGenerator extensionGenerator;
    private final WebClient webClient;

    DatabaseHandler(JdbcTemplate jdbcTemplate, ExtensionGenerator extensionGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.extensionGenerator = extensionGenerator;

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().followRedirect(false)
                )).build();

    }

    public Mono<ServerResponse> getAllByUser(ServerRequest request) {
        return ok().bodyValue("Implement me, jackhog.");
    }

    /* return ok() + url if existing record
     or created() + url if new record created. */
    public Mono<ServerResponse> putURL(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(URLTools::checkRedirects)
                .flatMap(this::getOrInsertByURL)
                .flatMap(pair -> switch (pair.getFirst()) {
                    case "insert" -> created(URI.create(pair.getSecond().getExtension())).build();
                    case "select" -> ok().bodyValue(pair.getSecond().getExtension());
                    default -> status(500).build();
                })
                .onErrorResume(e -> badRequest().bodyValue(e.getMessage()));
    }

    public Mono<ServerResponse> getURLByExtension(ServerRequest request) {
        String extension = request.pathVariable("extension");
        int update = jdbcTemplate.update(
                    "UPDATE url_record\n" +
                            "SET hits = hits + 1\n" +
                            "WHERE extension = ?", extension);

        if (update == 0) return badRequest().bodyValue(String.format("No record found for '%s'.", extension));

        return temporaryRedirect(jdbcTemplate.queryForObject(
                "SELECT url FROM url_record\n" +
                        "WHERE extension = ?", URI.class, extension)).build()
                .onErrorResume(e -> status(HttpStatus.BAD_REQUEST)
                        .bodyValue(String.format("Request for '%s' threw exception %s.", extension, e)));
    }

    private Mono<Pair<String, URLRecord>> getOrInsertByURL(String url) {
        String extension = "";
        try {
            extension = jdbcTemplate.queryForObject(
                    "SELECT extension FROM url_record WHERE url = ?",
                    String.class,
                    url
            );
        } catch (Exception e) {
            URLRecord record = new URLRecord(extensionGenerator.generate(), url, 0);
            jdbcTemplate.update("INSERT INTO url_record (url, extension, hits)" +
                            "values (?, ?, ?)",
                    record.getUrl(), record.getExtension(), record.getHits());
            return Mono.just(Pair.of("insert", record));
        }

        return Mono.just(Pair.of("select", new URLRecord(extension, url, 0)));
    }
}

