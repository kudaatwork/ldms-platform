package projectlx.fleet.management.business.auditable.api;

import projectlx.fleet.management.model.FleetTrackingDevice;

import java.util.Locale;

public interface FleetTrackingDeviceServiceAuditable {
    FleetTrackingDevice create(FleetTrackingDevice device, Locale locale, String username);
    FleetTrackingDevice update(FleetTrackingDevice device, Locale locale, String username);
    FleetTrackingDevice delete(FleetTrackingDevice device, Locale locale);
}
