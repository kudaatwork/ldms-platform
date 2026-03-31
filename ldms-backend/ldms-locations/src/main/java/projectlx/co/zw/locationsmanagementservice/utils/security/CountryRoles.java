package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CountryRoles {

    CREATE_COUNTRY("CREATE_COUNTRY", "Creates country"),
    UPDATE_COUNTRY("UPDATE_COUNTRY", "Updates country information"),
    VIEW_COUNTRY_BY_ID("VIEW_COUNTRY_BY_ID", "Views country by id"),
    DELETE_COUNTRY("DELETE_COUNTRY", "Deletes country"),
    VIEW_ALL_COUNTRIES_AS_A_LIST("VIEW_ALL_COUNTRIES_AS_A_LIST", "Views all countries as a list"),
    VIEW_ALL_COUNTRIES_BY_MULTIPLE_FILTERS("VIEW_ALL_COUNTRIES_BY_MULTIPLE_FILTERS", "Views all countries by multiple filters"),
    EXPORT_COUNTRIES("EXPORT_COUNTRIES", "Exports countries"),
    IMPORT_COUNTRIES("IMPORT_COUNTRIES", "Imports countries from CSV");

    private final String roleName;
    private final String description;
}