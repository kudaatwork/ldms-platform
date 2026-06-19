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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import projectlx.inventory.management.service.processor.api.InventoryIntegrationCredentialServiceProcessor;
import projectlx.inventory.management.utils.requests.CreateInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.requests.EditInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.responses.InventoryIntegrationCredentialResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/integration-credential")
@Tag(name = "Inventory Integration Credential Frontend Resource",
     description = "Manage API credentials for third-party inventory integrations")
@RequiredArgsConstructor
public class InventoryIntegrationCredentialFrontendResource {

    private final InventoryIntegrationCredentialServiceProcessor inventoryIntegrationCredentialServiceProcessor;
    private static final Logger logger =
            LoggerFactory.getLogger(InventoryIntegrationCredentialFrontendResource.class);

    @Auditable(action = "CREATE_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create integration credential",
               description = "Creates a new integration credential with a generated api_key.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credential created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<InventoryIntegrationCredentialResponse> create(
            @Valid @RequestBody final CreateInventoryIntegrationCredentialRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                inventoryIntegrationCredentialServiceProcessor.create(request, locale, username));
    }

    @Auditable(action = "UPDATE_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update integration credential",
               description = "Updates label, webhook URL, callback GRV URL or status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Credential not found")
    })
    public ResponseEntity<InventoryIntegrationCredentialResponse> update(
            @Valid @RequestBody final EditInventoryIntegrationCredentialRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                inventoryIntegrationCredentialServiceProcessor.update(request, locale, username));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find integration credential by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential retrieved"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<InventoryIntegrationCredentialResponse> findById(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                inventoryIntegrationCredentialServiceProcessor.findById(id, locale, username));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-organization/{organizationId}")
    @Operation(summary = "Find all integration credentials for an organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials retrieved")
    })
    public ResponseEntity<InventoryIntegrationCredentialResponse> findAllByOrganization(
            @PathVariable final Long organizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                inventoryIntegrationCredentialServiceProcessor.findAllByOrganization(
                        organizationId, locale, username));
    }

    @Auditable(action = "DELETE_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Soft-delete an integration credential")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<InventoryIntegrationCredentialResponse> delete(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                inventoryIntegrationCredentialServiceProcessor.delete(id, locale, username));
    }
}
