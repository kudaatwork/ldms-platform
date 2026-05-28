package projectlx.user.authentication.service.business.logic.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.user.authentication.service.clients.UserManagementServiceClient;

@ExtendWith(MockitoExtension.class)
class LoginIdentifierResolverTest {

    @Mock
    private UserManagementServiceClient userManagementServiceClient;

    @Test
    void resolveUsername_byEmail_returnsCanonicalUsername() {
        UserDto dto = new UserDto();
        dto.setUsername("platform.admin");
        dto.setEmail("admin@projectlx.co.zw");
        UserResponse response = new UserResponse();
        response.setSuccess(true);
        response.setUserDto(dto);

        when(userManagementServiceClient.findByPhoneNumberOrEmail("admin@projectlx.co.zw")).thenReturn(response);

        assertEquals(
                "platform.admin",
                LoginIdentifierResolver.resolve(userManagementServiceClient, "admin@projectlx.co.zw").username());
    }

    @Test
    void resolveUsername_byUsername_returnsUsername() {
        UserDto dto = new UserDto();
        dto.setUsername("jdoe");
        UserResponse response = new UserResponse();
        response.setSuccess(true);
        response.setUserDto(dto);

        when(userManagementServiceClient.findByUsername("jdoe")).thenReturn(response);

        assertEquals("jdoe", LoginIdentifierResolver.resolve(userManagementServiceClient, "jdoe").username());
    }

    @Test
    void resolveUsername_unknownEmail_returnsNull() {
        UserResponse response = new UserResponse();
        response.setSuccess(false);

        when(userManagementServiceClient.findByPhoneNumberOrEmail("missing@projectlx.co.zw")).thenReturn(response);

        assertNull(LoginIdentifierResolver.resolve(userManagementServiceClient, "missing@projectlx.co.zw"));
    }
}
