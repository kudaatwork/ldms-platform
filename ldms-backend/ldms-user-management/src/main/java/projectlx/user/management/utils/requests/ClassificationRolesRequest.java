package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Admin-portal request to assign or remove catalog roles for a single organisation classification
 * (e.g. SUPPLIER, TRANSPORT_COMPANY). Drives the editable classification drill-down on the
 * Group Roles Mapping card.
 */
@Getter
@Setter
@ToString
public class ClassificationRolesRequest {
    /** Organisation classification key the roles apply to. */
    @NotNull
    private String classification;
    /** Catalog role ids being toggled for the classification. */
    @NotEmpty
    private List<Long> roleIds;
}
