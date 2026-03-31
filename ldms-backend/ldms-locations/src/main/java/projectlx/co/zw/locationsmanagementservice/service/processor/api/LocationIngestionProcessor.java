package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationIngestionService.GooglePlaceDetails;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import java.util.Locale;

/**
 * Interface defining the contract for processing location ingestion requests.
 */
public interface LocationIngestionProcessor {
    
    /**
     * Process the ingestion of a Google Place.
     *
     * @param details The DTO containing data from the external API.
     * @param locale The locale of the requesting user.
     * @param username The username of the user performing the action.
     * @return The newly created and persisted Address entity.
     */
    Address ingestGooglePlace(GooglePlaceDetails details, Locale locale, String username);
}