package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycRejectRequest extends KycActionRequest {

    private String rejectionReason;
}
