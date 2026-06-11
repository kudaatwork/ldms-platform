package projectlx.inventory.management.service.rest.system;

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
import org.springframework.web.bind.annotation.*;
import projectlx.inventory.management.service.processor.api.InventoryAllocationServiceProcessor;
import projectlx.inventory.management.service.processor.api.SalesReservationServiceProcessor;
import projectlx.inventory.management.utils.requests.*;
import projectlx.inventory.management.utils.responses.InventoryAllocationResponse;
import projectlx.inventory.management.utils.responses.InventoryAvailabilityResponse;
import projectlx.inventory.management.utils.responses.SalesReservationResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/inventory-allocations")
@Tag(name = "Inventory Allocation System Resource", description = "Operations related to reserving, releasing, reallocating and checking availability (system)")
@RequiredArgsConstructor
public class InventoryAllocationSystemResource {

    private final InventoryAllocationServiceProcessor inventoryAllocationServiceProcessor;
    private final SalesReservationServiceProcessor salesReservationServiceProcessor;

    private static final Logger logger = LoggerFactory.getLogger(InventoryAllocationSystemResource.class);

    @Auditable(action = "RESERVE_INVENTORY")
    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory for a sales order line")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory reserved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<SalesReservationResponse> reserve(@Valid @RequestBody final CreateReservationRequest request,
                                                            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                            final Locale locale) {
        logger.info("Incoming request to reserve inventory: {}", request);
        return ResponseEntity.ok(salesReservationServiceProcessor.create(request, locale, "SYSTEM"));
    }

    @Auditable(action = "RELEASE_RESERVATION")
    @PostMapping("/release")
    @Operation(summary = "Release inventory reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation released successfully"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<SalesReservationResponse> release(@Valid @RequestBody final ReleaseReservationRequest request,
                                                            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                            final Locale locale) {
        logger.info("Incoming request to release reservation: {}", request);
        return ResponseEntity.ok(salesReservationServiceProcessor.delete(request.getReservationId(), locale, "SYSTEM"));
    }

    @Auditable(action = "REALLOCATE_INVENTORY")
    @PostMapping("/reallocate")
    @Operation(summary = "Reallocate inventory between locations or batches")
    public ResponseEntity<InventoryAllocationResponse> reallocate(@Valid @RequestBody final ReallocateInventoryRequest request,
                                                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                                  final Locale locale) {
        logger.info("Incoming request to reallocate inventory: {}", request);
        // Currently there is no underlying service method to perform a reallocation in-place.
        // Return a standardized response indicating not implemented, to be wired later.
        InventoryAllocationResponse resp = new InventoryAllocationResponse();
        resp.setStatusCode(501);
        resp.setSuccess(false);
        resp.setMessage("Inventory reallocation via API is not implemented yet");
        return ResponseEntity.status(501).body(resp);
    }

    @Auditable(action = "CHECK_INVENTORY_AVAILABILITY")
    @PostMapping("/availability")
    @Operation(summary = "Query inventory availability for a product at a warehouse")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability checked successfully")
    })
    public ResponseEntity<InventoryAvailabilityResponse> availability(@Valid @RequestBody final InventoryAvailabilityRequest request,
                                                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                                      final Locale locale) {
        logger.info("Incoming request to check inventory availability: {}", request);
        BigDecimal available = inventoryAllocationServiceProcessor.getAvailableQuantity(request.getProductId(), request.getWarehouseId());
        BigDecimal reserved = inventoryAllocationServiceProcessor.getReservedQuantity(request.getProductId(), request.getWarehouseId());
        boolean canAllocate = request.getRequiredQuantity() == null ? available.compareTo(BigDecimal.ZERO) > 0
                : inventoryAllocationServiceProcessor.isAllocationPossible(request.getProductId(), request.getWarehouseId(), request.getRequiredQuantity());

        InventoryAvailabilityResponse response = new InventoryAvailabilityResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage("Availability check completed successfully");
        response.setAvailableQuantity(available);
        response.setReservedQuantity(reserved);
        response.setCanAllocate(canAllocate);
        return ResponseEntity.ok(response);
    }
}
