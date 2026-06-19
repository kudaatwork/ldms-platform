package projectlx.inventory.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projectlx.inventory.management.business.logic.api.InventoryIntegrationCredentialService;
import projectlx.inventory.management.model.InventoryIntegrationCredential;
import projectlx.inventory.management.service.processor.api.CrossDockDispatchServiceProcessor;
import projectlx.inventory.management.utils.dtos.InventoryIntegrationCredentialDto;
import projectlx.inventory.management.utils.requests.DispatchIngestRequest;
import projectlx.inventory.management.utils.requests.GrvCallbackRequest;
import projectlx.inventory.management.utils.responses.CrossDockDispatchResponse;
import projectlx.inventory.management.utils.responses.InventoryIntegrationCredentialResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import org.modelmapper.ModelMapper;

import java.util.Locale;
import java.util.Optional;

/**
 * System-facing integration endpoints authenticated via {@code api_key} (not JWT).
 *
 * <p>Security note: these endpoints are excluded from JWT filter via Spring Security
 * permit-all rule on the {@code /system/integration/**} path.
 * The api_key in the request body is validated inside the service.
 */
@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/integration")
@Tag(name = "Inventory Integration System Resource",
     description = "System endpoints for third-party dispatch ingest and GRV callbacks (api_key auth)")
@RequiredArgsConstructor
public class InventoryIntegrationSystemResource {

    private final CrossDockDispatchServiceProcessor crossDockDispatchServiceProcessor;
    private final InventoryIntegrationCredentialService inventoryIntegrationCredentialService;
    private final ModelMapper modelMapper;
    private static final Logger logger = LoggerFactory.getLogger(InventoryIntegrationSystemResource.class);

    /**
     * POST /ldms-inventory-management/v1/system/integration/dispatch-ingest
     *
     * <p>Resolves org by api_key and creates a cross-dock dispatch record that
     * triggers the downstream shipment flow.
     */
    @PostMapping("/dispatch-ingest")
    @Operation(summary = "Ingest external dispatch",
               description = "Creates a cross-dock dispatch record for a third-party system dispatch event. " +
                             "Authenticated via api_key in request body (no JWT required).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dispatch ingested successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing api_key"),
            @ApiResponse(responseCode = "403", description = "Credential suspended"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<CrossDockDispatchResponse> dispatchIngest(
            @Valid @RequestBody final DispatchIngestRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        logger.info("System dispatch ingest received [externalId={}]", request.getExternalDispatchId());
        return ResponseEntity.ok(crossDockDispatchServiceProcessor.ingestDispatch(request, locale));
    }

    /**
     * POST /ldms-inventory-management/v1/system/integration/grv-callback
     *
     * <p>Internal — called when a GRV completes on a cross-dock shipment to push
     * result to the originating org's webhook.
     */
    @PostMapping("/grv-callback")
    @Operation(summary = "GRV completion callback",
               description = "Internal endpoint called when a GRV completes; pushes result to org webhook (stub).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Callback handled successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid api_key"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<CrossDockDispatchResponse> grvCallback(
            @Valid @RequestBody final GrvCallbackRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        logger.info("GRV callback received [grvId={}]", request.getGrvId());
        return ResponseEntity.ok(crossDockDispatchServiceProcessor.handleGrvCallback(request, locale));
    }

    /**
     * GET /ldms-inventory-management/v1/system/integration/resolve-credential
     *
     * <p>Internal — resolves a credential record by api_key. Used by other system
     * services that need to validate an api_key without a full ingest.
     */
    @GetMapping("/resolve-credential")
    @Operation(summary = "Resolve credential by api_key",
               description = "Internal use — resolves a credential by api_key for validation purposes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential resolved"),
            @ApiResponse(responseCode = "404", description = "Credential not found")
    })
    public ResponseEntity<InventoryIntegrationCredentialResponse> resolveCredential(
            @RequestParam final String apiKey,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        Optional<InventoryIntegrationCredential> optional =
                inventoryIntegrationCredentialService.resolveByApiKey(apiKey);

        if (optional.isEmpty()) {
            InventoryIntegrationCredentialResponse response = new InventoryIntegrationCredentialResponse();
            response.setStatusCode(404);
            response.setSuccess(false);
            response.setMessage("Credential not found for provided api_key");
            return ResponseEntity.status(404).body(response);
        }

        InventoryIntegrationCredentialDto dto =
                modelMapper.map(optional.get(), InventoryIntegrationCredentialDto.class);

        InventoryIntegrationCredentialResponse response = new InventoryIntegrationCredentialResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage("Credential resolved successfully");
        response.setInventoryIntegrationCredentialDto(dto);
        return ResponseEntity.ok(response);
    }
}
