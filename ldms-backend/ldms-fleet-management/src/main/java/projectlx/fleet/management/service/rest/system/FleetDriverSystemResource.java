package projectlx.fleet.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.fleet.management.service.processor.api.FleetDriverServiceProcessor;
import projectlx.fleet.management.utils.responses.FleetDriverResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

/**
 * System-only fleet driver endpoints called by other microservices (e.g. shipment-management,
 * trip-tracking) to resolve driver contact details for notifications.  No JWT session-org check.
 */
@CrossOrigin
@RestController
@RequestMapping("/ldms-fleet-management/v1/system/fleet-driver")
@Tag(name = "Fleet Driver System Resource", description = "Internal fleet driver lookup for system callers")
@RequiredArgsConstructor
public class FleetDriverSystemResource {

    private static final Logger logger = LoggerFactory.getLogger(FleetDriverSystemResource.class);

    private final FleetDriverServiceProcessor fleetDriverServiceProcessor;

    @Auditable(action = "SYSTEM_FIND_FLEET_DRIVER_BY_ID")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find fleet driver by ID (system)",
            description = "Returns fleet driver details by ID. No organisation-workspace restriction. " +
                          "For internal service-to-service calls only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fleet driver found successfully"),
            @ApiResponse(responseCode = "404", description = "Fleet driver not found"),
            @ApiResponse(responseCode = "400", description = "Invalid fleet driver ID")
    })
    public FleetDriverResponse findByIdForSystem(
            @PathVariable("id") final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.info("System request to find fleet driver by id={}", id);
        return fleetDriverServiceProcessor.findByIdForSystem(id, locale);
    }

    @Auditable(action = "SYSTEM_FIND_FLEET_DRIVER_BY_USER_ID")
    @GetMapping("/find-by-user-id/{userId}")
    @Operation(summary = "Find fleet driver by platform user ID (system)",
            description = "Returns fleet driver linked to the given user-management user id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fleet driver found successfully"),
            @ApiResponse(responseCode = "404", description = "Fleet driver not found"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    public FleetDriverResponse findByUserIdForSystem(
            @PathVariable("userId") final Long userId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.info("System request to find fleet driver by userId={}", userId);
        return fleetDriverServiceProcessor.findByUserIdForSystem(userId, locale);
    }
}
