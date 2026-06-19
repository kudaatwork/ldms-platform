package projectlx.trip.tracking.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.trip.tracking.business.auditable.api.TripDeliveryWorkflowServiceAuditable;
import projectlx.trip.tracking.model.TripDeliveryReturnLine;
import projectlx.trip.tracking.model.TripDeliveryWorkflow;
import projectlx.trip.tracking.repository.TripDeliveryReturnLineRepository;
import projectlx.trip.tracking.repository.TripDeliveryWorkflowRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class TripDeliveryWorkflowServiceAuditableImpl implements TripDeliveryWorkflowServiceAuditable {

    private final TripDeliveryWorkflowRepository workflowRepository;
    private final TripDeliveryReturnLineRepository returnLineRepository;

    @Override
    public TripDeliveryWorkflow create(TripDeliveryWorkflow workflow, Locale locale, String username) {
        return workflowRepository.save(workflow);
    }

    @Override
    public TripDeliveryWorkflow update(TripDeliveryWorkflow workflow, Locale locale, String username) {
        return workflowRepository.save(workflow);
    }

    @Override
    public TripDeliveryReturnLine createReturnLine(TripDeliveryReturnLine line, Locale locale, String username) {
        return returnLineRepository.save(line);
    }
}
