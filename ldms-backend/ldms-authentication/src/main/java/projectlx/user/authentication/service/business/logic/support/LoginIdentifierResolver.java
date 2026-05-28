package projectlx.user.authentication.service.business.logic.support;

import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.user.authentication.service.clients.UserManagementServiceClient;

/**
 * Resolves the sign-in identifier (email or username) to the canonical LDMS {@code username}
 * used by Spring Security and JWT issuance.
 */
public final class LoginIdentifierResolver {

    private LoginIdentifierResolver() {
    }

    /**
     * @param loginId value from the sign-in form (email address or username)
     * @return resolved login, or {@code null} when the identifier cannot be resolved
     */
    public static ResolvedLogin resolve(UserManagementServiceClient userManagementServiceClient, String loginId) {
        if (!StringUtils.hasText(loginId)) {
            return null;
        }
        String trimmed = loginId.trim();
        if (trimmed.contains("@")) {
            return resolveByEmail(userManagementServiceClient, trimmed);
        }
        return resolveByLoginName(userManagementServiceClient, trimmed);
    }

    private static ResolvedLogin resolveByEmail(
            UserManagementServiceClient userManagementServiceClient, String email) {
        UserResponse response = userManagementServiceClient.findByPhoneNumberOrEmail(email);
        return toResolvedLogin(response);
    }

    private static ResolvedLogin resolveByLoginName(
            UserManagementServiceClient userManagementServiceClient, String loginName) {
        UserResponse byUsername = userManagementServiceClient.findByUsername(loginName);
        ResolvedLogin resolved = toResolvedLogin(byUsername);
        if (resolved != null) {
            return resolved;
        }
        UserResponse byAlternate = userManagementServiceClient.findByPhoneNumberOrEmail(loginName);
        return toResolvedLogin(byAlternate);
    }

    private static ResolvedLogin toResolvedLogin(UserResponse response) {
        if (response == null || !response.isSuccess() || response.getUserDto() == null) {
            return null;
        }
        String username = response.getUserDto().getUsername();
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return new ResolvedLogin(username.trim(), response.getUserDto());
    }
}
