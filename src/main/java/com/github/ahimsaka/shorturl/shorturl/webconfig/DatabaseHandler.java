package com.github.ahimsaka.shorturl.shorturl.webconfig;

import com.github.ahimsaka.shorturl.shorturl.utils.URLRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import com.github.ahimsaka.shorturl.shorturl.utils.URLTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
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

    Mono<SecurityContext> context = ReactiveSecurityContextHolder.getContext();

    DatabaseHandler(JdbcTemplate jdbcTemplate, ExtensionGenerator extensionGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.extensionGenerator = extensionGenerator;
    }
    /*

    Creating a new ShortURL extension.

     */
    public Mono<ServerResponse> putURL(ServerRequest request){
        /*
        return ok() + url if existing record or created() + url if new record
        created.
         */
        return request.bodyToMono(String.class)
                .flatMap(URLTools::checkRedirects)
                .flatMap(this::getOrInsertByURL)
                .delayUntil(this::insertURLToUsersLinks)
                .flatMap(pair -> switch (pair.getFirst()) {
                    case "insert" -> created(URI.create(pair.getSecond().getExtension())).build();
                    case "select" -> ok().bodyValue(pair.getSecond().getExtension());
                    default -> status(500).build();
                })
                .onErrorResume(e -> badRequest().bodyValue(e.getMessage()));
    }

    public Mono<OAuth2User> insertURLToUsersLinks(Pair<String, URLRecord> pair){
        /*
        If User is signed in, adds extension to the join table. If not, does nothing.
         */
        return context.map(SecurityContext::getAuthentication)
                .doOnNext(auth -> log.info("after auth"))
                .map(Authentication::getPrincipal)
                .cast(OAuth2User.class)
                .doOnNext(user -> {
                    jdbcTemplate.update("INSERT INTO users_links (username, extension)" +
                                    "VALUES (?, ?)",
                            user.getAttribute("email"),
                            pair.getSecond().getExtension());
                });
    }

    public Mono<Pair<String, URLRecord>> getOrInsertByURL(String url) {
        /*
        If URL already listed in database, return the extension. Otherwise,
        insert and return.
         */
        String extension;
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
    /*

    Requesting an existing record.

     */
    public Mono<ServerResponse> getURLByExtension(ServerRequest request) {
        String extension = request.pathVariable("extension");
        int update = jdbcTemplate.update(
                    "UPDATE url_record\n" +
                            "SET hits = hits + 1\n" +
                            "WHERE extension = ?", extension);

        if (update == 0)
            return badRequest().bodyValue(String.format("No record found for '%s'.", extension));

        return temporaryRedirect(jdbcTemplate.queryForObject(
                "SELECT url FROM url_record\n" +
                        "WHERE extension = ?", URI.class, extension)).build()
                .onErrorResume(e -> status(HttpStatus.BAD_REQUEST)
                        .bodyValue(String.format("Request for '%s' threw exception %s.", extension, e)));
    }
    /*

    OAuth2 Support

     */
    public void upsertUser(OAuth2User user) {
        /*
        OAuth2 users won't be registering, so we need to make sure they're in
        the database.

        Currently only 2 OAuth2 providers accepted (Google and Github).
        Google oauth2user objects include an "iss" key/value pair, so we can
        deduce that OAuth2Users without that key came from Github.

        Must be changed if further providers are added.
         */
        String issuer = user.getAttributes()
                .getOrDefault("iss", "github")
                .toString();

        jdbcTemplate.update("INSERT INTO users (username, issuer, enabled) " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT DO NOTHING;",
                user.getAttribute("email"),
                issuer,
                true
        );
    }

    public Mono<ServerResponse> getAllByUser(ServerRequest request) {
        /*
        When a user logs in via OAuth, provide them a list of links they've previously
        registered.
         */
        return context.map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(OAuth2User.class)
                .doOnNext(this::upsertUser)
                .map(user -> {
                    String email = user.getAttribute("email").toString();
                    return jdbcTemplate.query("SELECT * FROM users_links INNER JOIN url_record " +
                                    "ON users_links.extension = url_record.extension " +
                                    "WHERE users_links.username = ?",
                            new Object[]{email},
                            (rs, rowNum) -> {
                                URLRecord urlRecord = new URLRecord();
                                urlRecord.setExtension(rs.getString("extension"));
                                urlRecord.setUrl(rs.getString("url"));
                                urlRecord.setHits(rs.getInt("hits"));
                                return urlRecord;
                            });
                })
                .flatMap(records -> ok().bodyValue(records.toString()));
    }
}

