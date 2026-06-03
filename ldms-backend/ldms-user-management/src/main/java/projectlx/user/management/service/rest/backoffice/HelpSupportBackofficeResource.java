package projectlx.user.management.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.HelpSupportServiceProcessor;
import projectlx.user.management.utils.responses.HelpSupportResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/backoffice/help-support")
@Tag(name = "Help & Support Backoffice Resource", description = "Support ticket queue for LX operators")
@RequiredArgsConstructor
public class HelpSupportBackofficeResource {

    private final HelpSupportServiceProcessor helpSupportServiceProcessor;

    @Auditable(action = "LIST_ALL_SUPPORT_TICKETS")
    @GetMapping("/support-ticket/list")
    @Operation(summary = "List all support tickets")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tickets retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public HelpSupportResponse listAllSupportTickets(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.listAllSupportTickets(locale);
    }
}
