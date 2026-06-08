package projectlx.co.zw.fleetmanagement.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.fleetmanagement.business.logic.api.FleetComplianceService;
import projectlx.co.zw.fleetmanagement.service.processor.api.FleetComplianceServiceProcessor;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetComplianceRecordResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class FleetComplianceServiceProcessorImpl implements FleetComplianceServiceProcessor {

    private final FleetComplianceService fleetComplianceService;

    @Override
    public FleetComplianceRecordResponse list(Locale locale, String username) {
        log.info("Processing list fleet compliance records for user {}", username);
        return fleetComplianceService.list(locale, username);
    }

    @Override
    public FleetComplianceRecordResponse create(CreateFleetComplianceRecordRequest request, Locale locale, String username) {
        log.info("Processing create fleet compliance record for user {}", username);
        return fleetComplianceService.create(request, locale, username);
    }

    @Override
    public FleetComplianceRecordResponse update(Long id, EditFleetComplianceRecordRequest request, Locale locale, String username) {
        log.info("Processing update fleet compliance record {} for user {}", id, username);
        return fleetComplianceService.update(id, request, locale, username);
    }

    @Override
    public FleetComplianceRecordResponse listExpiring(int withinDays, Locale locale, String username) {
        log.info("Processing expiring fleet compliance records for user {} within {} days", username, withinDays);
        return fleetComplianceService.listExpiring(withinDays, locale, username);
    }
}
