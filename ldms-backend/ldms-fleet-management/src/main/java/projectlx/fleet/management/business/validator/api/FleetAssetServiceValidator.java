package projectlx.fleet.management.business.validator.api;

import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.utils.requests.CompleteFleetAssetRegistrationRequest;
import projectlx.fleet.management.utils.requests.CreateFleetAssetRequest;
import projectlx.fleet.management.utils.requests.EditFleetAssetRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface FleetAssetServiceValidator {
    ValidatorDto isCreateFleetAssetRequestValid(CreateFleetAssetRequest request, Locale locale);
    ValidatorDto isEditFleetAssetRequestValid(EditFleetAssetRequest request, Locale locale);
    ValidatorDto isCompleteRegistrationRequestValid(CompleteFleetAssetRegistrationRequest request,
                                                    FleetAsset asset,
                                                    Locale locale);
}
