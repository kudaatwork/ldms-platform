package projectlx.user.management.utils.support;

import java.util.List;
import projectlx.user.management.utils.dtos.UserRoleDto;

/**
 * Populates derived module metadata on {@link UserRoleDto} instances for admin UI grouping.
 */
public final class UserRoleDtoModuleEnricher {

    private UserRoleDtoModuleEnricher() {
    }

    public static void enrich(UserRoleDto dto) {
        if (dto == null || dto.getRole() == null) {
            return;
        }
        LdmsRoleModuleResolver.RoleModule module = LdmsRoleModuleResolver.resolve(dto.getRole());
        dto.setModuleKey(module.key());
        dto.setModuleLabel(module.label());
    }

    public static void enrichAll(List<UserRoleDto> dtos) {
        if (dtos == null) {
            return;
        }
        for (UserRoleDto dto : dtos) {
            enrich(dto);
        }
    }
}
