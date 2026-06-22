package projectlx.fuel.expenses.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fuel.expenses.service.processor.api.RoadsideProviderServiceProcessor;
import projectlx.fuel.expenses.utils.responses.RoadsideProviderResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fuel-expenses/v1/frontend/roadside-provider")
@Tag(name = "Roadside provider directory", description = "Fuel stations, mechanics, and roadside support along corridors")
@RequiredArgsConstructor
public class RoadsideProviderFrontendResource {

    private final RoadsideProviderServiceProcessor roadsideProviderServiceProcessor;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    @Operation(summary = "List verified roadside providers")
    public ResponseEntity<RoadsideProviderResponse> list(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        RoadsideProviderResponse response = roadsideProviderServiceProcessor.listAll(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/nearby")
    @Operation(summary = "List roadside providers near a GPS point")
    public ResponseEntity<RoadsideProviderResponse> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "150") double radiusKm,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        RoadsideProviderResponse response = roadsideProviderServiceProcessor.listNearby(lat, lng, radiusKm, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
