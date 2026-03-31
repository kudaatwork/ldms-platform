package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@ToString
public class LocalizedNameMultipleFiltersRequest extends MultipleFiltersRequest {
    private String value;
    private Long languageId;
    private String referenceType;
    private EntityStatus entityStatus;
}