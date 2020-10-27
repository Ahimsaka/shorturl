package com.github.ahimsaka.shorturl.shorturl;

import com.github.ahimsaka.shorturl.shorturl.r2dbc.URLRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import com.github.ahimsaka.shorturl.shorturl.utils.URLTools;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import reactor.test.StepVerifier;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.fail;
import static org.springframework.util.Assert.isTrue;

@SpringBootTest
class ShortUrlApplicationTests {
	private static Logger log = LoggerFactory.getLogger(ShortUrlApplicationTests.class);
}


@SpringBootTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseHandlerTests {
    private static Logger log = LoggerFactory.getLogger(DatabaseHandlerTests.class);

    @Autowired
    WebTestClient webClient;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterAll
    void clearTestDB() {
        jdbcTemplate.execute("DELETE FROM url_record");
    }

    @Test
    void putValidURL(){
        webClient.put()
                .uri("/")
                .bodyValue("https://google.com/")
                .exchange().expectStatus().isCreated().expectHeader().exists("Location");
    }

    @Test
    void putInvalidURL() {
        webClient.put()
                .uri("/")
                .bodyValue("htttps://www.google.com/")
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void getValidExtension(){
        try {
            jdbcTemplate.update("INSERT INTO url_record VALUES ('http://www.bing.com/', '12345678', 0);");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        webClient.get()
                .uri("/12345678")
                .exchange()
                .expectStatus().isTemporaryRedirect();
    }
    @Test
    void getInvalidExtension(){
        webClient.get()
                .uri("/87654321")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().equals("No record found for '87654321'.");
    }

}

@SpringBootTest
class URLToolsTests {
    @Test
    void testValidURL(){
        StepVerifier.create(URLTools.checkRedirects("http://www.google.com"))
                .assertNext(url -> url.equals("http://www.googel.com"))
                .verifyComplete();
    }

    @Test
    void testRedirectsToSameLocation(){
        String urlA = URLTools.checkRedirects("http://www.google.com").block();
        String urlB = URLTools.checkRedirects("http://google.com").block();

        Assert.isTrue(urlA.equals(urlB), String.format("%s should be equal to %s", urlA, urlB));
    }

    @Test
    void failInvalidURL(){
        StepVerifier.create(URLTools.checkRedirects("localhost:8080/invalid_extension"))
                .expectError(MalformedURLException.class)
                .verify();

        StepVerifier.create(URLTools.checkRedirects("<>\\^`{|}"))
                .expectError(URISyntaxException.class)
                .verify();
    }

}

@SpringBootTest
class ExtensionGeneratorTests {
    /* test ExtensionGenerator.class to ensure that application.properties settings are
        properly picked up. These tests will also prevent packaging if those properties are
        set with values that would generate illegal or unsafe extension strings.
     */
    private static Logger log = LoggerFactory.getLogger(ExtensionGeneratorTests.class);

    @Autowired
    ExtensionGenerator extensionGenerator = new ExtensionGenerator();

    @Test
    void ExtensionGeneratorConfigTest() {
        // test that both properties are set, and set to valid response.
        Assert.isInstanceOf(Integer.class, extensionGenerator.getLength(), "ExtensionGenerator.length is not an integer value.");
        Assert.notNull(extensionGenerator.getChars(), "ExtensionGenerator.chars is NULL.");
        Assert.hasText(Arrays.toString(extensionGenerator.getChars()), "ExtensionGenerator.chars contains no text.");

        /* testing each character in application.properties -> jnanian.chars individually is slightly longer than
            converting it to a string and testing the string as a valid URI, but allows us to specify which character
            is invalid in the failure response.
         */
        for (int i = 0; i < extensionGenerator.getChars().length; i++) {
            String thisChar = String.valueOf(extensionGenerator.getChars()[i]);
            String reserved = ":/?#\"[]@!$&'()*+,;=";
            Assert.isTrue(!reserved.contains(thisChar),
                    String.format("The character '%s' is a reserved character and should not be included in randomly "
                                    + "assigned URL extensions. Please adjust application.properties and remove"
                                    + " this character.",
                            thisChar));

            String url = String.format("https://wwww.google.com/%s", thisChar);
            try {
                URL obj = new URL(url);
                obj.toURI();
            } catch (URISyntaxException | MalformedURLException e) {
                fail(String.format("ExtensionGenerator.chars contains %s, which is not a valid URL character."
                                +" Please adjust application.properties and remove this character.", url));
            }
        }
    }

    @Test
    void ExtensionGeneratorOutputTest() {

        String extension = extensionGenerator.generate();
        Assert.isTrue(extension.length() == extensionGenerator.getLength(), "Extension is the wrong length.");
        for (int i = 0; i < extension.length(); i++) {
            Assert.isTrue(String.valueOf(extensionGenerator.getChars()).contains(String.valueOf(extension.charAt(i))),
                    String.format("Character %s found in extension is not in the list of approved characters set in application.properties.",
                            extension.charAt(i)));
        }
    }

    @Test
    void ExtensionGeneratorUniquenessTest() {
        /* Test ensures that ExtensionGenerator.generate() creates a new string every
            time it is called. Also serves as "dummy check" to ensure that jnanian configuration
            set in application.properties are set to sensible values.

            May fail when nothing is wrong in very rare cases. Testing should be run again to confirm
            when this test fails.
         */
        String[] extensions = new String[1000];
        for (int i = 0; i < 1000; i++) {
            extensions[i] = extensionGenerator.generate();
        }
        Set<String> uniqueExtensions = new HashSet<>(Arrays.asList(extensions));
        isTrue(extensions.length - uniqueExtensions.size() < 2,
                String.format("Found only %d unique extensions out of %d extensions generated. " +
                                "This test may fail randomly. Please run tests. If this fails more than " +
                                "once, adjust the jnanian variables in application.properties to achieve " +
                                "collision-safe settings. See README.md for additional resources.",
                        uniqueExtensions.size(),
                        extensions.length));
    }

}

class URLRecordDataObjectTests {
    /* tests basic functions of URLRecord.class POJOs
        No logic is included in that class, so these tests should only fail as a result of
        improper Spring or lombok configuration.
     */
    private static Logger log = LoggerFactory.getLogger(URLRecordDataObjectTests.class);

    @Test
    void UriRecordDataObjectBasicTest() {
        URLRecord URLRecord = new URLRecord();
        URLRecord.setExtension("testExtension");
        Assert.isTrue(URLRecord.getExtension().equals("testExtension"),
                String.format("URLRecord.getExtension() returned %s, expected %s",
                        URLRecord.getExtension(),
                        "testExtension"));

        URLRecord.setUrl("testFinalUrl");
        Assert.isTrue(URLRecord.getUrl().equals("testFinalUrl"),
                String.format("URLRecord.getFinalUrl() returned %s, expected %s",
                        URLRecord.getUrl(),
                        "testFinalUrl"));

        URLRecord.setHits(42);
        Assert.isTrue(URLRecord.getHits() == 42,
                String.format("URLRecord.getHits() returned %d, expected %d",
                        URLRecord.getHits(),
                        42));
    }
}


