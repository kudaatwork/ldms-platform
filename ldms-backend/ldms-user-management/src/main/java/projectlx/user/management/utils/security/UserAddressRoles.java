package projectlx.user.management.utils.security;

public enum UserAddressRoles {
    CREATE_USER_ADDRESS("CREATE_USER_ADDRESS", "Creates user address"),
    DELETE_USER_ADDRESS("DELETE_USER_ADDRESS", "Deletes user address"),
    UPDATE_USER_ADDRESS("UPDATE_USER_ADDRESS", "Updates user address information"),
    VIEW_USER_ADDRESS_BY_ID("VIEW_USER_ADDRESS_BY_ID", "Views user address by id"),
    VIEW_ALL_USER_ADDRESSES_AS_A_LIST("VIEW_ALL_USER_ADDRESSES_AS_A_LIST", "Views all user addresses as a list"),
    VIEW_ALL_USER_ADDRESSES_BY_MULTIPLE_FILTERS("VIEW_ALL_USER_ADDRESSES_BY_MULTIPLE_FILTERS", "Views all user addresses by multiple filters"),
    EXPORT_USER_ADDRESSES("EXPORT_USER_ADDRESSES", "Exports user addresses data");

    private String roleName;
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    UserAddressRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}