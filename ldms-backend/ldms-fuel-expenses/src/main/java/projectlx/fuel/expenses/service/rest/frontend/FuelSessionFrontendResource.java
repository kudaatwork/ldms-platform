package projectlx.fuel.expenses.service.rest.frontend;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fuel.expenses.service.processor.api.FuelSessionServiceProcessor;
import projectlx.fuel.expenses.utils.responses.FuelSessionResponse;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fuel-expenses/v1/frontend/fuel-session")
@Tag(name = "Fuel Session Frontend Resource", description = "Operations for live fuel session monitoring")
@RequiredArgsConstructor
public class FuelSessionFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(FuelSessionFrontendResource.class);

    private final FuelSessionServiceProcessor fuelSessionServiceProcessor;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/live/{tripId}")
    @Operation(
            summary = "Get live fuel session for a trip",
            description = "Returns the current fuel level, remaining litres, distance travelled, " +
                    "and GPS position for the active fuel session associated with the given trip.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fuel session snapshot returned"),
            @ApiResponse(responseCode = "400", description = "Invalid trip ID"),
            @ApiResponse(responseCode = "404", description = "No active fuel session found for the trip"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FuelSessionResponse> getLiveByTripId(
            @PathVariable Long tripId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("GET /live/{} requested by user={}", tripId, username);

        FuelSessionResponse response = fuelSessionServiceProcessor.getLiveByTripId(tripId, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
