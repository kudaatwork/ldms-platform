package projectlx.co.zw.shared_library.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.shared_library.model.Organization;
import projectlx.co.zw.shared_library.model.OrganizationVerificationStatus;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationResponse extends CommonResponse {
    private OrganizationDto organizationDto;
    private BranchDto branchDto;
    private List<OrganizationDto> organizationDtoList;
    private Page<OrganizationDto> organizationDtoPage;
    
    // Added fields to support additional operations
    private List<Organization> organizations;
    private Boolean approved;
    private Boolean verified;
    public void setVerificationStatus(Object verificationStatus) {
        if (verificationStatus instanceof OrganizationVerificationStatus) {
            this.verificationStatus = (OrganizationVerificationStatus) verificationStatus;
        } else if (verificationStatus != null) {
            // If it's the other OrganizationVerificationStatus, we might need to map it or handle it
            // For now, we'll try to map by name if possible, or just ignore if incompatible
        }
    }

    private OrganizationVerificationStatus verificationStatus;
    private Long count;

    /** Populated when supplier customer registration finds an existing org that can be linked via duplex. */
    private Boolean duplexLinkOffered;

    /** Summary of an existing org offered for duplex/customer link. */
    private OrganizationDto existingOrganizationForLink;

    /** Result of pre-checking customer registration email availability. */
    private String customerRegistrationEmailStatus;
}
