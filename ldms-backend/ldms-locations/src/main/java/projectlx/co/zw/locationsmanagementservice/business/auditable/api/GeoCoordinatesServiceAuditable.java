package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import java.util.Locale;

public interface GeoCoordinatesServiceAuditable {
    GeoCoordinates create(GeoCoordinates geoCoordinates, Locale locale, String username);
    GeoCoordinates update(GeoCoordinates geoCoordinates, Locale locale, String username);
    GeoCoordinates delete(GeoCoordinates geoCoordinates, Locale locale);
}