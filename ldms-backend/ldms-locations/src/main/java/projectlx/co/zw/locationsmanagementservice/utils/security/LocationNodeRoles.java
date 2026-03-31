package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LocationNodeRoles {
    CREATE_LOCATION_NODE("CREATE_LOCATION_NODE", "Creates location hierarchy node"),
    UPDATE_LOCATION_NODE("UPDATE_LOCATION_NODE", "Updates location hierarchy node"),
    VIEW_LOCATION_NODE_BY_ID("VIEW_LOCATION_NODE_BY_ID", "Views location hierarchy node by id"),
    VIEW_LOCATION_NODE_BY_PARENT("VIEW_LOCATION_NODE_BY_PARENT", "Views location hierarchy node by parent"),
    VIEW_LOCATION_NODE_BY_FILTERS("VIEW_LOCATION_NODE_BY_FILTERS", "Views location hierarchy nodes by filters"),
    DELETE_LOCATION_NODE("DELETE_LOCATION_NODE", "Soft-deletes location hierarchy node");

    private final String roleName;
    private final String description;
}
