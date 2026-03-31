package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LocationIngestionRoles {

    INGEST_GOOGLE_PLACE("INGEST_GOOGLE_PLACE", "Ingests location from Google Places API"),
    VIEW_INGESTED_LOCATION_BY_ID("VIEW_INGESTED_LOCATION_BY_ID", "Views ingested location by id"),
    DELETE_INGESTED_LOCATION("DELETE_INGESTED_LOCATION", "Deletes ingested location"),
    VIEW_ALL_INGESTED_LOCATIONS("VIEW_ALL_INGESTED_LOCATIONS", "Views all ingested locations"),
    VIEW_INGESTED_LOCATIONS_BY_FILTERS("VIEW_INGESTED_LOCATIONS_BY_FILTERS", "Views ingested locations by filters"),
    EXPORT_INGESTED_LOCATIONS("EXPORT_INGESTED_LOCATIONS", "Exports ingested locations"),
    IMPORT_INGESTED_LOCATIONS("IMPORT_INGESTED_LOCATIONS", "Imports ingested locations from external sources");

    private final String roleName;
    private final String description;
}