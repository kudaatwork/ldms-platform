package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueOrganizationContactCredentialsRequest {

    @NotNull
    @Min(1)
    private Long organizationId;

    /** When omitted, the contact person linked to the organisation is resolved by email. */
    private Long contactUserId;
}
