package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateLocalizedNameRequest {

    // Basic information
    private String value;
    
    // Relationships
    private Long languageId;
    private String referenceType;
    private Long referenceId;
}