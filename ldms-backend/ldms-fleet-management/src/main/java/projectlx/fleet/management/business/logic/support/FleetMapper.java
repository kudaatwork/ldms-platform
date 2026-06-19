package projectlx.fleet.management.business.logic.support;

import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.model.FleetComplianceRecord;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.model.FleetTrackingDevice;
import projectlx.fleet.management.utils.dtos.FleetAssetDto;
import projectlx.fleet.management.utils.dtos.FleetComplianceRecordDto;
import projectlx.fleet.management.utils.dtos.FleetDriverDto;
import projectlx.fleet.management.utils.dtos.FleetTrackingDeviceDto;

public final class FleetMapper {

    private FleetMapper() {}

    public static FleetAssetDto toDto(FleetAsset entity) {
        FleetAssetDto dto = new FleetAssetDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setAssetType(entity.getAssetType() != null ? entity.getAssetType().name() : null);
        dto.setOwnershipType(entity.getOwnershipType() != null ? entity.getOwnershipType().name() : null);
        dto.setContractedTransporterOrganizationId(entity.getContractedTransporterOrganizationId());
        dto.setContractScope(entity.getContractScope() != null ? entity.getContractScope().name() : null);
        dto.setJobReference(entity.getJobReference());
        dto.setContractStartDate(entity.getContractStartDate());
        dto.setContractEndDate(entity.getContractEndDate());
        dto.setRegistrationStatus(entity.getRegistrationStatus() != null ? entity.getRegistrationStatus().name() : null);
        dto.setRegistration(entity.getRegistration());
        dto.setMakeModel(entity.getMakeModel());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setDriverName(entity.getDriverName());
        dto.setFleetDriverId(entity.getFleetDriverId());
        dto.setUtilizationPct(entity.getUtilizationPct());
        dto.setMaxSpeedKmh(entity.getMaxSpeedKmh());
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
        dto.setEmploymentType(entity.getEmploymentType() != null ? entity.getEmploymentType().name() : null);
        dto.setMarketplaceVisible(entity.getMarketplaceVisible());
        dto.setRosterSource("organization");

        // Personal details
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setLicenseNumber(entity.getLicenseNumber());
        dto.setLicenseClass(entity.getLicenseClass());
        dto.setLicenseUploadId(entity.getLicenseUploadId());
        dto.setLicenseBackUploadId(entity.getLicenseBackUploadId());

        // Identity documents
        dto.setNationalIdNumber(entity.getNationalIdNumber());
        dto.setNationalIdExpiryDate(entity.getNationalIdExpiryDate());
        dto.setNationalIdUploadId(entity.getNationalIdUploadId());
        dto.setNationalIdBackUploadId(entity.getNationalIdBackUploadId());
        dto.setPassportNumber(entity.getPassportNumber());
        dto.setPassportExpiryDate(entity.getPassportExpiryDate());
        dto.setPassportUploadId(entity.getPassportUploadId());

        // Residential address
        dto.setAddressLine1(entity.getAddressLine1());
        dto.setAddressLine2(entity.getAddressLine2());
        dto.setAddressCity(entity.getAddressCity());
        dto.setAddressProvince(entity.getAddressProvince());
        dto.setAddressPostalCode(entity.getAddressPostalCode());
        dto.setAddressCountry(entity.getAddressCountry());

        // Audit
        dto.setEntityStatus(entity.getEntityStatus() != null ? entity.getEntityStatus().name() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setModifiedAt(entity.getModifiedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        return dto;
    }

    public static FleetDriverDto toPartnerRosterDto(FleetDriver entity, String partnerOrganizationName) {
        FleetDriverDto dto = toDto(entity);
        dto.setRosterSource("transport_partner");
        dto.setHomeOrganizationName(partnerOrganizationName);
        return dto;
    }

    public static FleetTrackingDeviceDto toTrackingDeviceDto(FleetTrackingDevice entity, FleetAsset asset) {
        return toTrackingDeviceDto(entity, asset, false);
    }

    public static FleetTrackingDeviceDto toTrackingDeviceDto(
            FleetTrackingDevice entity, FleetAsset asset, boolean maskIngestKey) {
        FleetTrackingDeviceDto dto = new FleetTrackingDeviceDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setFleetAssetId(entity.getFleetAssetId());
        dto.setFleetDriverId(entity.getFleetDriverId());
        dto.setLinkedUserId(entity.getLinkedUserId());
        dto.setDeviceType(entity.getDeviceType() != null ? entity.getDeviceType().name() : null);
        dto.setInstallStatus(entity.getInstallStatus() != null ? entity.getInstallStatus().name() : null);
        dto.setIntegrationProvider(entity.getIntegrationProvider() != null ? entity.getIntegrationProvider().name() : null);
        dto.setDeviceLabel(entity.getDeviceLabel());
        dto.setDeviceSerial(entity.getDeviceSerial());
        dto.setExternalDeviceId(entity.getExternalDeviceId());
        dto.setIngestKey(maskIngestKey ? maskIngestKey(entity.getIngestKey()) : entity.getIngestKey());
        dto.setTracksGps(entity.isTracksGps());
        dto.setTracksFuel(entity.isTracksFuel());
        dto.setMqttTopic(entity.getMqttTopic());
        dto.setInstalledAt(entity.getInstalledAt());
        dto.setLastTelemetryAt(entity.getLastTelemetryAt());
        dto.setNotes(entity.getNotes());
        dto.setEntityStatus(entity.getEntityStatus() != null ? entity.getEntityStatus().name() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setModifiedAt(entity.getModifiedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        // Denormalised asset fields
        if (asset != null) {
            dto.setVehicleRegistration(asset.getRegistration());
            dto.setVehicleMakeModel(asset.getMakeModel());
        }
        return dto;
    }

    public static String maskIngestKey(String ingestKey) {
        if (ingestKey == null || ingestKey.length() <= 16) {
            return ingestKey;
        }
        return ingestKey.substring(0, 8) + "…" + ingestKey.substring(ingestKey.length() - 4);
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
