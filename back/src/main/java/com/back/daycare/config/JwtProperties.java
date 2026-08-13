package com.back.daycare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds,
        Rsa rsa
) {
    /**
     * publicKey/privateKey acceptent 3 formats :
     *  - "classpath:certs/private.pem" ou "file:/path/private.pem" (dev local)
     *  - contenu PEM brut (-----BEGIN...-----) si la variable d'env supporte le multiligne
     *  - contenu PEM encodé en Base64 (recommandé pour une variable d'env Docker/Coolify sur une seule ligne)
     */
    public record Rsa(String publicKey, String privateKey) {
    }
}

