package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class AddressMultipleFiltersRequest extends MultipleFiltersRequest {
    private String line1;
    private String line2;
    private String postalCode;
    private EntityStatus entityStatus;
}
