package com.github.ahimsaka.shorturl.shorturl.r2dbc;

import lombok.Data;

@Data
public class UriRecord {
    private String uri;
    private String extension;
    private int hits;
}

