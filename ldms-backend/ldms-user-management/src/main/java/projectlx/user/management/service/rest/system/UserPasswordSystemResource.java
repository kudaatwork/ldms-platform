package projectlx.user.management.service.rest.system;

import org.springframework.web.bind.annotation.PostMapping;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.UserPasswordServiceProcessor;
import projectlx.user.management.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.utils.requests.ResetPasswordRequest;
import projectlx.user.management.utils.responses.UserPasswordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/system/user-password")
@Tag(name = "User Password System Resource", description = "Operations related to managing user passwords")
@RequiredArgsConstructor
public class UserPasswordSystemResource {

    private final UserPasswordServiceProcessor userPasswordServiceProcessor;

    @Auditable(action = "CHANGE_USER_PASSWORD")
    @PutMapping("/change-password")
    @Operation(summary = "Update user password", description = "Updates an existing user password by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User password updated/changed successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public UserPasswordResponse update(@Valid @RequestBody final ChangeUserPasswordRequest changeUserPasswordRequest,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                          @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                  defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userPasswordServiceProcessor.update(changeUserPasswordRequest, "SYSTEM", locale);
    }

    @Auditable(action = "RESET_PASSWORD")
    @PostMapping("/reset-password")
    @Operation(summary = "Reset user password",
            description = "Resets user password using the reset token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token or request"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserPasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return userPasswordServiceProcessor.resetPassword(resetPasswordRequest, locale, "SYSTEM");
    }
}
