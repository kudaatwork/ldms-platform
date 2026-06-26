package projectlx.user.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.HelpSupportServiceProcessor;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.responses.HelpSupportResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/system/help-support")
@Tag(name = "Help & Support System Resource", description = "Inter-service support ticket access for LDMS Agent")
@RequiredArgsConstructor
public class HelpSupportSystemResource {

    private final HelpSupportServiceProcessor helpSupportServiceProcessor;

    @Auditable(action = "SYSTEM_LIST_SUPPORT_TICKETS_BY_USERNAME")
    @GetMapping("/support-ticket/by-username/{username}")
    @Operation(summary = "List support tickets for a portal user (agent / inter-service)")
    public ResponseEntity<HelpSupportResponse> listSupportTicketsByUsername(
            @PathVariable String username,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        HelpSupportResponse response = helpSupportServiceProcessor.listMySupportTickets(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SYSTEM_CREATE_SUPPORT_TICKET_FOR_USERNAME")
    @PostMapping("/support-ticket/by-username/{username}")
    @Operation(summary = "Create a support ticket for a portal user (agent / inter-service)")
    public ResponseEntity<HelpSupportResponse> createSupportTicketForUsername(
            @PathVariable String username,
            @Valid @RequestBody CreateSupportTicketRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        HelpSupportResponse response = helpSupportServiceProcessor.createSupportTicket(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
