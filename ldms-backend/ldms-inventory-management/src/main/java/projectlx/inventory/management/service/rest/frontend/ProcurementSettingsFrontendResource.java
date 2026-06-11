package projectlx.inventory.management.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import projectlx.inventory.management.business.logic.api.ProcurementSettingsService;
import projectlx.inventory.management.utils.requests.UpdateProcurementApprovalPolicyRequest;
import projectlx.inventory.management.utils.responses.ProcurementSettingsResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/procurement-settings")
@Tag(name = "Procurement Settings", description = "Manage procurement approval policy configuration")
@RequiredArgsConstructor
public class ProcurementSettingsFrontendResource {

    private final ProcurementSettingsService procurementSettingsService;
    private static final Logger logger = LoggerFactory.getLogger(ProcurementSettingsFrontendResource.class);

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/approval-policy")
    @Operation(summary = "Get approval policy",
            description = "Retrieves the current procurement approval policy including default required stages, min and max allowed stages.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval policy retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ProcurementSettingsResponse getApprovalPolicy(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        logger.info("Retrieving procurement approval policy");
        return procurementSettingsService.getApprovalPolicy(locale);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/approval-policy")
    @Operation(summary = "Update approval policy",
            description = "Updates the procurement approval policy. Supports setting both the platform-wide default and organisation-specific overrides.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval policy updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ProcurementSettingsResponse updateApprovalPolicy(
            @Valid @RequestBody UpdateProcurementApprovalPolicyRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Updating procurement approval policy by user: {}", username);
        return procurementSettingsService.updateApprovalPolicy(request, locale, username);
    }
}
