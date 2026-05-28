package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class OrganizationMultipleFiltersRequest extends MultipleFiltersRequest {
    private String name;
    private String email;
    /** {@link projectlx.co.zw.organizationmanagement.model.OrganizationClassification} name. */
    private String organizationClassification;
    /** {@link projectlx.co.zw.organizationmanagement.model.KycStatus} name. */
    private String kycStatus;
    /**
     * When true and {@code kycStatus} is blank, restricts to platform signup organisations in the KYC pipeline
     * (draft, submitted, stage reviews, resubmitted, rejected).
     */
    private Boolean kycQueueOnly;

    /**
     * When true, limits to organisations shown in the admin directory: admin-registered or signup orgs
     * that completed KYC ({@code kycStatus=APPROVED}). Excludes signup orgs still in the KYC pipeline.
     */
    private Boolean organizationDirectoryOnly;

    /**
     * Limits KYC queue rows to organisations assigned to this reviewer for their active stage
     * (stage 1: submitted / stage-1 review / resubmitted; stage 2: stage-2 review).
     */
    private String kycAssignedToUsername;

    /** Filter organisations linked to this industry. */
    private Long industryId;
}
