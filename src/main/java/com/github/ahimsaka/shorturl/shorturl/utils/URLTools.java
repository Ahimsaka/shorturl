package com.github.ahimsaka.shorturl.shorturl.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class URLTools {
    /*
    Class checks validity of submitted URLs and resolves them to final location
    to reduce data duplication.
     */
    private static final Logger log = LoggerFactory.getLogger(URLTools.class);
    private static final WebClient webClient = WebClient.create();

    public static Mono<String> checkRedirects(String url) {
        /*
         If temporary redirect, store requested URL. if permanent, store final Location.
         */
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        String standardURL;
        try {
            URI checkUri = new URI(url).normalize();

            if (!checkUri.isAbsolute()) checkUri = new URI("https://" + url);

            standardURL = checkUri.toURL().toString();
        } catch (MalformedURLException | URISyntaxException e) {
            return Mono.error(e);
        }

        return webClient.get()
                .uri(standardURL)
                .exchange()
                .onErrorReturn(ClientResponse.create(HttpStatus.NOT_FOUND).build())
                .map(response -> Pair.of(response.statusCode(), response.headers().asHttpHeaders()))
                .flatMap(pair -> {
                    if (pair.getFirst().equals(HttpStatus.TEMPORARY_REDIRECT))
                        return Mono.just(standardURL);
                    else if (pair.getFirst().is3xxRedirection()) {
                        if (standardURL.equals(pair.getSecond().getLocation()))
                            return Mono.just(standardURL);
                        else return checkRedirects(pair.getSecond().getLocation().toString());
                    }
                    else if (pair.getFirst().isError())
                        return Mono.error(new Error(pair.getFirst().getReasonPhrase()));
                    else return Mono.just(standardURL);
                });

    }
}
