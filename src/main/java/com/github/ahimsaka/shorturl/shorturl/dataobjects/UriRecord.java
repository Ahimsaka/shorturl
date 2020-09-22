package com.github.ahimsaka.shorturl.shorturl.dataobjects;

import lombok.Data;

@Data
public class UriRecord {
    private String uri;
    private String extension;
    private int hits;
}

