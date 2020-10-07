package com.github.ahimsaka.shorturl.shorturl.r2dbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
//@ToString
@NoArgsConstructor
@AllArgsConstructor
public class URLRecord {
    @Id
    private String extension;
    private String url;
    private int hits;
}

