package projectlx.user.authentication.service.business.logic.support;

import projectlx.co.zw.shared_library.utils.dtos.UserDto;

/**
 * Canonical username and optional profile loaded while resolving the sign-in identifier.
 */
public record ResolvedLogin(String username, UserDto userDto) {
}
