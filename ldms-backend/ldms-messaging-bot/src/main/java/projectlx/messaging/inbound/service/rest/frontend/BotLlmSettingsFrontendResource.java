package projectlx.messaging.inbound.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.messaging.inbound.service.processor.api.BotLlmSettingsServiceProcessor;
import projectlx.messaging.inbound.utils.requests.UpdateBotLlmRuntimeRequest;
import projectlx.messaging.inbound.utils.responses.BotLlmSettingsResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-messaging-inbound/v1/frontend/bot-llm-settings")
@Tag(name = "Bot LLM settings (frontend)", description = "Provider and model selection for Help & Support assistant")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BotLlmSettingsFrontendResource {

    private final BotLlmSettingsServiceProcessor botLlmSettingsServiceProcessor;

    @Auditable(action = "GET_BOT_LLM_SETTINGS_FRONTEND")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/current")
    @Operation(summary = "Current LLM provider, model, and catalog")
    public ResponseEntity<BotLlmSettingsResponse> currentSettings(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotLlmSettingsResponse response = botLlmSettingsServiceProcessor.currentSettings(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "UPDATE_BOT_LLM_RUNTIME_FRONTEND")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/runtime")
    @Operation(summary = "Switch provider/model at runtime for this deployment")
    public ResponseEntity<BotLlmSettingsResponse> updateRuntime(
            @Valid @RequestBody UpdateBotLlmRuntimeRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotLlmSettingsResponse response = botLlmSettingsServiceProcessor.updateRuntime(request, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
