package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DistrictRoles {

    CREATE_DISTRICT("CREATE_DISTRICT", "Creates district"),
    UPDATE_DISTRICT("UPDATE_DISTRICT", "Updates district information"),
    VIEW_DISTRICT_BY_ID("VIEW_DISTRICT_BY_ID", "Views district by id"),
    DELETE_DISTRICT("DELETE_DISTRICT", "Deletes district"),
    VIEW_ALL_DISTRICTS_AS_A_LIST("VIEW_ALL_DISTRICTS_AS_A_LIST", "Views all districts as a list"),
    VIEW_ALL_DISTRICTS_BY_MULTIPLE_FILTERS("VIEW_ALL_DISTRICTS_BY_MULTIPLE_FILTERS", "Views all districts by multiple filters"),
    EXPORT_DISTRICTS("EXPORT_DISTRICTS", "Exports districts"),
    IMPORT_DISTRICTS("IMPORT_DISTRICTS", "Imports districts from CSV");

    private final String roleName;
    private final String description;
}