package projectlx.shipment.management.business.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.notifications.LogisticsLifecycleNotificationSupport;
import projectlx.shipment.management.business.auditable.api.BorderClearanceCaseServiceAuditable;
import projectlx.shipment.management.business.auditable.api.ShipmentServiceAuditable;
import projectlx.shipment.management.business.auditable.impl.BorderClearanceCaseServiceAuditableImpl;
import projectlx.shipment.management.business.auditable.impl.ShipmentServiceAuditableImpl;
import projectlx.shipment.management.business.logic.api.BorderClearanceCaseService;
import projectlx.shipment.management.business.logic.api.PlatformDashboardService;
import projectlx.shipment.management.business.logic.api.ShipmentService;
import projectlx.shipment.management.business.logic.impl.BorderClearanceCaseServiceImpl;
import projectlx.shipment.management.business.logic.impl.PlatformDashboardServiceImpl;
import projectlx.shipment.management.business.logic.impl.ShipmentServiceImpl;
import projectlx.co.zw.shared_library.billing.PlatformWalletUsageSupport;
import projectlx.shipment.management.clients.BillingPaymentsServiceClient;
import projectlx.shipment.management.business.logic.support.CallerOrganizationResolver;
import projectlx.shipment.management.business.logic.support.LogisticsNotificationRecipientResolver;
import projectlx.shipment.management.business.logic.support.PlatformDashboardSupport;
import projectlx.shipment.management.business.logic.support.ShipmentFleetAllocatorSupport;
import projectlx.shipment.management.business.validator.api.BorderClearanceCaseServiceValidator;
import projectlx.shipment.management.business.validator.api.ShipmentServiceValidator;
import projectlx.shipment.management.business.validator.impl.BorderClearanceCaseServiceValidatorImpl;
import projectlx.shipment.management.business.validator.impl.ShipmentServiceValidatorImpl;
import projectlx.shipment.management.clients.FleetManagementServiceClient;
import projectlx.shipment.management.clients.OrganizationManagementServiceClient;
import projectlx.shipment.management.clients.TripTrackingServiceClient;
import projectlx.shipment.management.clients.UserManagementServiceClient;
import projectlx.shipment.management.repository.BorderClearanceCaseRepository;
import projectlx.shipment.management.repository.BorderClearanceDocumentRepository;
import projectlx.shipment.management.repository.ShipmentRepository;

@Configuration
public class BusinessConfig {

    @Bean
    public ShipmentServiceValidator shipmentServiceValidator(MessageService messageService) {
        return new ShipmentServiceValidatorImpl(messageService);
    }

    @Bean
    public ShipmentServiceAuditable shipmentServiceAuditable(ShipmentRepository shipmentRepository) {
        return new ShipmentServiceAuditableImpl(shipmentRepository);
    }

    @Bean
    public BorderClearanceCaseServiceValidator borderClearanceCaseServiceValidator(MessageService messageService) {
        return new BorderClearanceCaseServiceValidatorImpl(messageService);
    }

    @Bean
    public BorderClearanceCaseServiceAuditable borderClearanceCaseServiceAuditable(
            BorderClearanceCaseRepository borderClearanceCaseRepository,
            BorderClearanceDocumentRepository borderClearanceDocumentRepository) {
        return new BorderClearanceCaseServiceAuditableImpl(
                borderClearanceCaseRepository, borderClearanceDocumentRepository);
    }

    @Bean
    public LogisticsNotificationRecipientResolver logisticsNotificationRecipientResolver(
            OrganizationManagementServiceClient organizationManagementServiceClient,
            UserManagementServiceClient userManagementServiceClient,
            FleetManagementServiceClient fleetManagementServiceClient) {
        return new LogisticsNotificationRecipientResolver(
                organizationManagementServiceClient,
                userManagementServiceClient,
                fleetManagementServiceClient);
    }

    @Bean
    public BorderClearanceCaseService borderClearanceCaseService(
            BorderClearanceCaseServiceValidator borderClearanceCaseServiceValidator,
            BorderClearanceCaseServiceAuditable borderClearanceCaseServiceAuditable,
            BorderClearanceCaseRepository borderClearanceCaseRepository,
            BorderClearanceDocumentRepository borderClearanceDocumentRepository,
            TripTrackingServiceClient tripTrackingServiceClient,
            MessageService messageService) {
        return new BorderClearanceCaseServiceImpl(
                borderClearanceCaseServiceValidator,
                borderClearanceCaseServiceAuditable,
                borderClearanceCaseRepository,
                borderClearanceDocumentRepository,
                tripTrackingServiceClient,
                messageService);
    }

    @Bean
    public ShipmentFleetAllocatorSupport shipmentFleetAllocatorSupport(
            UserManagementServiceClient userManagementServiceClient) {
        return new ShipmentFleetAllocatorSupport(userManagementServiceClient);
    }

    @Bean
    public PlatformDashboardSupport platformDashboardSupport(ShipmentRepository shipmentRepository) {
        return new PlatformDashboardSupport(shipmentRepository);
    }

    @Bean
    public PlatformDashboardService platformDashboardService(PlatformDashboardSupport platformDashboardSupport,
                                                             MessageService messageService) {
        return new PlatformDashboardServiceImpl(platformDashboardSupport, messageService);
    }

    @Bean
    public PlatformWalletUsageSupport platformWalletUsageSupport(BillingPaymentsServiceClient billingPaymentsServiceClient) {
        return new PlatformWalletUsageSupport(billingPaymentsServiceClient::recordUsageCharge, "ldms-shipment-management");
    }

    @Bean
    public ShipmentService shipmentService(ShipmentServiceValidator shipmentServiceValidator,
                                           ShipmentServiceAuditable shipmentServiceAuditable,
                                           ShipmentRepository shipmentRepository,
                                           CallerOrganizationResolver callerOrganizationResolver,
                                           ShipmentFleetAllocatorSupport shipmentFleetAllocatorSupport,
                                           RabbitTemplate rabbitTemplate,
                                           MessageService messageService,
                                           LogisticsLifecycleNotificationSupport logisticsLifecycleNotificationSupport,
                                           LogisticsNotificationRecipientResolver logisticsNotificationRecipientResolver,
                                           OrganizationManagementServiceClient organizationManagementServiceClient,
                                           FleetManagementServiceClient fleetManagementServiceClient,
                                           BorderClearanceCaseService borderClearanceCaseService,
                                           PlatformWalletUsageSupport platformWalletUsageSupport) {
        return new ShipmentServiceImpl(shipmentServiceValidator, shipmentServiceAuditable,
                shipmentRepository, callerOrganizationResolver, shipmentFleetAllocatorSupport, rabbitTemplate, messageService,
                logisticsLifecycleNotificationSupport, logisticsNotificationRecipientResolver,
                organizationManagementServiceClient, fleetManagementServiceClient, borderClearanceCaseService,
                platformWalletUsageSupport);
    }
}
