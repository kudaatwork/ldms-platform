package projectlx.fuel.expenses.business.logic.api;

import projectlx.fuel.expenses.utils.responses.FuelSessionResponse;

import java.util.Locale;
import java.util.Map;

public interface FuelSessionService {

    /**
     * Creates a new FuelSession when a trip starts.
     * Triggered by the trip.started RabbitMQ event.
     *
     * @param payload deserialized trip.started event payload
     */
    void onTripStarted(Map<String, Object> payload);

    /**
     * Updates the active FuelSession for the trip when a location update arrives.
     * Calculates haversine distance from the last known position, deducts fuel
     * at 35 L/100 km, and publishes a fuel.level_updated event to fuel.exchange.
     * Triggered by the trip.location_updated RabbitMQ event.
     *
     * @param payload deserialized trip.location_updated event payload
     */
    void onLocationUpdated(Map<String, Object> payload);

    /**
     * Fetches the live fuel session snapshot for a given trip.
     *
     * @param tripId  the trip identifier
     * @param locale  locale for i18n error messages
     * @param username caller identity
     * @return response containing the live FuelSessionDto
     */
    FuelSessionResponse getLiveByTripId(Long tripId, Locale locale, String username);
}
