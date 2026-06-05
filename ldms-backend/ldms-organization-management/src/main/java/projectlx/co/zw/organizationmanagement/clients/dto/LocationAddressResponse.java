package projectlx.co.zw.organizationmanagement.clients.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationAddressResponse {

    private boolean success;
    private LocationAddressDto addressDto;
}
