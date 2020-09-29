package com.github.ahimsaka.shorturl.shorturl;

import com.github.ahimsaka.shorturl.shorturl.r2dbc.UriRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import com.github.ahimsaka.shorturl.shorturl.utils.URLTools;
import com.github.ahimsaka.shorturl.shorturl.webconfig.DatabaseHandler;
import com.github.ahimsaka.shorturl.shorturl.webconfig.WebConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootTest
class ShortUrlApplicationTests {
	private static Logger log = LoggerFactory.getLogger(ShortUrlApplicationTests.class);
}

@SpringBootTest
class DatabaseConnectionTests {
    private static Logger log = LoggerFactory.getLogger(DatabaseConnectionTests.class);
}

@ExtendWith(SpringExtension.class)
@WebFluxTest
@Import(DatabaseHandler.class)
class DatabaseHandlerTests {
    private static Logger log = LoggerFactory.getLogger(DatabaseHandlerTests.class);

    @Autowired
    WebTestClient webClient;

    @Test
    void postValidUrls(){
        webClient.post()
                .uri("/")
                .bodyValue("https://google.com/")
                .exchange().expectStatus().isOk();

        webClient.post()
                .uri("/")
                .bodyValue("http://google.com/")
                .exchange().expectStatus().isOk();

        webClient.post()
                .uri("/")
                .bodyValue("https://www.google.com/")
                .exchange().expectStatus().isOk();

        webClient.post()
                .uri("/")
                .bodyValue("www.google.com/")
                .exchange().expectStatus().isOk();

        webClient.post()
                .uri("/")
                .bodyValue("google.com/")
                .exchange().expectStatus().isOk();
    }

    @Test
    void postInvalidUrls() {
        webClient.post()
                .uri("/")
                .bodyValue("htttps://www.google.com/")
                .exchange().expectStatus().isBadRequest();
    }
}

@SpringBootTest
class URLToolsTests {
    /* implement tests when method is fixed */
    @Autowired
    URLTools urlTools;

}

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = WebConfig.class)
class WebConfigTests {
    /* class tests WebConfig.class to ensure proper routing of requests/parameters/extensions.
        Handling of invalid parameters/extensions takes place in DatabaseHandler, so testing
        for those edge cases will occur in DatabaseHandlerTests and Integration tests.
     */

    private static Logger log = LoggerFactory.getLogger(WebConfigTests.class);

    @Autowired
    private WebTestClient webClient;

    @MockBean
    DatabaseHandler databaseHandler;

    @Test
    void contextLoads() {
    }

    @Test
    void requestRootTest() {
        // test response from "/", which does not call the Handler function.
        webClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("test success");
    }

    @Test
    void getWithExtensionParamTest() {
        // test that a get request with extension passes path variable accurately
        Mockito.when(databaseHandler.getUrl(Mockito.argThat(request -> request.pathVariable("extension").equals("testExt"))))
                .thenReturn(ok().bodyValue("Test Passed"));

        webClient.get()
                .uri("/testExt")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).equals("Test Passed");

        Mockito.verify(databaseHandler, times(1)).getUrl(ArgumentMatchers.any());
    }

    @Test
    void postRequestParamTest() {
        // test that WebConfig.route() passes correct parameter to the Handler function.
        Mockito.when(databaseHandler.postUrl(ArgumentMatchers.any()))
                .thenReturn(ok().bodyValue("Test Passed"));

        webClient.post()
                .uri("/")
                .bodyValue("testBody")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).equals("Test Passed");

        Mockito.verify(databaseHandler, times(1)).postUrl(ArgumentMatchers.any());
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
    ExtensionGenerator extensionGenerator;

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

class UriRecordDataObjectTests {
    /* tests basic functions of UriRecord.class POJOs
        No logic is included in that class, so these tests should only fail as a result of
        improper Spring or lombok configuration.
     */
    private static Logger log = LoggerFactory.getLogger(UriRecordDataObjectTests.class);

    @Test
    void UriRecordDataObjectBasicTest() {
        UriRecord uriRecord = new UriRecord();
        uriRecord.setExtension("testExtension");
        Assert.isTrue(uriRecord.getExtension().equals("testExtension"),
                String.format("uriRecord.getExtension() returned %s, expected %s",
                        uriRecord.getExtension(),
                        "testExtension"));

        uriRecord.setUri("testFinalUrl");
        Assert.isTrue(uriRecord.getUri().equals("testFinalUrl"),
                String.format("uriRecord.getFinalUrl() returned %s, expected %s",
                        uriRecord.getUri(),
                        "testFinalUrl"));

        uriRecord.setHits(42);
        Assert.isTrue(uriRecord.getHits() == 42,
                String.format("uriRecord.getHits() returned %d, expected %d",
                        uriRecord.getHits(),
                        42));
    }
}


