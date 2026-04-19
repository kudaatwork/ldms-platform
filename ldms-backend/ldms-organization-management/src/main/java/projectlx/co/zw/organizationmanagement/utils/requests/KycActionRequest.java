package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycActionRequest {

    private String notes;
    /** When omitted on system calls, defaults to the acting principal (e.g. SYSTEM). */
    private String reviewerUsername;
    private Long reviewerUserId;
}
