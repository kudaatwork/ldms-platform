package projectlx.shipment.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class BorderClearanceCaseDto {

    private Long id;
    private String caseNumber;
    private Long organizationId;
    private Long shipmentId;
    private Long inventoryTransferId;
    private Long tripId;
    private String borderName;
    private String status;
    private String notes;
    private LocalDateTime clearedAt;
    private String clearedBy;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
    private List<BorderClearanceDocumentDto> documents;
}
