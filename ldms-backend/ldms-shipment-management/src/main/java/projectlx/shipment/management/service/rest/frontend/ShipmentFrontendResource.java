package projectlx.shipment.management.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import projectlx.shipment.management.service.processor.api.ShipmentServiceProcessor;
import projectlx.shipment.management.utils.requests.AllocateShipmentRequest;
import projectlx.shipment.management.utils.requests.ShipmentMultipleFiltersRequest;
import projectlx.shipment.management.utils.responses.ShipmentResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-shipment-management/v1/frontend/shipment")
@Tag(name = "Shipment Frontend Resource", description = "Shipment management operations for frontend consumers")
@RequiredArgsConstructor
public class ShipmentFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentFrontendResource.class);

    private final ShipmentServiceProcessor shipmentServiceProcessor;

    @Auditable(action = "FIND_SHIPMENT_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find shipment by id", description = "Returns a single shipment by its id for the caller's organisation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment found"),
            @ApiResponse(responseCode = "404", description = "Shipment not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ShipmentResponse> findById(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ShipmentResponse response = shipmentServiceProcessor.findById(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "FIND_SHIPMENTS_BY_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Find shipments by filters",
            description = "Returns a pageable list of shipments filtered by organizationId, status, inventoryTransferId, or free-text search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipments retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ShipmentResponse> findByMultipleFilters(
            @RequestBody final ShipmentMultipleFiltersRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ShipmentResponse response = shipmentServiceProcessor.findByMultipleFilters(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "ALLOCATE_SHIPMENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/allocate")
    @Operation(summary = "Allocate fleet to shipment",
            description = "Assigns a fleet driver and asset to a PENDING_ALLOCATION shipment, transitioning it to ALLOCATED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment allocated successfully"),
            @ApiResponse(responseCode = "400", description = "Allocation request invalid or shipment not in PENDING_ALLOCATION state"),
            @ApiResponse(responseCode = "404", description = "Shipment not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ShipmentResponse> allocate(
            @RequestBody final AllocateShipmentRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ShipmentResponse response = shipmentServiceProcessor.allocateFleet(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "FIND_SHIPMENT_BY_TRANSFER")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-transfer/{transferId}")
    @Operation(summary = "Find shipment by inventory transfer id",
            description = "Returns the shipment linked to a given inventory transfer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment found"),
            @ApiResponse(responseCode = "404", description = "Shipment not found for the given transfer"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ShipmentResponse> findByTransferId(
            @PathVariable final Long transferId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ShipmentResponse response = shipmentServiceProcessor.findByTransferId(transferId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
