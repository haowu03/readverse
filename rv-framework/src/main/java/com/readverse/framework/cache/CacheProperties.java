package com.readverse.framework.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rv.cache")
public class CacheProperties {
    private boolean enabled = true;
    private long localTtlSeconds = 60;
    private long chapterTtlSeconds = 1800;
    private long nullTtlSeconds = 120;
    private boolean bloomEnabled = true;
}
