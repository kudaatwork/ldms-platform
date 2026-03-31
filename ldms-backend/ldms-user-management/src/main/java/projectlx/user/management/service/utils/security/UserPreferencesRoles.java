package projectlx.user.management.service.utils.security;

public enum UserPreferencesRoles {
    CREATE_USER_PREFERENCES("CREATE_USER_PREFERENCES", "Creates user preferences"),
    DELETE_USER_PREFERENCES("DELETE_USER_PREFERENCES", "Deletes user preferences"),
    UPDATE_USER_PREFERENCES("UPDATE_USER_PREFERENCES", "Updates user preferences information"),
    VIEW_USER_PREFERENCES_BY_ID("VIEW_USER_PREFERENCES_BY_ID", "Views user preferences by id"),
    VIEW_ALL_USER_PREFERENCES_AS_A_LIST("VIEW_ALL_USER_PREFERENCES_AS_A_LIST", "Views all user preferences as a list"),
    VIEW_ALL_USER_PREFERENCES_BY_MULTIPLE_FILTERS("VIEW_ALL_USER_PREFERENCES_BY_MULTIPLE_FILTERS", "Views all user preferences by multiple filters");

    private String roleName;
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    UserPreferencesRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}