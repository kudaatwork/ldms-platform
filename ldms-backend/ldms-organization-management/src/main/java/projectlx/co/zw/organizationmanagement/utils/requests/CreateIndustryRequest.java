package projectlx.co.zw.organizationmanagement.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateIndustryRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 50)
    private String industryCode;

    @Size(max = 500)
    private String description;

    @Size(max = 200)
    private String regulatoryBodyName;

    @Size(max = 300)
    private String regulatoryBodyContactInfo;

    @Size(max = 1000)
    private String complianceRequirements;

    private Boolean active;
}
