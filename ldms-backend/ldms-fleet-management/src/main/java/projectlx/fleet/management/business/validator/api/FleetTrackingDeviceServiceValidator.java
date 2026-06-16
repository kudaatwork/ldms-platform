package projectlx.fleet.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.fleet.management.utils.requests.EditFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.requests.InstallFleetTrackingDeviceRequest;

import java.util.Locale;

public interface FleetTrackingDeviceServiceValidator {
    ValidatorDto isInstallRequestValid(InstallFleetTrackingDeviceRequest request, Locale locale);
    ValidatorDto isEditRequestValid(EditFleetTrackingDeviceRequest request, Locale locale);
}
