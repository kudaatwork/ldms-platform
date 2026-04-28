package projectlx.co.zw.shared_library.business.logic.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms}")
    private Long jwtAccessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private Long jwtRefreshTokenExpirationMs;

    public JwtServiceImpl(Environment environment) {
        this.jwtSecret = environment.getProperty("jwt.secret");
        if (this.jwtSecret == null) {
            throw new IllegalStateException("JWT Secret is not provided. Please define 'jwt.secret' in your configuration.");
        }
    }

    public String getSecret() {
        return jwtSecret;
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtAccessTokenExpirationMs);
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtRefreshTokenExpirationMs);
    }

    private String buildToken(UserDetails userDetails, long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails user) {
        String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isExpired(token);
    }

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    @Override
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = decodeSecret(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is blank.");
        }
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            try {
                return Base64.getUrlDecoder().decode(secret);
            } catch (IllegalArgumentException ignoredAgain) {
                // Fallback for plain-text secrets used in some environments.
                byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
                if (raw.length >= 32) {
                    return raw;
                }
                try {
                    return MessageDigest.getInstance("SHA-256").digest(raw);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("SHA-256 algorithm unavailable for JWT key derivation.", e);
                }
            }
        }
    }
}
