package projectlx.co.zw.fleetmanagement.business.validation.api;

import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetAssetRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetAssetRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface FleetAssetServiceValidator {
    ValidatorDto isCreateFleetAssetRequestValid(CreateFleetAssetRequest request, Locale locale);
    ValidatorDto isEditFleetAssetRequestValid(EditFleetAssetRequest request, Locale locale);
}
