package projectlx.fuel.expenses.business.logic.support;

import org.springframework.stereotype.Component;
import projectlx.fuel.expenses.model.FuelTelemetryLog;
import projectlx.fuel.expenses.model.OperationalFundRequest;
import projectlx.fuel.expenses.utils.dtos.FuelTelemetryLogDto;
import projectlx.fuel.expenses.utils.dtos.OperationalFundRequestDto;

@Component
public class FuelExpensesMapper {

    public OperationalFundRequestDto toDto(OperationalFundRequest entity) {
        if (entity == null) {
            return null;
        }
        OperationalFundRequestDto dto = new OperationalFundRequestDto();
        dto.setId(entity.getId());
        dto.setRequestNumber(entity.getRequestNumber());
        dto.setTripId(entity.getTripId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setFleetDriverId(entity.getFleetDriverId());
        dto.setFleetAssetId(entity.getFleetAssetId());
        dto.setRequestType(entity.getRequestType());
        dto.setStatus(entity.getStatus());
        dto.setLitersRequested(entity.getLitersRequested());
        dto.setAmountRequested(entity.getAmountRequested());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setDriverNotes(entity.getDriverNotes());
        dto.setApprovedLiters(entity.getApprovedLiters());
        dto.setApprovedAmount(entity.getApprovedAmount());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setDecidedBy(entity.getDecidedBy());
        dto.setDecidedAt(entity.getDecidedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setModifiedAt(entity.getModifiedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        return dto;
    }

    public FuelTelemetryLogDto toDto(FuelTelemetryLog entity) {
        if (entity == null) {
            return null;
        }
        FuelTelemetryLogDto dto = new FuelTelemetryLogDto();
        dto.setId(entity.getId());
        dto.setFuelSessionId(entity.getFuelSessionId());
        dto.setTripId(entity.getTripId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setFleetAssetId(entity.getFleetAssetId());
        dto.setSource(entity.getSource());
        dto.setReadingType(entity.getReadingType());
        dto.setFuelLevelPct(entity.getFuelLevelPct());
        dto.setFuelLiters(entity.getFuelLiters());
        dto.setOdometerKm(entity.getOdometerKm());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setDistanceDeltaKm(entity.getDistanceDeltaKm());
        dto.setConsumedLiters(entity.getConsumedLiters());
        dto.setRecordedAt(entity.getRecordedAt());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }
}
