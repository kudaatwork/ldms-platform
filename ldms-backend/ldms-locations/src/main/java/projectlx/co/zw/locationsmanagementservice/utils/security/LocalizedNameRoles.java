package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LocalizedNameRoles {

    CREATE_LOCALIZED_NAME("CREATE_LOCALIZED_NAME", "Creates localized name"),
    UPDATE_LOCALIZED_NAME("UPDATE_LOCALIZED_NAME", "Updates localized name information"),
    VIEW_LOCALIZED_NAME_BY_ID("VIEW_LOCALIZED_NAME_BY_ID", "Views localized name by id"),
    DELETE_LOCALIZED_NAME("DELETE_LOCALIZED_NAME", "Deletes localized name"),
    VIEW_ALL_LOCALIZED_NAMES_AS_A_LIST("VIEW_ALL_LOCALIZED_NAMES_AS_A_LIST", "Views all localized names as a list"),
    VIEW_ALL_LOCALIZED_NAMES_BY_MULTIPLE_FILTERS("VIEW_ALL_LOCALIZED_NAMES_BY_MULTIPLE_FILTERS", "Views all localized names by multiple filters"),
    EXPORT_LOCALIZED_NAMES("EXPORT_LOCALIZED_NAMES", "Exports localized names"),
    IMPORT_LOCALIZED_NAMES("IMPORT_LOCALIZED_NAMES", "Imports localized names from CSV");

    private final String roleName;
    private final String description;
}