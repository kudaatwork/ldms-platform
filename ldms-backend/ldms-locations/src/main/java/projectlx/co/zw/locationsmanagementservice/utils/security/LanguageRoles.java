package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LanguageRoles {

    CREATE_LANGUAGE("CREATE_LANGUAGE", "Creates language"),
    UPDATE_LANGUAGE("UPDATE_LANGUAGE", "Updates language information"),
    VIEW_LANGUAGE_BY_ID("VIEW_LANGUAGE_BY_ID", "Views language by id"),
    DELETE_LANGUAGE("DELETE_LANGUAGE", "Deletes language"),
    VIEW_ALL_LANGUAGES_AS_A_LIST("VIEW_ALL_LANGUAGES_AS_A_LIST", "Views all languages as a list"),
    VIEW_ALL_LANGUAGES_BY_MULTIPLE_FILTERS("VIEW_ALL_LANGUAGES_BY_MULTIPLE_FILTERS", "Views all languages by multiple filters"),
    EXPORT_LANGUAGES("EXPORT_LANGUAGES", "Exports languages"),
    IMPORT_LANGUAGES("IMPORT_LANGUAGES", "Imports languages from CSV");

    private final String roleName;
    private final String description;
}