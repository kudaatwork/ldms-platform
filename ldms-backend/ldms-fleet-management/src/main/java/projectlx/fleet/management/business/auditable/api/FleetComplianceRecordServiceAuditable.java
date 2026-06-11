package projectlx.fleet.management.business.auditable.api;

import projectlx.fleet.management.model.FleetComplianceRecord;

import java.util.List;
import java.util.Locale;

public interface FleetComplianceRecordServiceAuditable {
    FleetComplianceRecord create(FleetComplianceRecord record, Locale locale, String username);
    FleetComplianceRecord update(FleetComplianceRecord record, Locale locale, String username);
    FleetComplianceRecord delete(FleetComplianceRecord record, Locale locale);
    List<FleetComplianceRecord> createAll(List<FleetComplianceRecord> records, Locale locale, String username);
}
