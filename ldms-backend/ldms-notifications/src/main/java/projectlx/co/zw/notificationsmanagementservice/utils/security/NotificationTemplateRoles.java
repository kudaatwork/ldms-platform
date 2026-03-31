package projectlx.co.zw.notificationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NotificationTemplateRoles {

    CREATE_TEMPLATE("CREATE_TEMPLATE", "Creates notification template"),
    DELETE_TEMPLATE("DELETE_TEMPLATE", "Deletes notification template"),
    UPDATE_TEMPLATE("UPDATE_TEMPLATE", "Updates notification template information"),
    VIEW_TEMPLATE_BY_ID("VIEW_TEMPLATE_BY_ID", "Views notification template by id"),
    VIEW_ALL_TEMPLATES_AS_A_LIST("VIEW_ALL_TEMPLATES_AS_A_LIST", "Views all notification templates as a list"),
    VIEW_ALL_TEMPLATES_BY_MULTIPLE_FILTERS("VIEW_ALL_TEMPLATES_BY_MULTIPLE_FILTERS", "Views all notification templates by multiple filters");

    private final String roleName;
    private final String description;
}