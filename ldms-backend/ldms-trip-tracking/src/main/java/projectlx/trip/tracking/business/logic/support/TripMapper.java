package projectlx.trip.tracking.business.logic.support;

import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripDeliveryReturnLine;
import projectlx.trip.tracking.model.TripDeliveryWorkflow;
import projectlx.trip.tracking.model.TripEvent;
import projectlx.trip.tracking.utils.dtos.TripDeliveryReturnLineDto;
import projectlx.trip.tracking.utils.dtos.TripDeliveryWorkflowDto;
import projectlx.trip.tracking.utils.dtos.TripDto;
import projectlx.trip.tracking.utils.dtos.TripEventDto;

import java.util.List;

public final class TripMapper {

    private TripMapper() {
    }

    public static TripDto toDto(Trip trip) {
        if (trip == null) {
            return null;
        }
        TripDto dto = new TripDto();
        dto.setId(trip.getId());
        dto.setTripNumber(trip.getTripNumber());
        dto.setOrganizationId(trip.getOrganizationId());
        dto.setShipmentId(trip.getShipmentId());
        dto.setShipmentNumber(trip.getShipmentNumber());
        dto.setInventoryTransferId(trip.getInventoryTransferId());
        dto.setFleetDriverId(trip.getFleetDriverId());
        dto.setFleetAssetId(trip.getFleetAssetId());
        dto.setStatus(trip.getStatus());
        dto.setStartedAt(trip.getStartedAt());
        dto.setArrivedAt(trip.getArrivedAt());
        dto.setCompletedAt(trip.getCompletedAt());
        dto.setReceiverUserId(trip.getReceiverUserId());
        dto.setFromWarehouseName(trip.getFromWarehouseName());
        dto.setToWarehouseName(trip.getToWarehouseName());
        dto.setProductName(trip.getProductName());
        dto.setProductCode(trip.getProductCode());
        dto.setQuantity(trip.getQuantity());
        dto.setCreatedAt(trip.getCreatedAt());
        dto.setCreatedBy(trip.getCreatedBy());
        return dto;
    }

    public static TripDto toDtoWithEvents(Trip trip, List<TripEvent> events) {
        TripDto dto = toDto(trip);
        if (dto != null && events != null) {
            Long tripId = trip.getId();
            dto.setRecentEvents(events.stream().map(event -> toEventDto(event, tripId)).toList());
        }
        return dto;
    }

    public static TripEventDto toEventDto(TripEvent event) {
        return toEventDto(event, null);
    }

    public static TripEventDto toEventDto(TripEvent event, Long tripId) {
        if (event == null) {
            return null;
        }
        TripEventDto dto = new TripEventDto();
        dto.setId(event.getId());
        dto.setTripId(tripId != null ? tripId : (event.getTrip() != null ? event.getTrip().getId() : null));
        dto.setEventType(event.getEventType());
        dto.setEventTime(event.getEventTime());
        dto.setLatitude(event.getLatitude());
        dto.setLongitude(event.getLongitude());
        dto.setNotes(event.getNotes());
        dto.setRecordedByUserId(event.getRecordedByUserId());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }

    public static TripDeliveryWorkflowDto toWorkflowDto(TripDeliveryWorkflow workflow) {
        return toWorkflowDto(workflow, null);
    }

    public static TripDeliveryWorkflowDto toWorkflowDto(TripDeliveryWorkflow workflow, Long tripId) {
        if (workflow == null) {
            return null;
        }
        TripDeliveryWorkflowDto dto = new TripDeliveryWorkflowDto();
        dto.setId(workflow.getId());
        Long resolvedTripId = tripId;
        if (resolvedTripId == null && workflow.getTrip() != null) {
            resolvedTripId = workflow.getTrip().getId();
            dto.setTripNumber(workflow.getTrip().getTripNumber());
        }
        dto.setTripId(resolvedTripId);
        dto.setDriverCountingStartedAt(workflow.getDriverCountingStartedAt());
        dto.setDriverCountingFinishedAt(workflow.getDriverCountingFinishedAt());
        dto.setCustomerCountingStartedAt(workflow.getCustomerCountingStartedAt());
        dto.setCustomerCountingFinishedAt(workflow.getCustomerCountingFinishedAt());
        dto.setExpectedQuantity(workflow.getExpectedQuantity());
        dto.setCountedQuantity(workflow.getCountedQuantity());
        dto.setOtpChannel(workflow.getOtpChannel());
        dto.setOtpRecipient(workflow.getOtpRecipient());
        dto.setDeliveryNotes(workflow.getDeliveryNotes());
        dto.setReturnInitiatedAt(workflow.getReturnInitiatedAt());
        dto.setReturnCompletedAt(workflow.getReturnCompletedAt());
        dto.setEntityStatus(workflow.getEntityStatus() != null ? workflow.getEntityStatus().name() : null);
        dto.setCreatedAt(workflow.getCreatedAt());
        dto.setCreatedBy(workflow.getCreatedBy());
        dto.setModifiedAt(workflow.getModifiedAt());
        dto.setModifiedBy(workflow.getModifiedBy());
        if (workflow.getReturnLines() != null) {
            dto.setReturnLines(workflow.getReturnLines().stream()
                    .map(TripMapper::toReturnLineDto)
                    .toList());
        }
        return dto;
    }

    public static TripDeliveryReturnLineDto toReturnLineDto(TripDeliveryReturnLine line) {
        if (line == null) {
            return null;
        }
        TripDeliveryReturnLineDto dto = new TripDeliveryReturnLineDto();
        dto.setId(line.getId());
        dto.setWorkflowId(line.getWorkflow() != null ? line.getWorkflow().getId() : null);
        dto.setProductName(line.getProductName());
        dto.setQuantity(line.getQuantity());
        dto.setReason(line.getReason());
        dto.setRecordedByRole(line.getRecordedByRole());
        dto.setEntityStatus(line.getEntityStatus() != null ? line.getEntityStatus().name() : null);
        dto.setCreatedAt(line.getCreatedAt());
        dto.setCreatedBy(line.getCreatedBy());
        return dto;
    }
}
