package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EditLocalizedNameRequest {

    private Long id; // Identifier field

    // Basic information
    private String value;
    
    // Relationships
    private Long languageId;
    private String referenceType;
    private Long referenceId;
}