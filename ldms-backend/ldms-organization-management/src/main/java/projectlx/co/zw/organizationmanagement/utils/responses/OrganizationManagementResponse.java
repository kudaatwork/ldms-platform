package projectlx.co.zw.organizationmanagement.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import projectlx.co.zw.organizationmanagement.utils.dtos.FleetVehicleDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryUsageDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.KycApprovalPolicyDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.OnboardingStatusDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationKycReviewDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.TradingPartnerDto;
import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationManagementResponse extends OrganizationResponse {

    private List<OrganizationKycReviewDto> kycReviews;
    private IndustryDto industryDto;
    private List<IndustryUsageDto> industryUsageDtoList;
    private Page<IndustryUsageDto> industryUsageDtoPage;
    private Page<BranchDto> branchDtoPage;
    private Page<AgentDto> agentDtoPage;
    private BranchDto branchDto;
    private AgentDto agentDto;
    private KycApprovalPolicyDto kycApprovalPolicyDto;
    private OnboardingStatusDto onboardingStatusDto;
    private FleetVehicleDto fleetVehicleDto;
    private List<FleetVehicleDto> fleetVehicleDtoList;

    private TradingPartnerDto tradingPartnerDto;
    private List<TradingPartnerDto> tradingPartnerDtoList;
}
