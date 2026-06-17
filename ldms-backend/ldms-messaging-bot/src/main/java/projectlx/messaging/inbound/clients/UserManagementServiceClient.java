package projectlx.messaging.inbound.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

import java.util.Locale;

public interface UserManagementServiceClient {

    @GetMapping("/ldms-user-management/v1/system/user/session-profile-by-username/{username}")
    UserResponse findSessionProfileByUsername(
            @PathVariable("username") String username,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    default UserResponse findSessionProfileByUsername(String username) {
        return findSessionProfileByUsername(username, Locale.ENGLISH);
    }
}
