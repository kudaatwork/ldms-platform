package projectlx.co.zw.organizationmanagement.clients.dto;

import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

/**
 * Mirrors {@code AddressResponse} from ldms-locations ({@code CommonResponse} + {@code addressDto}).
 */
@Getter
@Setter
public class LocationAddressResponse extends CommonResponse {

    private LocationAddressDto addressDto;
}
