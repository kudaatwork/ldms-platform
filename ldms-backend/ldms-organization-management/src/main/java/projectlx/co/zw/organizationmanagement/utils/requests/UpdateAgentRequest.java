package projectlx.co.zw.organizationmanagement.utils.requests;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAgentRequest {

    private Long organizationId;

    @Size(max = 50)
    private String agentKind;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String phoneNumber;

    @Size(max = 50)
    private String agentType;

    @Size(max = 100)
    private String role;

    private Long branchId;

    private Long representedOrganizationId;

    private Long locationId;

    @Size(max = 200)
    private String assignedRegion;

    private Boolean active;
}
