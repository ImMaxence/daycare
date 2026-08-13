package com.back.daycare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds,
        String secret
) {
}

