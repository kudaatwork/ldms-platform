package projectlx.user.management.service.utils.security;

public enum UserPasswordRoles {
    CHANGE_USER_PASSWORD("CHANGE_USER_PASSWORD", "Changes user password"),
    RESET_USER_PASSWORD("RESET_USER_PASSWORD", "Resets user password");

    private String roleName;
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    UserPasswordRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}