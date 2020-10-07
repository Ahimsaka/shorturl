package com.github.ahimsaka.shorturl.shorturl.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Component
public class URLTools {
    private static final Logger log = LoggerFactory.getLogger(URLTools.class);
    private static final WebClient webClient = WebClient.create();

    public static Mono<String> standardizeAndResolveURL(String url) {
        return checkRedirects(standardizeURL(url));
    }

    // Make sure that URL formatting is valid
    private static String standardizeURL(String url) {
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
    private static Mono<String> checkRedirects(String url) {
        String standardURL = standardizeURL(url);
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
