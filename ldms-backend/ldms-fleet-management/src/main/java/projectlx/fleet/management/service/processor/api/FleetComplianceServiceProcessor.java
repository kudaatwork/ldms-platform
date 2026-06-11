package projectlx.fleet.management.service.processor.api;

import projectlx.fleet.management.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.fleet.management.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.fleet.management.utils.responses.FleetComplianceRecordResponse;

import java.util.Locale;

public interface FleetComplianceServiceProcessor {
    FleetComplianceRecordResponse list(Locale locale, String username);
    FleetComplianceRecordResponse create(CreateFleetComplianceRecordRequest request, Locale locale, String username);
    FleetComplianceRecordResponse update(Long id, EditFleetComplianceRecordRequest request, Locale locale, String username);
    FleetComplianceRecordResponse listExpiring(int withinDays, Locale locale, String username);
}
