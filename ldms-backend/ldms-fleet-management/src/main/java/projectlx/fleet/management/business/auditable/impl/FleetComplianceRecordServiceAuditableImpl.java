package projectlx.fleet.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fleet.management.business.auditable.api.FleetComplianceRecordServiceAuditable;
import projectlx.fleet.management.model.FleetComplianceRecord;
import projectlx.fleet.management.repository.FleetComplianceRecordRepository;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FleetComplianceRecordServiceAuditableImpl implements FleetComplianceRecordServiceAuditable {

    private final FleetComplianceRecordRepository fleetComplianceRecordRepository;

    @Override
    public FleetComplianceRecord create(FleetComplianceRecord record, Locale locale, String username) {
        return fleetComplianceRecordRepository.save(record);
    }

    @Override
    public FleetComplianceRecord update(FleetComplianceRecord record, Locale locale, String username) {
        return fleetComplianceRecordRepository.save(record);
    }

    @Override
    public FleetComplianceRecord delete(FleetComplianceRecord record, Locale locale) {
        return fleetComplianceRecordRepository.save(record);
    }

    @Override
    public List<FleetComplianceRecord> createAll(List<FleetComplianceRecord> records, Locale locale, String username) {
        return fleetComplianceRecordRepository.saveAll(records);
    }
}
