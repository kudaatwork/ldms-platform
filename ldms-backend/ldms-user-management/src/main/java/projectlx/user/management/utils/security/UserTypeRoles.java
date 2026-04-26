package projectlx.user.management.utils.security;

public enum UserTypeRoles {
    CREATE_USER_TYPE("CREATE_USER_TYPE", "Creates user type"),
    DELETE_USER_TYPE("DELETE_USER_TYPE", "Deletes user type"),
    UPDATE_USER_TYPE("UPDATE_USER_TYPE", "Updates user type information"),
    VIEW_USER_TYPE_BY_ID("VIEW_USER_TYPE_BY_ID", "Views user type by id"),
    VIEW_ALL_USER_TYPES_AS_A_LIST("VIEW_ALL_USER_TYPES_AS_A_LIST", "Views all user types as a list"),
    VIEW_ALL_USER_TYPES_BY_MULTIPLE_FILTERS("VIEW_ALL_USER_TYPES_BY_MULTIPLE_FILTERS", "Views all user types by multiple filters"),
    EXPORT_USER_TYPES("EXPORT_USER_TYPES", "Exports user types data"),
    IMPORT_USER_TYPES("IMPORT_USER_TYPES", "Imports user types data");

    private String roleName;
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    UserTypeRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}