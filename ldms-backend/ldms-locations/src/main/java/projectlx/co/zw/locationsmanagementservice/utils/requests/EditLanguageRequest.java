package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EditLanguageRequest {

    private Long id; // Identifier field

    // Basic information
    private String name;
    private String isoCode;
    private String nativeName;
    private Boolean isDefault;
}