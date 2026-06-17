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
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fleet.management.service.processor.api.FleetAssetServiceProcessor;
import projectlx.fleet.management.utils.responses.FleetAssetResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fleet-management/v1/system/fleet-asset")
@Tag(name = "Fleet Asset System Resource", description = "Internal fleet asset lookup for system callers")
@RequiredArgsConstructor
public class FleetAssetSystemResource {

    private static final Logger logger = LoggerFactory.getLogger(FleetAssetSystemResource.class);

    private final FleetAssetServiceProcessor fleetAssetServiceProcessor;

    @Auditable(action = "SYSTEM_FIND_FLEET_ASSET_BY_ID")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find fleet asset by ID (system)",
            description = "Returns fleet asset details including max speed limit. For internal service-to-service calls only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fleet asset found successfully"),
            @ApiResponse(responseCode = "404", description = "Fleet asset not found"),
            @ApiResponse(responseCode = "400", description = "Invalid fleet asset ID")
    })
    public FleetAssetResponse findByIdForSystem(
            @PathVariable("id") final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.info("System request to find fleet asset by id={}", id);
        return fleetAssetServiceProcessor.findByIdForSystem(id, locale);
    }
}
