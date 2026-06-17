package projectlx.messaging.inbound.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.messaging.inbound.service.processor.api.BotKnowledgeServiceProcessor;
import projectlx.messaging.inbound.utils.responses.BotKnowledgeResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-messaging-inbound/v1/backoffice/bot-knowledge")
@Tag(name = "Bot knowledge (backoffice)", description = "Reload LDMS assistant reference documents")
@RequiredArgsConstructor
public class BotKnowledgeBackofficeResource {

    private final BotKnowledgeServiceProcessor botKnowledgeServiceProcessor;

    @Auditable(action = "BACKOFFICE_BOT_KNOWLEDGE_STATUS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/status")
    @Operation(summary = "Knowledge corpus load status")
    public ResponseEntity<BotKnowledgeResponse> status(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotKnowledgeResponse response = botKnowledgeServiceProcessor.status(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_BOT_KNOWLEDGE_RELOAD")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reload")
    @Operation(summary = "Reload knowledge from classpath and optional external directory")
    public ResponseEntity<BotKnowledgeResponse> reload(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotKnowledgeResponse response = botKnowledgeServiceProcessor.reload(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
