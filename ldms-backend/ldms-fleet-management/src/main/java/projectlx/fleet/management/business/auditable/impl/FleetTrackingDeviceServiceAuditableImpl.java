package projectlx.fleet.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fleet.management.business.auditable.api.FleetTrackingDeviceServiceAuditable;
import projectlx.fleet.management.model.FleetTrackingDevice;
import projectlx.fleet.management.repository.FleetTrackingDeviceRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class FleetTrackingDeviceServiceAuditableImpl implements FleetTrackingDeviceServiceAuditable {

    private final FleetTrackingDeviceRepository fleetTrackingDeviceRepository;

    @Override
    public FleetTrackingDevice create(FleetTrackingDevice device, Locale locale, String username) {
        return fleetTrackingDeviceRepository.save(device);
    }

    @Override
    public FleetTrackingDevice update(FleetTrackingDevice device, Locale locale, String username) {
        return fleetTrackingDeviceRepository.save(device);
    }

    @Override
    public FleetTrackingDevice delete(FleetTrackingDevice device, Locale locale) {
        return fleetTrackingDeviceRepository.save(device);
    }
}
