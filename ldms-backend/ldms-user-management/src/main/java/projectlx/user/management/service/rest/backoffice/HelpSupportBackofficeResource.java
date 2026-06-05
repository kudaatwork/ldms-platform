package projectlx.user.management.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.HelpSupportServiceProcessor;
import projectlx.user.management.utils.requests.AddSupportTicketMessageRequest;
import projectlx.user.management.utils.requests.AssignSupportTicketRequest;
import projectlx.user.management.utils.requests.SupportTicketExportFilterRequest;
import projectlx.user.management.utils.requests.UpdateSupportTicketStatusRequest;
import projectlx.user.management.utils.responses.HelpSupportResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/backoffice/help-support")
@Tag(name = "Help & Support Backoffice Resource", description = "Support ticket queue for LX operators")
@RequiredArgsConstructor
public class HelpSupportBackofficeResource {

    private static final Logger logger = LoggerFactory.getLogger(HelpSupportBackofficeResource.class);

    private final HelpSupportServiceProcessor helpSupportServiceProcessor;

    @Auditable(action = "LIST_ALL_SUPPORT_TICKETS")
    @GetMapping("/support-ticket/list")
    @Operation(summary = "List all support tickets")
    public HelpSupportResponse listAllSupportTickets(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.listAllSupportTickets(locale);
    }

    @Auditable(action = "FIND_SUPPORT_TICKET_BY_ID")
    @GetMapping("/support-ticket/find-by-id/{id}")
    @Operation(summary = "Find support ticket by id with conversation thread")
    public HelpSupportResponse findSupportTicketById(
            @PathVariable("id") Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.findSupportTicketById(id, locale, true);
    }

    @Auditable(action = "UPDATE_SUPPORT_TICKET_STATUS")
    @PutMapping("/support-ticket/update-status")
    @Operation(summary = "Update support ticket status")
    public HelpSupportResponse updateSupportTicketStatus(
            @Valid @RequestBody UpdateSupportTicketStatusRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.updateSupportTicketStatus(request, locale, actorUsername());
    }

    @Auditable(action = "ASSIGN_SUPPORT_TICKET")
    @PutMapping("/support-ticket/assign")
    @Operation(summary = "Assign support ticket to a handler")
    public HelpSupportResponse assignSupportTicket(
            @Valid @RequestBody AssignSupportTicketRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.assignSupportTicket(request, locale, actorUsername());
    }

    @Auditable(action = "ADD_SUPPORT_TICKET_MESSAGE")
    @PostMapping("/support-ticket/add-message")
    @Operation(summary = "Add handler or internal note to a support ticket")
    public HelpSupportResponse addSupportTicketMessage(
            @Valid @RequestBody AddSupportTicketMessageRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.addSupportTicketMessage(request, locale, actorUsername(), true);
    }

    @Auditable(action = "EXPORT_SUPPORT_TICKETS")
    @PostMapping("/support-ticket/export")
    @Operation(summary = "Export support ticket queue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Export generated"),
            @ApiResponse(responseCode = "400", description = "Invalid export format")
    })
    public ResponseEntity<byte[]> exportSupportTickets(
            @RequestBody(required = false) SupportTicketExportFilterRequest filters,
            @RequestParam String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        try {
            byte[] data = helpSupportServiceProcessor.exportSupportTickets(filters, format, locale);
            String normalized = format.toLowerCase(Locale.ROOT);
            MediaType contentType;
            String filename;
            switch (normalized) {
                case "csv" -> {
                    contentType = MediaType.parseMediaType("text/csv");
                    filename = "support-tickets.csv";
                }
                case "excel", "xlsx" -> {
                    contentType = MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    filename = "support-tickets.xlsx";
                }
                case "pdf" -> {
                    contentType = MediaType.APPLICATION_PDF;
                    filename = "support-tickets.pdf";
                }
                default -> throw new IllegalArgumentException("Unsupported export format: " + format);
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(contentType)
                    .body(data);
        } catch (Exception ex) {
            logger.error("Failed to export support tickets", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String actorUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return "backoffice-operator";
        }
        return auth.getName();
    }
}
