package projectlx.fuel.expenses.events.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.fuel.expenses.business.logic.api.FuelSessionService;
import projectlx.fuel.expenses.utils.config.RabbitMQConsumerConfig;

import java.util.Map;

/**
 * Listens for trip.location_updated events from trip.exchange.
 *
 * On receipt, delegates to FuelSessionService.onLocationUpdated to:
 *  - Calculate haversine distance from the last GPS fix.
 *  - Deduct fuel at 35 L/100 km.
 *  - Publish fuel.level_updated to fuel.exchange.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TripLocationUpdatedEventHandler {

    private final FuelSessionService fuelSessionService;

    @RabbitListener(queues = RabbitMQConsumerConfig.FUEL_TRIP_LOCATION_UPDATED_QUEUE)
    public void handleTripLocationUpdated(Map<String, Object> payload) {
        try {
            log.debug("Received trip.location_updated event: tripId={} lat={} lng={}",
                    payload.get("tripId"), payload.get("latitude"), payload.get("longitude"));
            fuelSessionService.onLocationUpdated(payload);
        } catch (Exception ex) {
            log.error("Error processing trip.location_updated event for tripId={}: {}",
                    payload.get("tripId"), ex.getMessage(), ex);
            throw ex;
        }
    }
}
