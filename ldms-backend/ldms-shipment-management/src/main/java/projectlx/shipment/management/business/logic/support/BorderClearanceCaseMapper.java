package projectlx.shipment.management.business.logic.support;

import projectlx.shipment.management.model.BorderClearanceCase;
import projectlx.shipment.management.model.BorderClearanceDocument;
import projectlx.shipment.management.utils.dtos.BorderClearanceCaseDto;
import projectlx.shipment.management.utils.dtos.BorderClearanceDocumentDto;

import java.util.List;
import java.util.stream.Collectors;

public final class BorderClearanceCaseMapper {

    private BorderClearanceCaseMapper() {}

    public static BorderClearanceCaseDto toCaseDto(BorderClearanceCase bc) {
        if (bc == null) {
            return null;
        }
        BorderClearanceCaseDto dto = new BorderClearanceCaseDto();
        dto.setId(bc.getId());
        dto.setCaseNumber(bc.getCaseNumber());
        dto.setOrganizationId(bc.getOrganizationId());
        dto.setShipmentId(bc.getShipmentId());
        dto.setInventoryTransferId(bc.getInventoryTransferId());
        dto.setTripId(bc.getTripId());
        dto.setBorderName(bc.getBorderName());
        dto.setStatus(bc.getStatus() != null ? bc.getStatus().name() : null);
        dto.setNotes(bc.getNotes());
        dto.setClearedAt(bc.getClearedAt());
        dto.setClearedBy(bc.getClearedBy());
        dto.setEntityStatus(bc.getEntityStatus() != null ? bc.getEntityStatus().name() : null);
        dto.setCreatedAt(bc.getCreatedAt());
        dto.setCreatedBy(bc.getCreatedBy());
        dto.setModifiedAt(bc.getModifiedAt());
        dto.setModifiedBy(bc.getModifiedBy());
        return dto;
    }

    public static BorderClearanceCaseDto toCaseDtoWithDocuments(BorderClearanceCase bc,
                                                                  List<BorderClearanceDocument> documents) {
        BorderClearanceCaseDto dto = toCaseDto(bc);
        if (dto != null && documents != null) {
            dto.setDocuments(documents.stream()
                    .map(BorderClearanceCaseMapper::toDocumentDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public static BorderClearanceDocumentDto toDocumentDto(BorderClearanceDocument doc) {
        if (doc == null) {
            return null;
        }
        BorderClearanceDocumentDto dto = new BorderClearanceDocumentDto();
        dto.setId(doc.getId());
        dto.setCaseId(doc.getCaseId());
        dto.setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType().name() : null);
        dto.setFileUploadId(doc.getFileUploadId());
        dto.setFileName(doc.getFileName());
        dto.setDescription(doc.getDescription());
        dto.setEntityStatus(doc.getEntityStatus() != null ? doc.getEntityStatus().name() : null);
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setCreatedBy(doc.getCreatedBy());
        return dto;
    }
}
