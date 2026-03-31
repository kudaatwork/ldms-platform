package projectlx.co.zw.locationsmanagementservice.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationIngestionService.GooglePlaceDetails;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocationIngestionProcessor;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/system/locations/ingest")
@Tag(name = "Locations Ingest System Resource", description = "Operations related to managing ingested locations (system)")
@RequiredArgsConstructor
public class LocationIngestionSystemResource {

    private final LocationIngestionProcessor locationIngestionProcessor;
    private static final Logger logger = LoggerFactory.getLogger(LocationIngestionSystemResource.class);

    @Auditable(action = "INGEST_GOOGLE_PLACE")
    @PostMapping("/google-place")
    @Operation(summary = "Ingest a Google Place", description = "Creates a hierarchical address structure from Google Places API data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location ingested successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<Address> ingestGooglePlace(
            @RequestBody GooglePlaceDetails details,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        
        logger.info("Incoming system request to ingest Google Place: {}", details);
        
        Address address = locationIngestionProcessor.ingestGooglePlace(details, locale, "SYSTEM");
        
        logger.info("Successfully ingested Google Place (system). Address ID: {}", 
                address != null ? address.getId() : "null");
        
        return ResponseEntity.ok(address);
    }
}