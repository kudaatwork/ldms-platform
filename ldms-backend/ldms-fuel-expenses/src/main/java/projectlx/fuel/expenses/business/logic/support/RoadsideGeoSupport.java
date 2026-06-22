package projectlx.fuel.expenses.business.logic.support;

import java.math.BigDecimal;

public final class RoadsideGeoSupport {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private RoadsideGeoSupport() {
    }

    public static BigDecimal[] boundingBox(double latitude, double longitude, double radiusKm) {
        double latDelta = radiusKm / EARTH_RADIUS_KM * (180.0 / Math.PI);
        double lngDelta = radiusKm / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(latitude))) * (180.0 / Math.PI);
        return new BigDecimal[] {
                BigDecimal.valueOf(latitude - latDelta),
                BigDecimal.valueOf(latitude + latDelta),
                BigDecimal.valueOf(longitude - lngDelta),
                BigDecimal.valueOf(longitude + lngDelta),
        };
    }

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 10.0) / 10.0;
    }
}
