package projectlx.co.zw.fleetmanagement.business.logic.api;

import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetComplianceRecordResponse;

import java.util.Locale;

public interface FleetComplianceService {
    FleetComplianceRecordResponse list(Locale locale, String username);
    FleetComplianceRecordResponse create(CreateFleetComplianceRecordRequest request, Locale locale, String username);
    FleetComplianceRecordResponse update(Long id, EditFleetComplianceRecordRequest request, Locale locale, String username);
    FleetComplianceRecordResponse listExpiring(int withinDays, Locale locale, String username);
}
