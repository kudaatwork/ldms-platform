package projectlx.fleet.management.business.logic.support;

import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.model.FleetTrackingDevice;
import projectlx.fleet.management.utils.dtos.FleetTrackingDeviceDto;
import projectlx.fleet.management.utils.dtos.FleetTrackingIntegrationCredentialDto;

public final class FleetTrackingIntegrationCredentialMapper {

    private FleetTrackingIntegrationCredentialMapper() {}

    public static FleetTrackingIntegrationCredentialDto fromDevice(
            FleetTrackingDevice device, FleetAsset asset, boolean maskIngestKey) {
        FleetTrackingDeviceDto deviceDto = FleetMapper.toTrackingDeviceDto(device, asset, maskIngestKey);
        return fromDeviceDto(deviceDto);
    }

    public static FleetTrackingIntegrationCredentialDto fromDeviceDto(FleetTrackingDeviceDto deviceDto) {
        if (deviceDto == null) {
            return null;
        }
        FleetTrackingIntegrationCredentialDto dto = new FleetTrackingIntegrationCredentialDto();
        dto.setId(deviceDto.getId());
        dto.setOrganizationId(deviceDto.getOrganizationId());
        dto.setCredentialLabel(deviceDto.getDeviceLabel());
        dto.setIngestKey(deviceDto.getIngestKey());
        dto.setIntegrationProvider(deviceDto.getIntegrationProvider());
        dto.setStatus(deviceDto.getInstallStatus());
        dto.setFleetAssetId(deviceDto.getFleetAssetId());
        dto.setVehicleRegistration(deviceDto.getVehicleRegistration());
        dto.setVehicleMakeModel(deviceDto.getVehicleMakeModel());
        dto.setExternalDeviceId(deviceDto.getExternalDeviceId());
        dto.setMqttTopic(deviceDto.getMqttTopic());
        dto.setLastTelemetryAt(deviceDto.getLastTelemetryAt());
        dto.setCreatedAt(deviceDto.getCreatedAt());
        return dto;
    }
}
