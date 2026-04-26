package projectlx.user.management.utils.security;

public enum UserAccountRoles {
    CREATE_USER_ACCOUNT("CREATE_USER_ACCOUNT", "Creates user account"),
    DELETE_USER_ACCOUNT("DELETE_USER_ACCOUNT", "Deletes user account"),
    UPDATE_USER_ACCOUNT("UPDATE_USER_ACCOUNT", "Updates user account information"),
    VIEW_USER_ACCOUNT_BY_ID("VIEW_USER_ACCOUNT_BY_ID", "Views user account by id"),
    VIEW_ALL_USER_ACCOUNTS_AS_A_LIST("VIEW_ALL_USER_ACCOUNTS_AS_A_LIST", "Views all user accounts as a list"),
    VIEW_ALL_USER_ACCOUNTS_BY_MULTIPLE_FILTERS("VIEW_ALL_USER_ACCOUNTS_BY_MULTIPLE_FILTERS", "Views all user accounts by multiple filters"),
    EXPORT_USER_ACCOUNTS("EXPORT_USER_ACCOUNTS", "Exports user accounts data");

    private String roleName;
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    UserAccountRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}