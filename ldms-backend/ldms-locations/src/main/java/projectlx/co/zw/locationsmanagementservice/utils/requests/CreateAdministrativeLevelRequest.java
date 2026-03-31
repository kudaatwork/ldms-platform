package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateAdministrativeLevelRequest {

    // Basic information
    private String name;
    private String code;
    private Integer level;
    private Long countryId;
    
    // Additional information
    private String description;
}