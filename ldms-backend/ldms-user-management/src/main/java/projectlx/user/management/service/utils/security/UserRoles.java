package projectlx.user.management.service.utils.security;

public enum UserRoles {
    CREATE_USER("CREATE_USER","Creates users"),
    DELETE_USER("DELETE_USER","Deletes users"),
    UPDATE_USER("UPDATE_USER","Updates user information"),
    VIEW_USER_BY_ID("VIEW_USER_BY_ID","Views user by id"),
    VIEW_ALL_USERS_AS_A_LIST("VIEW_ALL_USERS_AS_A_LIST","Views all users as a list"),
    VIEW_ALL_USERS_BY_MULTIPLE_FILTERS("VIEW_ALL_USERS_BY_MULTIPLE_FILTERS","Views all users by multiple filters"),
    VIEW_USER_BY_USERNAME("VIEW_USER_BY_USERNAME","Views user by username"),
    VIEW_USER_BY_PHONE_NUMBER_OR_EMAIL("VIEW_USER_BY_PHONE_NUMBER_OR_EMAIL","Views user by phone number or email"),
    VIEW_USERS_BY_ORGANIZATION("VIEW_USERS_BY_ORGANIZATION","Views users by organization"),
    VIEW_USERS_BY_BRANCH("VIEW_USERS_BY_BRANCH","Views users by branch"),
    VIEW_USERS_BY_AGENT("VIEW_USERS_BY_AGENT","Views users by agent"),
    EXPORT_USERS("EXPORT_USERS","Exports users data"),
    IMPORT_USERS("IMPORT_USERS","Imports users data"),
    VERIFY_USER_EMAIL("VERIFY_USER_EMAIL", "Verifies user's email"),
    RESEND_VERIFICATION_LINK("RESEND_VERIFICATION_LINK", "Resends the verification link to user's email"),
    FORGOT_PASSWORD("FORGOT_PASSWORD", "Used by the user when they have forgotten their password"),
    VALIDATE_RESET_TOKEN("VALIDATE_RESET_TOKEN", "Used to validate user's token");

    private String roleName;
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    UserRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}
