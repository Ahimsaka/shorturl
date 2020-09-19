package com.github.ahimsaka.shorturl.shorturl;

import com.github.ahimsaka.shorturl.shorturl.dataobjects.UrlRecord;
import com.github.ahimsaka.shorturl.shorturl.utils.ExtensionGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

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

	@Test
	void contextLoads() {
	}
}

@SpringBootTest
class DatabaseConnectionTests {
    private static Logger log = LoggerFactory.getLogger(DatabaseConnectionTests.class);

}

@SpringBootTest
class UrlRecordDataObjectTests {
    private static Logger log = LoggerFactory.getLogger(UrlRecordDataObjectTests.class);



    @Test
    void UrlRecordDataObjectBasicTest() {
        UrlRecord urlRecord = new UrlRecord();
        urlRecord.setExtension("testExtension");
        Assert.isTrue(urlRecord.getExtension().equals("testExtension"),
                String.format("urlRecord.getExtension() returned %s, expected %s",
                        urlRecord.getExtension(),
                        "testExtension"));

        urlRecord.setUrl("testFinalUrl");
        Assert.isTrue(urlRecord.getUrl().equals("testFinalUrl"),
                String.format("urlRecord.getFinalUrl() returned %s, expected %s",
                        urlRecord.getUrl(),
                        "testFinalUrl"));

        urlRecord.setHits(42);
        Assert.isTrue(urlRecord.getHits() == 42,
                String.format("urlRecord.getHits() returned %d, expected %d",
                        urlRecord.getHits(),
                        42));
    }

}

@SpringBootTest
class ExtensionGeneratorTests {
    private static Logger log = LoggerFactory.getLogger(ExtensionGeneratorTests.class);

    @Autowired
    ExtensionGenerator extensionGenerator;

    @Test
    void ExtensionGeneratorConfigTest() {
        Assert.isInstanceOf(Integer.class, extensionGenerator.getLength(), "ExtensionGenerator.length is not an integer value.");
        Assert.notNull(extensionGenerator.getChars(), "ExtensionGenerator.chars is NULL.");
        Assert.hasText(Arrays.toString(extensionGenerator.getChars()), "ExtensionGenerator.chars contains no text.");

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
                                +" Please adjust application.properties and remove this character."
                        , url));
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
        String[] extensions = new String[200];
        for (int i = 0; i < 200; i++) {
            extensions[i] = extensionGenerator.generate();
        }
        Set<String> uniqueExtensions = new HashSet<>(Arrays.asList(extensions));
        isTrue(uniqueExtensions.size() == extensions.length,
                String.format("Found only %d unique extensions out of %d extensions generated.",
                        uniqueExtensions.size(),
                        extensions.length));
    }

}



