package projectlx.fleet.management.business.validator.api;

import projectlx.fleet.management.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.fleet.management.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface FleetComplianceServiceValidator {
    ValidatorDto isCreateFleetComplianceRequestValid(CreateFleetComplianceRecordRequest request, Locale locale);
    ValidatorDto isEditFleetComplianceRequestValid(EditFleetComplianceRecordRequest request, Locale locale);
}
