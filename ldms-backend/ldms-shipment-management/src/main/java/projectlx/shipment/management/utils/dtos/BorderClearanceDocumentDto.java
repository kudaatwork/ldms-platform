package projectlx.shipment.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class BorderClearanceDocumentDto {

    private Long id;
    private Long caseId;
    private String documentType;
    private Long fileUploadId;
    private String fileName;
    private String description;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
}
