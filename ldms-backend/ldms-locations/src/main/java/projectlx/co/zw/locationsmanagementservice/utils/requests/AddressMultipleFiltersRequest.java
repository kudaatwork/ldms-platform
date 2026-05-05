package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.locationsmanagementservice.utils.enums.SettlementType;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@ToString
public class AddressMultipleFiltersRequest extends MultipleFiltersRequest {
    private String line1;
    private String line2;
    private String postalCode;
    private SettlementType settlementType;
    private Long settlementId;
    private Long suburbId;
    private Long villageId;
    private Long cityId;
    private Long districtId;
    private Long provinceId;
    private Long countryId;
    private EntityStatus entityStatus;
}