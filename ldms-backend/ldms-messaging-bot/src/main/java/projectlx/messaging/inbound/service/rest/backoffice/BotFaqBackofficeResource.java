package projectlx.messaging.inbound.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.messaging.inbound.service.processor.api.BotFaqServiceProcessor;
import projectlx.messaging.inbound.utils.requests.CreateBotFaqRequest;
import projectlx.messaging.inbound.utils.requests.EditBotFaqRequest;
import projectlx.messaging.inbound.utils.responses.BotFaqResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-messaging-inbound/v1/backoffice/bot-faq")
@Tag(name = "Bot FAQ (backoffice)", description = "Manage FAQ knowledge for bot RAG")
@RequiredArgsConstructor
public class BotFaqBackofficeResource {

    private final BotFaqServiceProcessor botFaqServiceProcessor;

    @Auditable(action = "BACKOFFICE_LIST_BOT_FAQ")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    @Operation(summary = "List all bot FAQ entries")
    public ResponseEntity<BotFaqResponse> list(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotFaqResponse response = botFaqServiceProcessor.listAll(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_FIND_BOT_FAQ")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find bot FAQ by id")
    public ResponseEntity<BotFaqResponse> findById(
            @PathVariable Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotFaqResponse response = botFaqServiceProcessor.findById(id, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_CREATE_BOT_FAQ")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create bot FAQ entry")
    public ResponseEntity<BotFaqResponse> create(
            @RequestBody CreateBotFaqRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotFaqResponse response = botFaqServiceProcessor.create(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_UPDATE_BOT_FAQ")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update/{id}")
    @Operation(summary = "Update bot FAQ entry")
    public ResponseEntity<BotFaqResponse> update(
            @PathVariable Long id,
            @RequestBody EditBotFaqRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotFaqResponse response = botFaqServiceProcessor.update(id, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_DELETE_BOT_FAQ")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Soft-delete bot FAQ entry")
    public ResponseEntity<BotFaqResponse> delete(
            @PathVariable Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotFaqResponse response = botFaqServiceProcessor.delete(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
