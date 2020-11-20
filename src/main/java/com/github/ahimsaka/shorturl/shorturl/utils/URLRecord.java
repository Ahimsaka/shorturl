package com.github.ahimsaka.shorturl.shorturl.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class URLRecord {
    @Id
    private String extension;
    private String url;
    private int hits;
}

