package projectlx.messaging.inbound.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface HelpSupportServiceClient {

    @GetMapping("/ldms-user-management/v1/system/help-support/support-ticket/by-username/{username}")
    JsonNode listSupportTicketsByUsername(
            @PathVariable("username") String username,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-user-management/v1/system/help-support/support-ticket/by-username/{username}")
    JsonNode createSupportTicketForUsername(
            @PathVariable("username") String username,
            @RequestBody JsonNode request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
