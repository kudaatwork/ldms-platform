package projectlx.user.authentication.service.business.logic.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import projectlx.co.zw.shared_library.business.logic.api.CustomUserDetailsService;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.authentication.service.utils.enums.I18Code;
import projectlx.user.authentication.service.model.Address;
import projectlx.user.authentication.service.model.User;
import projectlx.user.authentication.service.model.UserAccount;
import projectlx.user.authentication.service.model.UserGroup;
import projectlx.user.authentication.service.model.UserPassword;
import projectlx.user.authentication.service.model.UserPreferences;
import projectlx.user.authentication.service.model.UserRole;
import projectlx.user.authentication.service.model.UserSecurity;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.user.authentication.service.clients.UserManagementServiceClient;

@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService {

    private final UserManagementServiceClient userManagementServiceClient;
    private final ModelMapper modelMapper;
    private final MessageService messageService;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserResponse userResponse = userManagementServiceClient.findByUsername(username);

        if (!userResponse.isSuccess()) {
            String detail = messageService.getMessage(
                    I18Code.MESSAGE_USER_NOT_FOUND_FOR_USERNAME.getCode(), new String[] {username}, Locale.getDefault());
            throw new UsernameNotFoundException(detail);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserAccount userAccount = modelMapper.map(userResponse.getUserDto().getUserAccountDto(), UserAccount.class);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserSecurity userSecurity = modelMapper.map(userResponse.getUserDto().getUserSecurityDto(), UserSecurity.class);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserGroup userGroup = modelMapper.map(userResponse.getUserDto().getUserGroupDto(), UserGroup.class);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Address address = modelMapper.map(userResponse.getUserDto().getAddressDto(), Address.class);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserPreferences userPreferences = modelMapper.map(userResponse.getUserDto().getUserPreferencesDto(), UserPreferences.class);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserPassword userPassword = modelMapper.map(userResponse.getUserDto().getUserPasswordDto(), UserPassword.class);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<UserRole> userRoleList = modelMapper.map(userResponse.getUserDto().getUserGroupDto().getUserRoleDtoSet(), new TypeToken<List<UserRole>>(){}.getType());
        Set<UserRole> userRoleSet = new HashSet<>(userRoleList);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        User user = modelMapper.map(userResponse.getUserDto(), User.class);
        user.setUserAccount(userAccount);
        user.setUserSecurity(userSecurity);
        user.setUserGroup(userGroup);
        user.getUserGroup().setUserRoles(userRoleSet);
        user.setAddress(address);
        user.setUserPreferences(userPreferences);
        user.setUserPassword(userPassword);

        // Map roles from UserGroup -> UserRoles
        Set<GrantedAuthority> authorities = user.getUserGroup().getUserRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getUserPassword().getPassword(),
                authorities
        );
    }
}

