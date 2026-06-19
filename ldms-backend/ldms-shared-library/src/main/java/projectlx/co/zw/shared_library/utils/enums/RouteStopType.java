package projectlx.co.zw.shared_library.utils.enums;

/**
 * Classifies the role of a stop on a shipment or trip route.
 * <ul>
 *   <li>{@link #ORIGIN} – the departure point where goods are picked up.</li>
 *   <li>{@link #EN_ROUTE_DEPOT} – an intermediate depot, border post, or waypoint along the route.</li>
 *   <li>{@link #DESTINATION} – the final delivery point.</li>
 * </ul>
 */
public enum RouteStopType {
    ORIGIN,
    EN_ROUTE_DEPOT,
    DESTINATION
}
