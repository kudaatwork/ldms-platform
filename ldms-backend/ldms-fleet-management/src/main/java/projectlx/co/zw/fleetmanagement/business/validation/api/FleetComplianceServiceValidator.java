package projectlx.co.zw.fleetmanagement.business.validation.api;

import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface FleetComplianceServiceValidator {
    ValidatorDto isCreateFleetComplianceRequestValid(CreateFleetComplianceRecordRequest request, Locale locale);
    ValidatorDto isEditFleetComplianceRequestValid(EditFleetComplianceRecordRequest request, Locale locale);
}
