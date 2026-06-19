package projectlx.trip.tracking.business.auditable.api;

import projectlx.trip.tracking.model.TripDeliveryReturnLine;
import projectlx.trip.tracking.model.TripDeliveryWorkflow;

import java.util.Locale;

public interface TripDeliveryWorkflowServiceAuditable {

    TripDeliveryWorkflow create(TripDeliveryWorkflow workflow, Locale locale, String username);

    TripDeliveryWorkflow update(TripDeliveryWorkflow workflow, Locale locale, String username);

    TripDeliveryReturnLine createReturnLine(TripDeliveryReturnLine line, Locale locale, String username);
}
