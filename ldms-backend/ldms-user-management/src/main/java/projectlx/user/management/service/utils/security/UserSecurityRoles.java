package projectlx.user.management.service.utils.security;

public enum UserSecurityRoles {
    CREATE_USER_SECURITY("CREATE_USER_SECURITY","Creates user security"),
    DELETE_USER_SECURITY("DELETE_USER_SECURITY","Deletes user security"),
    UPDATE_USER_SECURITY("UPDATE_USER_SECURITY","Updates user security information"),
    VIEW_USER_SECURITY_BY_ID("VIEW_USER_SECURITY_BY_ID","Views user security by id"),
    VIEW_ALL_USER_SECURITIES_AS_A_LIST("VIEW_ALL_USER_SECURITIES_AS_A_LIST","Views all user securities as a list"),
    VIEW_USER_SECURITIES_BY_MULTIPLE_FILTERS("VIEW_USER_SECURITIES_BY_MULTIPLE_FILTERS","Views user securities by multiple filters");

    private String roleName;
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    UserSecurityRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
    
    @Override
    public String toString() {
        return this.roleName;
    }
}