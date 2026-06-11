package projectlx.trip.tracking.business.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import projectlx.trip.tracking.business.auditable.api.DeliveryOtpServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripEventServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripServiceAuditable;
import projectlx.trip.tracking.business.auditable.impl.DeliveryOtpServiceAuditableImpl;
import projectlx.trip.tracking.business.auditable.impl.TripEventServiceAuditableImpl;
import projectlx.trip.tracking.business.auditable.impl.TripServiceAuditableImpl;
import projectlx.trip.tracking.business.logic.api.TripService;
import projectlx.trip.tracking.business.logic.impl.TripServiceImpl;
import projectlx.trip.tracking.business.logic.support.CallerOrganizationResolver;
import projectlx.trip.tracking.business.logic.support.TripNumberGenerator;
import projectlx.trip.tracking.business.validator.api.TripServiceValidator;
import projectlx.trip.tracking.business.validator.impl.TripServiceValidatorImpl;
import projectlx.trip.tracking.clients.InventoryManagementServiceClient;
import projectlx.trip.tracking.clients.ShipmentManagementServiceClient;
import projectlx.trip.tracking.repository.DeliveryOtpRepository;
import projectlx.trip.tracking.repository.TripEventRepository;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

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
                                   BCryptPasswordEncoder bCryptPasswordEncoder) {
        return new TripServiceImpl(tripServiceValidator, tripServiceAuditable, tripEventServiceAuditable,
                deliveryOtpServiceAuditable, tripRepository, tripEventRepository, deliveryOtpRepository,
                callerOrganizationResolver, tripNumberGenerator, shipmentManagementServiceClient,
                inventoryManagementServiceClient, rabbitTemplate, messageService, bCryptPasswordEncoder);
    }
}
