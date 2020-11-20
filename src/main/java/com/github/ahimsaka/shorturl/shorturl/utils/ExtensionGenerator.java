package com.github.ahimsaka.shorturl.shorturl.utils;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jnanoid")
public class ExtensionGenerator {
	/*
	This class allows us to set the length and valid characters for our extensions
	in the separate application.properties configuration file.
	 */
	private int length;
	private char[] chars;

	public String generate() {
		return NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, chars, length);
	}
}
