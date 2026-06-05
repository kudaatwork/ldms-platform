package projectlx.co.zw.organizationmanagement.clients.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationAddressCreateRequest {

    private String line1;
    private String line2;
    private String postalCode;
    private Long suburbId;
}
