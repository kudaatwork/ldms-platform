package projectlx.co.zw.fleetmanagement.business.logic.support;

import projectlx.co.zw.fleetmanagement.model.FleetAsset;
import projectlx.co.zw.fleetmanagement.model.FleetComplianceRecord;
import projectlx.co.zw.fleetmanagement.model.FleetDriver;
import projectlx.co.zw.fleetmanagement.utils.dtos.FleetAssetDto;
import projectlx.co.zw.fleetmanagement.utils.dtos.FleetComplianceRecordDto;
import projectlx.co.zw.fleetmanagement.utils.dtos.FleetDriverDto;

public final class FleetMapper {

    private FleetMapper() {}

    public static FleetAssetDto toDto(FleetAsset entity) {
        FleetAssetDto dto = new FleetAssetDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setAssetType(entity.getAssetType() != null ? entity.getAssetType().name() : null);
        dto.setOwnershipType(entity.getOwnershipType() != null ? entity.getOwnershipType().name() : null);
        dto.setContractedTransporterOrganizationId(entity.getContractedTransporterOrganizationId());
        dto.setRegistration(entity.getRegistration());
        dto.setMakeModel(entity.getMakeModel());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setDriverName(entity.getDriverName());
        dto.setUtilizationPct(entity.getUtilizationPct());
        dto.setEntityStatus(entity.getEntityStatus() != null ? entity.getEntityStatus().name() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setModifiedAt(entity.getModifiedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        return dto;
    }

    public static FleetDriverDto toDto(FleetDriver entity) {
        FleetDriverDto dto = new FleetDriverDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setUserId(entity.getUserId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setLicenseNumber(entity.getLicenseNumber());
        dto.setLicenseClass(entity.getLicenseClass());
        dto.setEntityStatus(entity.getEntityStatus() != null ? entity.getEntityStatus().name() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setModifiedAt(entity.getModifiedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        return dto;
    }

    public static FleetComplianceRecordDto toDto(FleetComplianceRecord entity) {
        FleetComplianceRecordDto dto = new FleetComplianceRecordDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setSubjectType(entity.getSubjectType() != null ? entity.getSubjectType().name() : null);
        dto.setSubjectId(entity.getSubjectId());
        dto.setComplianceType(entity.getComplianceType() != null ? entity.getComplianceType().name() : null);
        dto.setFileUploadId(entity.getFileUploadId());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setNotes(entity.getNotes());
        dto.setEntityStatus(entity.getEntityStatus() != null ? entity.getEntityStatus().name() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setModifiedAt(entity.getModifiedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        return dto;
    }
}
