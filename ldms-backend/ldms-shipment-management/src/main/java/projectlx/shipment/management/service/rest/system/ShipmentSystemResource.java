package projectlx.shipment.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.shipment.management.service.processor.api.ShipmentServiceProcessor;
import projectlx.shipment.management.utils.requests.ShipmentMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.AutoAllocateShipmentFromFleetRequest;
import projectlx.shipment.management.utils.requests.UpdateShipmentStatusRequest;
import projectlx.shipment.management.utils.responses.ShipmentResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-shipment-management/v1/system/shipment")
@Tag(name = "Shipment System Resource", description = "Internal system operations for shipment management (inter-service calls)")
@RequiredArgsConstructor
public class ShipmentSystemResource {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentSystemResource.class);

    private final ShipmentServiceProcessor shipmentServiceProcessor;

    @Auditable(action = "SYSTEM_FIND_SHIPMENT_BY_ID")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "System: find shipment by id", description = "Returns a shipment by id — used by inter-service callers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment found"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentResponse> findById(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        ShipmentResponse response = shipmentServiceProcessor.findById(id, locale, "SYSTEM");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SYSTEM_FIND_SHIPMENT_BY_ID")
    @GetMapping("/by-id/{id}")
    @Operation(summary = "System: find shipment by id (alias)", description = "Alias for find-by-id used by inter-service callers.")
    public ResponseEntity<ShipmentResponse> findByIdAlias(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return findById(id, locale);
    }

    @Auditable(action = "SYSTEM_FIND_SHIPMENTS_BY_FILTERS")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "System: find shipments by filters", description = "Filters shipments by multiple criteria — used by inter-service callers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipments retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid filter request")
    })
    public ResponseEntity<ShipmentResponse> findByMultipleFilters(
            @RequestBody final ShipmentMultipleFiltersRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        ShipmentResponse response = shipmentServiceProcessor.findByMultipleFilters(request, locale, "SYSTEM");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SYSTEM_FIND_SHIPMENT_BY_TRANSFER")
    @GetMapping("/by-transfer/{transferId}")
    @Operation(summary = "System: find shipment by inventory transfer id",
            description = "Returns the shipment linked to the given inventory transfer — used by inter-service callers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment found"),
            @ApiResponse(responseCode = "404", description = "Shipment not found for the given transfer")
    })
    public ResponseEntity<ShipmentResponse> findByTransferId(
            @PathVariable final Long transferId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        ShipmentResponse response = shipmentServiceProcessor.findByTransferId(transferId, locale, "SYSTEM");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SYSTEM_FIND_SHIPMENT_BY_SALES_ORDER")
    @GetMapping("/by-sales-order/{salesOrderId}")
    @Operation(summary = "System: find shipment by sales order id",
            description = "Returns the shipment linked to a bought-goods sales order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment found"),
            @ApiResponse(responseCode = "404", description = "Shipment not found for the given sales order")
    })
    public ResponseEntity<ShipmentResponse> findBySalesOrderId(
            @PathVariable final Long salesOrderId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        ShipmentResponse response = shipmentServiceProcessor.findBySalesOrderId(salesOrderId, locale, "SYSTEM");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SYSTEM_UPDATE_SHIPMENT_STATUS")
    @PatchMapping("/status")
    @Operation(summary = "System: update shipment status",
            description = "Transitions a shipment's status (e.g. IN_TRANSIT, ARRIVED_PENDING_OTP, DELIVERED). "
                    + "Called by the trip-tracking service after milestone events.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transition or request"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentResponse> updateStatus(
            @RequestBody final UpdateShipmentStatusRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        ShipmentResponse response = shipmentServiceProcessor.updateStatus(request, locale, "SYSTEM");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SYSTEM_AUTO_ALLOCATE_SHIPMENT_FROM_FLEET")
    @PostMapping("/auto-allocate-from-fleet")
    @Operation(summary = "System: auto-allocate shipment from fleet driver assignment",
            description = "When a transport company or shipper assigns a driver to a fleet vehicle, "
                    + "links the oldest matching shipment in PENDING_FLEET_ALLOCATION to that driver and vehicle.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auto-allocation applied or no matching shipment"),
            @ApiResponse(responseCode = "400", description = "Allocation validation failed")
    })
    public ResponseEntity<ShipmentResponse> autoAllocateFromFleet(
            @RequestBody final AutoAllocateShipmentFromFleetRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        ShipmentResponse response = shipmentServiceProcessor.autoAllocateFromFleet(request, locale, "SYSTEM");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
