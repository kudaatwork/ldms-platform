package projectlx.messaging.inbound.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.messaging.inbound.service.processor.api.BotSessionServiceProcessor;
import projectlx.messaging.inbound.utils.responses.BotSessionResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-messaging-inbound/v1/backoffice/bot-session")
@Tag(name = "Bot session (backoffice)", description = "Cross-tenant bot conversation monitor for LX administrators")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BotSessionBackofficeResource {

    private final BotSessionServiceProcessor botSessionServiceProcessor;

    @Auditable(action = "LIST_BOT_SESSIONS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    @Operation(summary = "List all bot conversations")
    public ResponseEntity<BotSessionResponse> listSessions(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotSessionResponse response = botSessionServiceProcessor.listAllSessions(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "FIND_BOT_SESSION")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{sessionId}")
    @Operation(summary = "Get one bot conversation with full thread")
    public ResponseEntity<BotSessionResponse> findSessionById(
            @PathVariable("sessionId") String sessionId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotSessionResponse response = botSessionServiceProcessor.findSessionById(sessionId, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
