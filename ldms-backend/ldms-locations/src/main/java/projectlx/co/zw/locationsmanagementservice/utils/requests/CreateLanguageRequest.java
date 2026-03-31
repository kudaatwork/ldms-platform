package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateLanguageRequest {

    // Basic information
    private String name;
    private String isoCode;
    private String nativeName;
    private Boolean isDefault;
}