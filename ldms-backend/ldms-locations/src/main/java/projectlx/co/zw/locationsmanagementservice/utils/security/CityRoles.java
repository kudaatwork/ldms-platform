package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CityRoles {

    CREATE_CITY("CREATE_CITY", "Creates city"),
    UPDATE_CITY("UPDATE_CITY", "Updates city information"),
    VIEW_CITY_BY_ID("VIEW_CITY_BY_ID", "Views city by id"),
    DELETE_CITY("DELETE_CITY", "Deletes city"),
    VIEW_ALL_CITIES_AS_A_LIST("VIEW_ALL_CITIES_AS_A_LIST", "Views all cities as a list"),
    VIEW_ALL_CITIES_BY_MULTIPLE_FILTERS("VIEW_ALL_CITIES_BY_MULTIPLE_FILTERS", "Views all cities by multiple filters"),
    EXPORT_CITIES("EXPORT_CITIES", "Exports cities"),
    IMPORT_CITIES("IMPORT_CITIES", "Imports cities from CSV");

    private final String roleName;
    private final String description;
}
