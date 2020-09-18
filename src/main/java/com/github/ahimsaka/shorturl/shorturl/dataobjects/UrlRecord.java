package com.github.ahimsaka.shorturl.shorturl.dataobjects;

import lombok.Data;

@Data
public class UrlRecord {
    private String finalUrl;
    private String extension;
    private int hits;
}
