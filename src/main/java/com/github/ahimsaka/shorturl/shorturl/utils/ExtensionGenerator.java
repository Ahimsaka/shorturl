package com.github.ahimsaka.shorturl.shorturl.utils;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jnanoid")
public class ExtensionGenerator {
	private int length;
	private char[] chars;

	public String generate() {
		return NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, chars, length);
	}
}
