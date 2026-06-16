package projectlx.fleet.management.business.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fleet.management.business.auditable.api.FleetAssetServiceAuditable;
import projectlx.fleet.management.business.auditable.api.FleetComplianceRecordServiceAuditable;
import projectlx.fleet.management.business.auditable.api.FleetDriverServiceAuditable;
import projectlx.fleet.management.business.auditable.impl.FleetAssetServiceAuditableImpl;
import projectlx.fleet.management.business.auditable.impl.FleetComplianceRecordServiceAuditableImpl;
import projectlx.fleet.management.business.auditable.impl.FleetDriverServiceAuditableImpl;
import projectlx.fleet.management.business.logic.api.FleetAssetService;
import projectlx.fleet.management.business.logic.api.FleetComplianceService;
import projectlx.fleet.management.business.logic.api.FleetDriverService;
import projectlx.fleet.management.business.logic.impl.FleetAssetServiceImpl;
import projectlx.fleet.management.business.logic.impl.FleetComplianceServiceImpl;
import projectlx.fleet.management.business.logic.impl.FleetDriverServiceImpl;
import projectlx.fleet.management.business.logic.support.CallerOrganizationResolver;
import projectlx.fleet.management.business.logic.support.FleetAssetRegistrationNotificationSupport;
import projectlx.fleet.management.business.logic.support.FleetFileUploadHelper;
import projectlx.fleet.management.business.logic.support.FleetOwnershipValidationSupport;
import projectlx.fleet.management.business.validator.api.FleetAssetServiceValidator;
import projectlx.fleet.management.business.validator.api.FleetComplianceServiceValidator;
import projectlx.fleet.management.business.validator.api.FleetDriverServiceValidator;
import projectlx.fleet.management.business.validator.impl.FleetAssetServiceValidatorImpl;
import projectlx.fleet.management.business.validator.impl.FleetComplianceServiceValidatorImpl;
import projectlx.fleet.management.business.validator.impl.FleetDriverServiceValidatorImpl;
import projectlx.fleet.management.business.auditable.api.FleetTrackingDeviceServiceAuditable;
import projectlx.fleet.management.business.auditable.impl.FleetTrackingDeviceServiceAuditableImpl;
import projectlx.fleet.management.business.logic.api.FleetTrackingDeviceService;
import projectlx.fleet.management.business.logic.impl.FleetTrackingDeviceServiceImpl;
import projectlx.fleet.management.business.validator.api.FleetTrackingDeviceServiceValidator;
import projectlx.fleet.management.business.validator.impl.FleetTrackingDeviceServiceValidatorImpl;
import projectlx.fleet.management.repository.FleetAssetRepository;
import projectlx.fleet.management.repository.FleetComplianceRecordRepository;
import projectlx.fleet.management.repository.FleetDriverRepository;
import projectlx.fleet.management.repository.FleetTrackingDeviceRepository;

@Configuration
@Import({UtilsConfig.class})
public class BusinessConfig {

    @Value("${ldms.fleet.compliance-expiring-soon-days:30}")
    private int fleetComplianceExpiringSoonDays;

    // ============================================================
    // Auditable beans
    // ============================================================

    @Bean
    public FleetAssetServiceAuditable fleetAssetServiceAuditable(FleetAssetRepository fleetAssetRepository) {
        return new FleetAssetServiceAuditableImpl(fleetAssetRepository);
    }

    @Bean
    public FleetDriverServiceAuditable fleetDriverServiceAuditable(FleetDriverRepository fleetDriverRepository) {
        return new FleetDriverServiceAuditableImpl(fleetDriverRepository);
    }

    @Bean
    public FleetComplianceRecordServiceAuditable fleetComplianceRecordServiceAuditable(
            FleetComplianceRecordRepository fleetComplianceRecordRepository) {
        return new FleetComplianceRecordServiceAuditableImpl(fleetComplianceRecordRepository);
    }

    // ============================================================
    // Validator beans
    // ============================================================

    @Bean
    public FleetAssetServiceValidator fleetAssetServiceValidator(MessageService messageService) {
        return new FleetAssetServiceValidatorImpl(messageService);
    }

    @Bean
    public FleetDriverServiceValidator fleetDriverServiceValidator(MessageService messageService) {
        return new FleetDriverServiceValidatorImpl(messageService);
    }

    @Bean
    public FleetComplianceServiceValidator fleetComplianceServiceValidator(MessageService messageService) {
        return new FleetComplianceServiceValidatorImpl(messageService);
    }

    // ============================================================
    // Service beans
    // ============================================================

    @Bean
    public FleetAssetService fleetAssetService(
            FleetAssetServiceValidator fleetAssetServiceValidator,
            FleetAssetRepository fleetAssetRepository,
            FleetComplianceRecordServiceAuditable fleetComplianceRecordServiceAuditable,
            CallerOrganizationResolver callerOrganizationResolver,
            FleetAssetRegistrationNotificationSupport fleetAssetRegistrationNotificationSupport,
            FleetOwnershipValidationSupport fleetOwnershipValidationSupport,
            FleetFileUploadHelper fleetFileUploadHelper,
            MessageService messageService,
            FleetAssetServiceAuditable fleetAssetServiceAuditable,
            FleetDriverRepository fleetDriverRepository,
            @Value("${ldms.fleet.compliance-expiring-soon-days:30}") int defaultExpiringSoonDays) {
        return new FleetAssetServiceImpl(
                fleetAssetServiceValidator,
                fleetAssetRepository,
                fleetComplianceRecordServiceAuditable,
                callerOrganizationResolver,
                fleetAssetRegistrationNotificationSupport,
                fleetOwnershipValidationSupport,
                fleetFileUploadHelper,
                messageService,
                fleetAssetServiceAuditable,
                fleetDriverRepository,
                defaultExpiringSoonDays);
    }

    @Bean
    public FleetDriverService fleetDriverService(
            FleetDriverServiceValidator fleetDriverServiceValidator,
            FleetDriverRepository fleetDriverRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            MessageService messageService,
            FleetDriverServiceAuditable fleetDriverServiceAuditable,
            FleetFileUploadHelper fleetFileUploadHelper,
            FleetOwnershipValidationSupport fleetOwnershipValidationSupport) {
        return new FleetDriverServiceImpl(
                fleetDriverServiceValidator,
                fleetDriverRepository,
                callerOrganizationResolver,
                messageService,
                fleetDriverServiceAuditable,
                fleetFileUploadHelper,
                fleetOwnershipValidationSupport);
    }

    // ============================================================
    // Tracking device beans
    // ============================================================

    @Bean
    public FleetTrackingDeviceServiceAuditable fleetTrackingDeviceServiceAuditable(
            FleetTrackingDeviceRepository fleetTrackingDeviceRepository) {
        return new FleetTrackingDeviceServiceAuditableImpl(fleetTrackingDeviceRepository);
    }

    @Bean
    public FleetTrackingDeviceServiceValidator fleetTrackingDeviceServiceValidator(MessageService messageService) {
        return new FleetTrackingDeviceServiceValidatorImpl(messageService);
    }

    @Bean
    public FleetTrackingDeviceService fleetTrackingDeviceService(
            FleetTrackingDeviceServiceValidator fleetTrackingDeviceServiceValidator,
            FleetTrackingDeviceServiceAuditable fleetTrackingDeviceServiceAuditable,
            FleetTrackingDeviceRepository fleetTrackingDeviceRepository,
            FleetAssetRepository fleetAssetRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            MessageService messageService) {
        return new FleetTrackingDeviceServiceImpl(
                fleetTrackingDeviceServiceValidator,
                fleetTrackingDeviceServiceAuditable,
                fleetTrackingDeviceRepository,
                fleetAssetRepository,
                callerOrganizationResolver,
                messageService);
    }

    @Bean
    public FleetComplianceService fleetComplianceService(
            FleetComplianceServiceValidator fleetComplianceServiceValidator,
            FleetComplianceRecordRepository fleetComplianceRecordRepository,
            FleetAssetRepository fleetAssetRepository,
            FleetDriverRepository fleetDriverRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            FleetFileUploadHelper fleetFileUploadHelper,
            MessageService messageService,
            FleetComplianceRecordServiceAuditable fleetComplianceRecordServiceAuditable) {
        return new FleetComplianceServiceImpl(
                fleetComplianceServiceValidator,
                fleetComplianceRecordRepository,
                fleetAssetRepository,
                fleetDriverRepository,
                callerOrganizationResolver,
                fleetFileUploadHelper,
                messageService,
                fleetComplianceRecordServiceAuditable,
                fleetComplianceExpiringSoonDays);
    }
}
