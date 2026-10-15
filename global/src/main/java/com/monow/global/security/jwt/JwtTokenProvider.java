package com.monow.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "type";

    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String createAccessToken(Long userId, String role) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(jwtProperties.accessTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(jwtProperties.refreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();

    }

    public boolean validateToken(String token) {

        try {
           getClaims(token);
           return true;

        } catch (JwtException | IllegalArgumentException exception) {
            return false;

        }

    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);

        return Long.valueOf(claims.getSubject());
    }

    public String getRole(String token) {
        Claims claims = getClaims(token);

        return claims.get(ROLE_CLAIM, String.class);

    }

    public boolean isAccessToken(String accessToken) {
        Claims claims = getClaims(accessToken);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        return ACCESS_TOKEN_TYPE.equals(tokenType);

    }

    public boolean isRefreshToken(String refreshToken) {
        Claims claims = getClaims(refreshToken);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        return REFRESH_TOKEN_TYPE.equals(tokenType);

    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
