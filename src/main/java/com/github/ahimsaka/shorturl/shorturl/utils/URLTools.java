package com.github.ahimsaka.shorturl.shorturl.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class URLTools {
    Logger log = LoggerFactory.getLogger(URLTools.class);
    WebClient webClient = WebClient.create();

    public Mono<String> resolveURL(String url) {
        /* TO DO: implement method which requests URL and retrieves the final address
            whence the request resolves. Commented code below has the Mono/WebClient logic
            working but does not handle redirects correctly yet.
        */
        return Mono.just(url);
        /*
       return webClient.get()
               .uri(url)
               .exchange()
               .flatMap(clientResponse -> {
                    if (clientResponse.rawStatusCode() == 302) {
                           return Mono.just(url);
                    }
                    if (clientResponse.statusCode().is3xxRedirection()) return Mono.just(clientResponse.headers().asHttpHeaders().get("Location").get(0));
                    if (clientResponse.statusCode().isError()) {
                        clientResponse.body((clientHttpResponse, context) -> {
                            return clientHttpResponse.getBody();
                        });
                        return clientResponse.bodyToMono(String.class);
                    }
                    return Mono.just(url);
                });
                */
    }
}
