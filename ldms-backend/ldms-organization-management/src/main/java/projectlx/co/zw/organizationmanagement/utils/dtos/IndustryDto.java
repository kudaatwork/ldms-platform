package projectlx.co.zw.organizationmanagement.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndustryDto {

    private Long id;
    private String name;
    private String industryCode;
    private String description;
    private String regulatoryBodyName;
    private String regulatoryBodyContactInfo;
    private String complianceRequirements;
    private boolean active;
}
