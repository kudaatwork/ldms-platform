package projectlx.co.zw.organizationmanagement.clients;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueOrganizationContactCredentialsRequest {

    private Long organizationId;
    private Long contactUserId;
}
