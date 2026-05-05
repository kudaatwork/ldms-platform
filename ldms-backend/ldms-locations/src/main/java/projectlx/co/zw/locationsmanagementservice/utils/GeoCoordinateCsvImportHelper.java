package projectlx.co.zw.locationsmanagementservice.utils;

import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;

import java.math.BigDecimal;

/**
 * Resolves how CSV import should populate geo fields before {@code create} runs.
 * <p>Priority: explicit {@code GEO COORDINATES ID} &gt; {@code LATITUDE}+{@code LONGITUDE} (persisted as a new
 * row inside the service) &gt; copy coordinates from a parent entity’s geo point (new row, new id).</p>
 */
public final class GeoCoordinateCsvImportHelper {

    private GeoCoordinateCsvImportHelper() {
    }

    public record ResolvedGeo(Long geoCoordinatesId, BigDecimal latitude, BigDecimal longitude) {
    }

    /**
     * @param geoCoordinatesId optional existing geo row to link
     * @param latitude         optional when paired with longitude
     * @param longitude        optional when paired with latitude
     * @param parentGeo        optional fallback (e.g. country → province → district → suburb)
     */
    public static ResolvedGeo resolve(
            Long geoCoordinatesId,
            BigDecimal latitude,
            BigDecimal longitude,
            GeoCoordinates parentGeo) {
        if (geoCoordinatesId != null && geoCoordinatesId > 0) {
            return new ResolvedGeo(geoCoordinatesId, null, null);
        }
        if (latitude != null && longitude != null) {
            return new ResolvedGeo(null, latitude, longitude);
        }
        if (parentGeo != null && parentGeo.getLatitude() != null && parentGeo.getLongitude() != null) {
            return new ResolvedGeo(null, parentGeo.getLatitude(), parentGeo.getLongitude());
        }
        return new ResolvedGeo(null, null, null);
    }
}
