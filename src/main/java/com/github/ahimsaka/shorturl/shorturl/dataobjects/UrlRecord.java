package com.github.ahimsaka.shorturl.shorturl.dataobjects;

import lombok.Data;

@Data
public class UrlRecord {
    private String url;
    private String extension;
    private int hits;
}
