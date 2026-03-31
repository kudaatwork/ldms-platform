package projectlx.co.zw.shared_library.business.logic.api;

import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;

public interface JwtService {
    String generateToken(UserDetails userDetails);
    String generateRefreshToken(UserDetails userDetails);
    boolean isTokenValid(String token, UserDetails user);
    String extractUsername(String token);
    boolean isExpired(String token);
    List<String> extractRoles(String token);
}
