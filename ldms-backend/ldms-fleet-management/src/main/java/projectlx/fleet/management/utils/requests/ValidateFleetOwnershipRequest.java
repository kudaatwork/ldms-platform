package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ValidateFleetOwnershipRequest {
    private Long registeringOrganizationId;
    private String ownershipType;
    private Long contractedTransporterOrganizationId;
}
