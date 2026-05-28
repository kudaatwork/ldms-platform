package projectlx.co.zw.shared_library.business.logic.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import projectlx.co.zw.shared_library.utils.config.JwtProperties;
import projectlx.co.zw.shared_library.utils.security.JwtSigningKeys;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;

    @Override
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, Map.of());
    }

    @Override
    public String generateToken(UserDetails userDetails, Map<String, Object> additionalClaims) {
        return buildToken(userDetails, jwtProperties.getAccessTokenExpirationMs(), additionalClaims);
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtProperties.getRefreshTokenExpirationMs(), Map.of());
    }

    private String buildToken(UserDetails userDetails, long expirationMillis, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>();
        if (additionalClaims != null) {
            additionalClaims.forEach((key, value) -> {
                if (key != null && value != null) {
                    claims.put(key, value);
                }
            });
        }
        claims.put(
                "roles",
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(getSignKey())
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
        return normalizeRoleCodes(claims.get("roles"));
    }

    private static List<String> normalizeRoleCodes(Object rolesClaim) {
        if (rolesClaim == null) {
            return List.of();
        }
        if (rolesClaim instanceof String rolesString) {
            if (!StringUtils.hasText(rolesString)) {
                return List.of();
            }
            return java.util.Arrays.stream(rolesString.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(JwtServiceImpl::stripRolePrefix)
                    .toList();
        }
        if (rolesClaim instanceof Collection<?> collection) {
            List<String> roles = new ArrayList<>();
            for (Object item : collection) {
                if (item == null) {
                    continue;
                }
                String role = stripRolePrefix(item.toString().trim());
                if (StringUtils.hasText(role)) {
                    roles.add(role);
                }
            }
            return roles;
        }
        return List.of();
    }

    private static String stripRolePrefix(String role) {
        if (role.regionMatches(true, 0, "ROLE_", 0, 5)) {
            return role.substring(5);
        }
        return role;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignKey() {
        return JwtSigningKeys.hmacSha256Key(jwtProperties.getSecret());
    }
}
