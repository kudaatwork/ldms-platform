package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import projectlx.co.zw.locationsmanagementservice.model.Address;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Interface defining the contract for ingesting location data from an external source.
 */
public interface LocationIngestionService {

    /**
     * Main orchestration method. It takes the parsed details from an external source (like Google Places API)
     * and creates a complete, hierarchical Address entity in the local database.
     *
     * @param details The DTO containing data from the external API.
     * @param locale The locale of the requesting user.
     * @param username The username of the user performing the action.
     * @return The newly created and persisted Address entity.
     */
    Address ingestGooglePlace(GooglePlaceDetails details, Locale locale, String username);

    // --- DTOs (Data Transfer Objects) ---
    // These are part of the service's public contract.
    // They represent the structure of the data this service expects.

    class GooglePlaceDetails {
        private Geometry geometry;
        private List<AddressComponent> addressComponents;
        // Getters & Setters...
        public Geometry getGeometry() { return geometry; }
        public List<AddressComponent> getAddressComponents() { return addressComponents; }
    }

    class Geometry {
        private Location location;
        // Getters & Setters...
        public Location getLocation() { return location; }
    }

    class Location {
        private BigDecimal lat;
        private BigDecimal lng;
        // Getters & Setters...
        public BigDecimal getLat() { return lat; }
        public BigDecimal getLng() { return lng; }
    }

    class AddressComponent {
        private String longName;
        private String shortName;
        private List<String> types;
        // Getters & Setters...
        public String getLongName() { return longName; }
        public String getShortName() { return shortName; }
        public List<String> getTypes() { return types; }
    }
}
