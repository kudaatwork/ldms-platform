package projectlx.user.authentication.service.business.logic.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.business.logic.api.CustomUserDetailsService;
import projectlx.co.zw.shared_library.utils.dtos.UserAccountDto;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.dtos.UserGroupDto;
import projectlx.co.zw.shared_library.utils.dtos.UserPasswordDto;
import projectlx.co.zw.shared_library.utils.dtos.UserRoleDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.user.authentication.service.clients.UserManagementServiceClient;
import projectlx.user.authentication.service.utils.enums.I18Code;

@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService {

    private final UserManagementServiceClient userManagementServiceClient;
    private final MessageService messageService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserResponse userResponse = loadUserProfile(username);
        UserDto userDto = userResponse.getUserDto();

        String resolvedUsername = userDto.getUsername();
        if (!StringUtils.hasText(resolvedUsername)) {
            throw userNotFound(username);
        }

        UserPasswordDto passwordDto = userDto.getUserPasswordDto();
        if (passwordDto == null || !StringUtils.hasText(passwordDto.getPassword())) {
            throw new UsernameNotFoundException("User account has no password configured");
        }

        Set<GrantedAuthority> authorities = resolveAuthorities(userDto.getUserGroupDto());

        boolean accountLocked = isAccountLocked(userDto.getUserAccountDto());

        return org.springframework.security.core.userdetails.User.builder()
                .username(resolvedUsername.trim())
                .password(passwordDto.getPassword())
                .disabled(accountLocked)
                .accountLocked(accountLocked)
                .authorities(authorities)
                .build();
    }

    private static boolean isAccountLocked(UserAccountDto userAccountDto) {
        return userAccountDto != null && Boolean.TRUE.equals(userAccountDto.getIsAccountLocked());
    }

    private UserResponse loadUserProfile(String username) {
        try {
            UserResponse userResponse = userManagementServiceClient.findByUsername(username);
            if (userResponse == null || !userResponse.isSuccess() || userResponse.getUserDto() == null) {
                throw userNotFound(username);
            }
            return userResponse;
        } catch (UsernameNotFoundException ex) {
            throw ex;
        } catch (FeignException ex) {
            throw new UsernameNotFoundException(
                    "User management service is unavailable; cannot load user profile", ex);
        }
    }

    private UsernameNotFoundException userNotFound(String username) {
        String detail = messageService.getMessage(
                I18Code.MESSAGE_USER_NOT_FOUND_FOR_USERNAME.getCode(),
                new String[] {username},
                Locale.getDefault());
        return new UsernameNotFoundException(detail);
    }

    private static Set<GrantedAuthority> resolveAuthorities(UserGroupDto userGroupDto) {
        if (userGroupDto == null || userGroupDto.getUserRoleDtoSet() == null) {
            return Set.of();
        }
        List<UserRoleDto> roles = userGroupDto.getUserRoleDtoSet();
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (UserRoleDto roleDto : roles) {
            if (roleDto == null || !StringUtils.hasText(roleDto.getRole())) {
                continue;
            }
            String code = roleDto.getRole().trim().toUpperCase();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + code));
        }
        return authorities;
    }
}
