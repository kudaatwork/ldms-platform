package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProvinceRoles {

    CREATE_PROVINCE("CREATE_PROVINCE", "Creates province"),
    UPDATE_PROVINCE("UPDATE_PROVINCE", "Updates province information"),
    VIEW_PROVINCE_BY_ID("VIEW_PROVINCE_BY_ID", "Views province by id"),
    DELETE_PROVINCE("DELETE_PROVINCE", "Deletes province"),
    VIEW_ALL_PROVINCES_AS_A_LIST("VIEW_ALL_PROVINCES_AS_A_LIST", "Views all provinces as a list"),
    VIEW_ALL_PROVINCES_BY_MULTIPLE_FILTERS("VIEW_ALL_PROVINCES_BY_MULTIPLE_FILTERS", "Views all provinces by multiple filters"),
    EXPORT_PROVINCES("EXPORT_PROVINCES", "Exports provinces"),
    IMPORT_PROVINCES("IMPORT_PROVINCES", "Imports provinces from CSV");

    private final String roleName;
    private final String description;
}