package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class BranchMultipleFiltersRequest extends MultipleFiltersRequest {

    private String branchName;
    private Long organizationId;
    /** {@code BRANCH} or {@code SUB_BRANCH} */
    private String branchLevel;
    private Boolean depot;
    private Long parentBranchId;
    private String region;
    private Boolean active;
}
