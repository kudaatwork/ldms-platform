package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum GeoCoordinatesRoles {

    CREATE_GEO_COORDINATES("CREATE_GEO_COORDINATES", "Creates geo coordinates"),
    UPDATE_GEO_COORDINATES("UPDATE_GEO_COORDINATES", "Updates geo coordinates information"),
    VIEW_GEO_COORDINATES_BY_ID("VIEW_GEO_COORDINATES_BY_ID", "Views geo coordinates by id"),
    DELETE_GEO_COORDINATES("DELETE_GEO_COORDINATES", "Deletes geo coordinates"),
    VIEW_ALL_GEO_COORDINATES_AS_A_LIST("VIEW_ALL_GEO_COORDINATES_AS_A_LIST", "Views all geo coordinates as a list"),
    VIEW_ALL_GEO_COORDINATES_BY_MULTIPLE_FILTERS("VIEW_ALL_GEO_COORDINATES_BY_MULTIPLE_FILTERS", "Views all geo coordinates by multiple filters"),
    EXPORT_GEO_COORDINATES("EXPORT_GEO_COORDINATES", "Exports geo coordinates"),
    IMPORT_GEO_COORDINATES("IMPORT_GEO_COORDINATES", "Imports geo coordinates from CSV");

    private final String roleName;
    private final String description;
}