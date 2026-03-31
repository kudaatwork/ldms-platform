package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EditAdministrativeLevelRequest {

    private Long id; // Identifier field

    // Basic information
    private String name;
    private String code;
    private Integer level;
    private Long countryId;
    
    // Additional information
    private String description;
}