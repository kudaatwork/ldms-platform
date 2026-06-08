package projectlx.co.zw.fleetmanagement.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FleetRoles {

    VIEW_FLEET_ASSETS("VIEW_FLEET_ASSETS", "Views fleet assets"),
    CREATE_FLEET_ASSET("CREATE_FLEET_ASSET", "Creates fleet assets"),
    UPDATE_FLEET_ASSET("UPDATE_FLEET_ASSET", "Updates fleet assets"),
    DELETE_FLEET_ASSET("DELETE_FLEET_ASSET", "Deletes fleet assets"),
    VIEW_FLEET_DRIVERS("VIEW_FLEET_DRIVERS", "Views fleet drivers"),
    CREATE_FLEET_DRIVER("CREATE_FLEET_DRIVER", "Creates fleet drivers"),
    UPDATE_FLEET_DRIVER("UPDATE_FLEET_DRIVER", "Updates fleet drivers"),
    DELETE_FLEET_DRIVER("DELETE_FLEET_DRIVER", "Deletes fleet drivers"),
    VIEW_FLEET_COMPLIANCE("VIEW_FLEET_COMPLIANCE", "Views fleet compliance records"),
    CREATE_FLEET_COMPLIANCE("CREATE_FLEET_COMPLIANCE", "Creates fleet compliance records"),
    UPDATE_FLEET_COMPLIANCE("UPDATE_FLEET_COMPLIANCE", "Updates fleet compliance records");

    private final String roleName;
    private final String description;

    @Override
    public String toString() {
        return roleName;
    }
}
