package projectlx.trip.tracking.business.logic.support;

import lombok.RequiredArgsConstructor;
import projectlx.trip.tracking.business.auditable.api.TripDeliveryWorkflowServiceAuditable;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripDeliveryWorkflow;
import projectlx.trip.tracking.repository.TripDeliveryWorkflowRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.trip.tracking.utils.enums.TripStatus;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Ensures a {@link TripDeliveryWorkflow} row exists when the driver enters the delivery phase.
 */
@RequiredArgsConstructor
public class TripDeliveryWorkflowBootstrapSupport {

    private final TripDeliveryWorkflowRepository workflowRepository;
    private final TripDeliveryWorkflowServiceAuditable workflowAuditable;

    public TripDeliveryWorkflow ensureWorkflow(Trip trip, LocalDateTime now, String username, Locale locale) {
        return workflowRepository
                .findByTripIdAndEntityStatusNot(trip.getId(), EntityStatus.DELETED)
                .orElseGet(() -> {
                    TripDeliveryWorkflow workflow = new TripDeliveryWorkflow();
                    workflow.setTrip(trip);
                    workflow.setExpectedQuantity(
                            trip.getQuantity() != null ? trip.getQuantity().stripTrailingZeros() : null);
                    workflow.setEntityStatus(EntityStatus.ACTIVE);
                    workflow.setCreatedAt(now);
                    workflow.setCreatedBy(username);
                    return workflowAuditable.create(workflow, locale, username);
                });
    }

    public boolean isDeliveryPhase(TripStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case ARRIVED, COUNTING_STOCK, COUNT_COMPLETE, OTP_PENDING, DELIVERED, RETURN_IN_TRANSIT, RETURNED ->
                    true;
            default -> false;
        };
    }
}
