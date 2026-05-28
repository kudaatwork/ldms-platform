package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class AgentMultipleFiltersRequest extends MultipleFiltersRequest {

    private Long organizationId;
    private String agentKind;
}
