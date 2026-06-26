package projectlx.messaging.inbound.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
import projectlx.messaging.inbound.service.processor.api.BotSessionServiceProcessor;
import projectlx.messaging.inbound.utils.requests.RateBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.SendBotMessageRequest;
import projectlx.messaging.inbound.utils.requests.StartBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.UpdateBotAssistantModeRequest;
import projectlx.messaging.inbound.utils.responses.BotSessionResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-messaging-inbound/v1/frontend/bot-session")
@Tag(name = "Bot session (frontend)", description = "LDMS assistant chat for platform portal users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BotSessionFrontendResource {

    private final BotSessionServiceProcessor botSessionServiceProcessor;

    @Auditable(action = "START_BOT_SESSION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/start")
    @Operation(summary = "Start a new LDMS assistant conversation")
    public ResponseEntity<BotSessionResponse> startSession(
            @Valid @RequestBody(required = false) StartBotSessionRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        StartBotSessionRequest body = request != null ? request : new StartBotSessionRequest();
        BotSessionResponse response = botSessionServiceProcessor.startSession(body, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SEND_BOT_MESSAGE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/send-message")
    @Operation(summary = "Send a message in an assistant conversation")
    public ResponseEntity<BotSessionResponse> sendMessage(
            @Valid @RequestBody SendBotMessageRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotSessionResponse response = botSessionServiceProcessor.sendMessage(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_MY_BOT_SESSIONS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-sessions")
    @Operation(summary = "List my assistant conversations")
    public ResponseEntity<BotSessionResponse> listMySessions(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotSessionResponse response = botSessionServiceProcessor.listMySessions(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "FIND_MY_BOT_SESSION")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{sessionId}")
    @Operation(summary = "Get one of my assistant conversations with full thread")
    public ResponseEntity<BotSessionResponse> findMySessionById(
            @PathVariable("sessionId") String sessionId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotSessionResponse response = botSessionServiceProcessor.findMySessionById(sessionId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "RATE_BOT_SESSION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/rate")
    @Operation(summary = "Rate an assistant conversation (1–5)")
    public ResponseEntity<BotSessionResponse> rateSession(
            @Valid @RequestBody RateBotSessionRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotSessionResponse response = botSessionServiceProcessor.rateSession(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "UPDATE_BOT_ASSISTANT_MODE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/assistant-mode")
    @Operation(summary = "Switch between Assistant (user guide) and Agent (platform architecture) mode")
    public ResponseEntity<BotSessionResponse> updateAssistantMode(
            @Valid @RequestBody UpdateBotAssistantModeRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotSessionResponse response = botSessionServiceProcessor.updateAssistantMode(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/pricing")
    @Operation(summary = "Live Lexi message costs from the platform action charge catalog")
    public ResponseEntity<BotSessionResponse> getPricing(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotSessionResponse response = botSessionServiceProcessor.getPricing(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
