package com.back.daycare.service;

import com.back.daycare.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String generateAccessToken(String username) {
        return generateToken(username, jwtProperties.accessTokenTtlSeconds(), TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, jwtProperties.refreshTokenTtlSeconds(), TOKEN_TYPE_REFRESH);
    }

    private String generateToken(String username, long ttlSeconds, String type) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .subject(username)
                .claim(CLAIM_TOKEN_TYPE, type)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}

