package projectlx.co.zw.organizationmanagement.utils.responses;

import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationKycReviewDto;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.List;

@Getter
@Setter
public class OrganizationManagementResponse extends OrganizationResponse {

    private List<OrganizationKycReviewDto> kycReviews;
}
