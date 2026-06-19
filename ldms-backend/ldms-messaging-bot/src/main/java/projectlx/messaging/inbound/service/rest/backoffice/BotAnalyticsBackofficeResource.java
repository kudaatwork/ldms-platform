package projectlx.messaging.inbound.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.messaging.inbound.service.processor.api.BotAnalyticsServiceProcessor;
import projectlx.messaging.inbound.utils.responses.BotAnalyticsResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-messaging-inbound/v1/backoffice/bot-analytics")
@Tag(name = "Bot analytics (backoffice)", description = "Bot assistant usage metrics")
@RequiredArgsConstructor
public class BotAnalyticsBackofficeResource {

    private final BotAnalyticsServiceProcessor botAnalyticsServiceProcessor;

    @Auditable(action = "BACKOFFICE_BOT_ANALYTICS_SUMMARY")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/summary")
    @Operation(summary = "Bot assistant analytics summary")
    public ResponseEntity<BotAnalyticsResponse> summary(
            @RequestParam(defaultValue = "30") int days,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotAnalyticsResponse response = botAnalyticsServiceProcessor.getSummary(days, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
