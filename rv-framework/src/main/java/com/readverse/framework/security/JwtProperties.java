package com.readverse.framework.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rv.jwt")
public class JwtProperties {
    private String secret;
    private long accessTtlSeconds = 1800;
    private long refreshTtlSeconds = 604800;
    private String issuer = "readverse";
}
