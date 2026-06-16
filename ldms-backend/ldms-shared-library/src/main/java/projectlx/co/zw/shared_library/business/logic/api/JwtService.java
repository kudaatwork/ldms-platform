package projectlx.co.zw.shared_library.business.logic.api;

import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import java.util.Map;

public interface JwtService {
    String generateToken(UserDetails userDetails);

    /**
     * Access token with extra claims (e.g. userId, email, organizationId) merged into the JWT payload.
     */
    String generateToken(UserDetails userDetails, Map<String, Object> additionalClaims);

    String generateRefreshToken(UserDetails userDetails);
    boolean isTokenValid(String token, UserDetails user);
    String extractUsername(String token);
    Long extractOrganizationId(String token);
    boolean isExpired(String token);
    List<String> extractRoles(String token);
}
