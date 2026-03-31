package projectlx.user.management.service.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UserGroupRoles {
    CREATE_USER_GROUP("CREATE_USER_GROUP", "Creates user group"),
    DELETE_USER_GROUP("DELETE_USER_GROUP", "Deletes user group"),
    UPDATE_USER_GROUP("UPDATE_USER_GROUP", "Updates user group information"),
    VIEW_USER_GROUP_BY_ID("VIEW_USER_GROUP_BY_ID", "Views user group by id"),
    VIEW_ALL_USER_GROUPS_AS_A_LIST("VIEW_ALL_USER_GROUPS_AS_A_LIST", "Views all user groups as a list"),
    VIEW_ALL_USER_GROUPS_BY_MULTIPLE_FILTERS("VIEW_ALL_USER_GROUPS_BY_MULTIPLE_FILTERS", "Views all user groups by multiple filters"),
    ASSIGN_USER_ROLES_TO_USER_GROUP("ASSIGN_USER_ROLES_TO_USER_GROUP", "Assigns user roles to a user group"),
    REMOVE_USER_ROLES_FROM_USER_GROUP("REMOVE_USER_ROLES_FROM_USER_GROUP", "Removes user roles from a user group"),
    EXPORT_USER_GROUPS("EXPORT_USER_GROUPS", "Exports user groups data"),
    IMPORT_USER_GROUPS("IMPORT_USER_GROUPS", "Imports user groups data");

    private final String roleName;
    private final String description;
}