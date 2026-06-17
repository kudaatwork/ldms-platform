package projectlx.fuel.expenses.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.fuel.expenses.service.processor.api.FuelSessionServiceProcessor;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fuel-expenses/v1/system/fuel-session")
@Tag(name = "Fuel Session System Resource", description = "Inter-service fuel session updates")
@RequiredArgsConstructor
public class FuelSessionSystemResource {

    private final FuelSessionServiceProcessor fuelSessionServiceProcessor;

    @PostMapping("/location-updated")
    @Operation(summary = "Apply a trip location update to the fuel session",
            description = "Used by trip-tracking IoT simulation and telematics ingest to keep fuel levels in sync.")
    public ResponseEntity<Void> locationUpdated(@RequestBody Map<String, Object> payload) {
        fuelSessionServiceProcessor.onLocationUpdated(payload);
        return ResponseEntity.ok().build();
    }
}
