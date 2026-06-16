package projectlx.fuel.expenses.events.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.fuel.expenses.business.logic.api.FuelSessionService;
import projectlx.fuel.expenses.utils.config.RabbitMQConsumerConfig;

import java.util.Map;

/**
 * Listens for trip.started events from trip.exchange.
 *
 * On receipt, delegates to FuelSessionService.onTripStarted to open
 * a new FuelSession for the trip with a 400 L tank at 100 % capacity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TripStartedEventHandler {

    private final FuelSessionService fuelSessionService;

    @RabbitListener(queues = RabbitMQConsumerConfig.FUEL_TRIP_STARTED_QUEUE)
    public void handleTripStarted(Map<String, Object> payload) {
        try {
            log.info("Received trip.started event: tripId={} orgId={}",
                    payload.get("tripId"), payload.get("organizationId"));
            fuelSessionService.onTripStarted(payload);
        } catch (Exception ex) {
            log.error("Error processing trip.started event for tripId={}: {}",
                    payload.get("tripId"), ex.getMessage(), ex);
            throw ex;
        }
    }
}
