package projectlx.user.management.business.logic.support;

import java.util.List;

/**
 * Platform-portal user type visibility: bootstrap shows only {@link #SYSTEM_ADMINISTRATOR};
 * internal provisioning types stay hidden from organisation workspace UI.
 */
public final class OrganizationPortalUserTypePolicy {

    public static final String SYSTEM_ADMINISTRATOR = "System Administrator";

    public static final List<String> INTERNAL_USER_TYPES = List.of(
            "ORGANIZATION_CONTACT",
            "ORGANIZATION_MEMBER");

    private OrganizationPortalUserTypePolicy() {
    }
}
