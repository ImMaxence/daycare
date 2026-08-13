package com.back.daycare.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Charge la paire de clés RSA utilisée pour signer/vérifier les JWT.
 * Supporte 3 sources pour chaque clé (voir {@link JwtProperties.Rsa}) :
 * fichier classpath/disque (dev), PEM brut, ou PEM encodé en Base64 (recommandé en prod/Docker).
 */
@Configuration
@RequiredArgsConstructor
public class JwtKeyConfig {

    private static final String PEM_MARKER = "-----BEGIN";

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;

    @Bean
    public RSAPublicKey rsaPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decoded = Base64.getDecoder().decode(resolvePemContent(jwtProperties.rsa().publicKey()));
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        return (RSAPublicKey) publicKey;
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decoded = Base64.getDecoder().decode(resolvePemContent(jwtProperties.rsa().privateKey()));
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        return (RSAPrivateKey) privateKey;
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) {
        RSAKey rsaKey = new RSAKey.Builder(rsaPublicKey).privateKey(rsaPrivateKey).build();
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey rsaPublicKey) {
        return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
    }

    /**
     * Résout la valeur de config en Base64 "nu" du contenu de la clé, quel que soit le format d'entrée.
     */
    private String resolvePemContent(String value) throws IOException {
        String raw;
        if (value.startsWith("classpath:") || value.startsWith("file:")) {
            raw = readResource(resourceLoader.getResource(value));
        } else if (value.contains(PEM_MARKER)) {
            raw = value;
        } else {
            // Déjà en Base64 (ex: variable d'env Docker/Coolify) -> on décode le PEM encodé
            raw = new String(Base64.getDecoder().decode(value.trim()), StandardCharsets.UTF_8);
        }
        return stripPemHeaders(raw);
    }

    private String readResource(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String stripPemHeaders(String content) {
        return content
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
    }
}

