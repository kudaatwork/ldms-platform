package projectlx.co.zw.organizationmanagement.clients;

import lombok.Getter;
import lombok.Setter;

/**
 * Minimal subset of user-management {@code UsersMultipleFiltersRequest} for Feign lookups.
 */
@Getter
@Setter
public class UsersMultipleFiltersFeignRequest {
    private int page;
    private int size;
    private String searchValue;
    private String email;
    private Long organizationId;
}
