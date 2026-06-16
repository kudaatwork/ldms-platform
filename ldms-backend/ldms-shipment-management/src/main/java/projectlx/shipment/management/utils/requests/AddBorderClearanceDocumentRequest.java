package projectlx.shipment.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AddBorderClearanceDocumentRequest {

    private Long caseId;
    private Long fileUploadId;
    private String documentType;
    private String fileName;
    private String description;
}
