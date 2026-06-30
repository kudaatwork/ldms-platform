package projectlx.co.zw.notifications.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.notifications.service.processor.api.PlatformUserNotificationProcessor;
import projectlx.co.zw.notifications.utils.responses.PlatformUserNotificationResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-notifications/v1/frontend/platform-user-notification")
@Tag(name = "Platform User Notification", description = "In-app notification bell inbox for the platform portal")
public class PlatformUserNotificationFrontendResource {

    private final PlatformUserNotificationProcessor processor;

    public PlatformUserNotificationFrontendResource(PlatformUserNotificationProcessor processor) {
        this.processor = processor;
    }

    @Auditable(action = "LIST_PLATFORM_USER_NOTIFICATION_INBOX")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/inbox")
    @Operation(summary = "List active inbox notifications for the signed-in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inbox retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public PlatformUserNotificationResponse inbox(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return processor.listInbox(locale, username);
    }

    @Auditable(action = "DISMISS_PLATFORM_USER_NOTIFICATION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/dismiss/{id}")
    @Operation(summary = "Dismiss a single inbox notification")
    public PlatformUserNotificationResponse dismiss(
            @PathVariable("id") Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return processor.dismiss(id, locale, username);
    }

    @Auditable(action = "DISMISS_ALL_PLATFORM_USER_NOTIFICATIONS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/dismiss-all")
    @Operation(summary = "Dismiss all inbox notifications for the signed-in user")
    public PlatformUserNotificationResponse dismissAll(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return processor.dismissAll(locale, username);
    }
}
