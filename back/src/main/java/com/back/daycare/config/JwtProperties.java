package com.back.daycare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds,
        Rsa rsa
) {
    public record Rsa(Resource publicKey, Resource privateKey) {
    }
}

