package projectlx.user.management.service.utils.security;

public enum UserRoleRoles {
    CREATE_USER_ROLE("CREATE_USER_ROLE", "Creates user role"),
    DELETE_USER_ROLE("DELETE_USER_ROLE", "Deletes user role"),
    UPDATE_USER_ROLE("UPDATE_USER_ROLE", "Updates user role information"),
    VIEW_USER_ROLE_BY_ID("VIEW_USER_ROLE_BY_ID", "Views user role by id"),
    VIEW_ALL_USER_ROLES_AS_A_LIST("VIEW_ALL_USER_ROLES_AS_A_LIST", "Views all user roles as a list"),
    VIEW_ALL_USER_ROLES_BY_MULTIPLE_FILTERS("VIEW_ALL_USER_ROLES_BY_MULTIPLE_FILTERS", "Views all user roles by multiple filters"),
    IMPORT_USER_ROLES("IMPORT_USER_ROLES", "Imports user roles data");

    private String roleName;
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    UserRoleRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}