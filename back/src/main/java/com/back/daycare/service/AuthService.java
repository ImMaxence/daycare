package com.back.daycare.service;

import com.back.daycare.config.JwtProperties;
import com.back.daycare.dto.request.LoginRequest;
import com.back.daycare.dto.request.RefreshTokenRequest;
import com.back.daycare.dto.response.TokenResponse;
import com.back.daycare.entity.User;
import com.back.daycare.exception.InvalidTokenException;
import com.back.daycare.exception.ResourceNotFoundException;
import com.back.daycare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setLastConnexion(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        return new TokenResponse(accessToken, refreshToken, jwtProperties.accessTokenTtlSeconds());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(request.refreshToken());
        } catch (JwtException e) {
            throw new InvalidTokenException("Refresh token invalide ou expiré");
        }

        if (!TOKEN_TYPE_REFRESH.equals(jwt.getClaimAsString(CLAIM_TOKEN_TYPE))) {
            throw new InvalidTokenException("Le token fourni n'est pas un refresh token");
        }

        String username = jwt.getSubject();
        userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        String accessToken = jwtService.generateAccessToken(username);
        String newRefreshToken = jwtService.generateRefreshToken(username);

        return new TokenResponse(accessToken, newRefreshToken, jwtProperties.accessTokenTtlSeconds());
    }
}

