package com.github.ahimsaka.shorturl.shorturl.webconfig;

import com.github.ahimsaka.shorturl.shorturl.dao.URLRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import com.github.ahimsaka.shorturl.shorturl.utils.URLTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;

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

    public Mono<ServerResponse> getAllByUser(Object principal) {
        DefaultOAuth2User user = (DefaultOAuth2User) principal;
        log.info(user.getAttributes().toString());
        return ok().bodyValue(user.getAttributes().toString());
        //DefaultOidcUser user = (DefaultOidcUser) principal;
        //log.info(user.getName() + "\n" + user.getPreferredUsername() + "\n" + user.getIdToken().toString());
        //return ok().bodyValue(((DefaultOidcUser) principal).getUserInfo().getFullName());

        /*jdbcTemplate.execute("SELECT n2.extension, n2.url " +
                "FROM (SELECT extension FROM user_links WHERE username = ?) n1" +
                "INNER JOIN (select extension, url from url_record) n2", principal)*/
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

