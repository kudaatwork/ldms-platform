package projectlx.trip.tracking.business.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.trip.tracking.business.auditable.api.DeliveryOtpServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripEventServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripServiceAuditable;
import projectlx.trip.tracking.business.auditable.impl.DeliveryOtpServiceAuditableImpl;
import projectlx.trip.tracking.business.auditable.impl.TripEventServiceAuditableImpl;
import projectlx.trip.tracking.business.auditable.impl.TripServiceAuditableImpl;
import projectlx.trip.tracking.business.logic.api.TripLiveService;
import projectlx.trip.tracking.business.logic.api.TripService;
import projectlx.trip.tracking.business.logic.api.TripTelemetryIngestService;
import projectlx.trip.tracking.business.logic.impl.TripLiveServiceImpl;
import projectlx.trip.tracking.business.logic.impl.TripServiceImpl;
import projectlx.trip.tracking.business.logic.impl.TripTelemetryIngestServiceImpl;
import projectlx.trip.tracking.business.logic.support.CallerOrganizationResolver;
import projectlx.trip.tracking.business.logic.support.TripJourneyTimingSupport;
import projectlx.trip.tracking.business.logic.support.TripIotDemoSimulator;
import projectlx.trip.tracking.business.logic.support.TripLiveSnapshotEnricher;
import projectlx.trip.tracking.business.logic.support.TripLiveSseRegistry;
import projectlx.trip.tracking.business.logic.support.TripTrailSupport;
import projectlx.trip.tracking.business.logic.support.TripNumberGenerator;
import projectlx.trip.tracking.business.logic.support.ShipmentTripStartLock;
import projectlx.trip.tracking.business.logic.support.TripRoutePlannerSupport;
import projectlx.trip.tracking.business.logic.support.TripTelemetryPublisher;
import projectlx.trip.tracking.business.validator.api.TripServiceValidator;
import projectlx.trip.tracking.business.validator.impl.TripServiceValidatorImpl;
import projectlx.trip.tracking.clients.FleetManagementServiceClient;
import projectlx.trip.tracking.clients.FuelExpensesServiceClient;
import projectlx.trip.tracking.clients.InventoryManagementServiceClient;
import projectlx.trip.tracking.clients.OrganizationManagementServiceClient;
import projectlx.trip.tracking.clients.ShipmentManagementServiceClient;
import projectlx.trip.tracking.clients.UserManagementServiceClient;
import projectlx.trip.tracking.business.logic.support.LogisticsNotificationRecipientResolver;
import projectlx.trip.tracking.repository.DeliveryOtpRepository;
import projectlx.trip.tracking.repository.TripEventRepository;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.repository.TripRoutePlanRepository;
import projectlx.trip.tracking.utils.config.IotIntegrationProperties;
import projectlx.co.zw.shared_library.utils.notifications.LogisticsLifecycleNotificationSupport;

import java.util.Optional;

@Configuration
public class BusinessConfig {

    @Bean
    public TripServiceValidator tripServiceValidator(MessageService messageService) {
        return new TripServiceValidatorImpl(messageService);
    }

    @Bean
    public TripServiceAuditable tripServiceAuditable(TripRepository tripRepository) {
        return new TripServiceAuditableImpl(tripRepository);
    }

    @Bean
    public TripEventServiceAuditable tripEventServiceAuditable(TripEventRepository tripEventRepository) {
        return new TripEventServiceAuditableImpl(tripEventRepository);
    }

    @Bean
    public DeliveryOtpServiceAuditable deliveryOtpServiceAuditable(DeliveryOtpRepository deliveryOtpRepository) {
        return new DeliveryOtpServiceAuditableImpl(deliveryOtpRepository);
    }

    @Bean
    public TripRoutePlannerSupport tripRoutePlannerSupport(TripRoutePlanRepository tripRoutePlanRepository,
                                                           ObjectMapper objectMapper) {
        return new TripRoutePlannerSupport(tripRoutePlanRepository, objectMapper);
    }

    @Bean
    public TripTelemetryPublisher tripTelemetryPublisher(RabbitTemplate rabbitTemplate,
                                                         TripLiveSseRegistry sseRegistry,
                                                         IotIntegrationProperties iotProperties,
                                                         ObjectMapper objectMapper,
                                                         Optional<MqttClient> optionalTripTrackingMqttClient,
                                                         Optional<FuelExpensesServiceClient> fuelExpensesServiceClient) {
        return new TripTelemetryPublisher(rabbitTemplate, sseRegistry, iotProperties, objectMapper,
                optionalTripTrackingMqttClient, fuelExpensesServiceClient);
    }

    @Bean
    public TripTrailSupport tripTrailSupport(ObjectMapper objectMapper) {
        return new TripTrailSupport(objectMapper);
    }

    @Bean
    public TripJourneyTimingSupport tripJourneyTimingSupport() {
        return new TripJourneyTimingSupport();
    }

    @Bean
    public TripLiveSnapshotEnricher tripLiveSnapshotEnricher(FleetManagementServiceClient fleetManagementServiceClient,
                                                             ShipmentManagementServiceClient shipmentManagementServiceClient,
                                                             TripTrailSupport tripTrailSupport,
                                                             TripJourneyTimingSupport tripJourneyTimingSupport,
                                                             TripRoutePlanRepository tripRoutePlanRepository) {
        return new TripLiveSnapshotEnricher(fleetManagementServiceClient, shipmentManagementServiceClient,
                tripTrailSupport, tripJourneyTimingSupport, tripRoutePlanRepository);
    }

    @Bean
    public TripIotDemoSimulator tripIotDemoSimulator(TripRoutePlanRepository tripRoutePlanRepository,
                                                     TripRepository tripRepository,
                                                     TripRoutePlannerSupport tripRoutePlannerSupport,
                                                     TripTelemetryPublisher tripTelemetryPublisher,
                                                     TripEventServiceAuditable tripEventServiceAuditable,
                                                     TripTrailSupport tripTrailSupport,
                                                     TripJourneyTimingSupport tripJourneyTimingSupport,
                                                     IotIntegrationProperties iotProperties,
                                                     FleetManagementServiceClient fleetManagementServiceClient) {
        return new TripIotDemoSimulator(tripRoutePlanRepository, tripRepository,
                tripRoutePlannerSupport, tripTelemetryPublisher, tripEventServiceAuditable,
                tripTrailSupport, tripJourneyTimingSupport, iotProperties, fleetManagementServiceClient);
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
    public TripService tripService(TripServiceValidator tripServiceValidator,
                                   TripServiceAuditable tripServiceAuditable,
                                   TripEventServiceAuditable tripEventServiceAuditable,
                                   DeliveryOtpServiceAuditable deliveryOtpServiceAuditable,
                                   TripRepository tripRepository,
                                   TripEventRepository tripEventRepository,
                                   DeliveryOtpRepository deliveryOtpRepository,
                                   CallerOrganizationResolver callerOrganizationResolver,
                                   TripNumberGenerator tripNumberGenerator,
                                   ShipmentManagementServiceClient shipmentManagementServiceClient,
                                   InventoryManagementServiceClient inventoryManagementServiceClient,
                                   RabbitTemplate rabbitTemplate,
                                   MessageService messageService,
                                   BCryptPasswordEncoder bCryptPasswordEncoder,
                                   TripRoutePlannerSupport routePlannerSupport,
                                   TripIotDemoSimulator demoSimulator,
                                   TripTelemetryPublisher telemetryPublisher,
                                   TripRoutePlanRepository tripRoutePlanRepository,
                                   IotIntegrationProperties iotProperties,
                                   ShipmentTripStartLock shipmentTripStartLock,
                                   LogisticsLifecycleNotificationSupport logisticsLifecycleNotificationSupport,
                                   LogisticsNotificationRecipientResolver logisticsNotificationRecipientResolver) {
        return new TripServiceImpl(tripServiceValidator, tripServiceAuditable, tripEventServiceAuditable,
                deliveryOtpServiceAuditable, tripRepository, tripEventRepository, deliveryOtpRepository,
                callerOrganizationResolver, tripNumberGenerator, shipmentManagementServiceClient,
                inventoryManagementServiceClient, rabbitTemplate, messageService, bCryptPasswordEncoder,
                routePlannerSupport, demoSimulator, telemetryPublisher, tripRoutePlanRepository, iotProperties,
                shipmentTripStartLock, logisticsLifecycleNotificationSupport, logisticsNotificationRecipientResolver);
    }

    @Bean
    public TripTelemetryIngestService tripTelemetryIngestService(
            FleetManagementServiceClient fleetManagementServiceClient,
            TripRepository tripRepository,
            TripRoutePlannerSupport tripRoutePlannerSupport,
            TripTelemetryPublisher tripTelemetryPublisher,
            TripRoutePlanRepository tripRoutePlanRepository) {
        return new TripTelemetryIngestServiceImpl(
                fleetManagementServiceClient,
                tripRepository,
                tripRoutePlannerSupport,
                tripTelemetryPublisher,
                tripRoutePlanRepository);
    }

    @Bean
    public TripLiveService tripLiveService(TripRepository tripRepository,
                                           TripRoutePlanRepository tripRoutePlanRepository,
                                           CallerOrganizationResolver callerOrganizationResolver,
                                           TripRoutePlannerSupport routePlannerSupport,
                                           TripIotDemoSimulator demoSimulator,
                                           TripLiveSseRegistry sseRegistry,
                                           TripTelemetryPublisher telemetryPublisher,
                                           TripLiveSnapshotEnricher snapshotEnricher,
                                           MessageService messageService) {
        return new TripLiveServiceImpl(tripRepository, tripRoutePlanRepository, callerOrganizationResolver,
                routePlannerSupport, demoSimulator, sseRegistry, telemetryPublisher, snapshotEnricher, messageService);
    }
}
