package projectlx.shipment.management.business.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.shipment.management.business.auditable.api.ShipmentServiceAuditable;
import projectlx.shipment.management.business.auditable.impl.ShipmentServiceAuditableImpl;
import projectlx.shipment.management.business.logic.api.ShipmentService;
import projectlx.shipment.management.business.logic.impl.ShipmentServiceImpl;
import projectlx.shipment.management.business.logic.support.CallerOrganizationResolver;
import projectlx.shipment.management.business.validator.api.ShipmentServiceValidator;
import projectlx.shipment.management.business.validator.impl.ShipmentServiceValidatorImpl;
import projectlx.shipment.management.repository.ShipmentRepository;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

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
    public ShipmentService shipmentService(ShipmentServiceValidator shipmentServiceValidator,
                                           ShipmentServiceAuditable shipmentServiceAuditable,
                                           ShipmentRepository shipmentRepository,
                                           CallerOrganizationResolver callerOrganizationResolver,
                                           RabbitTemplate rabbitTemplate,
                                           MessageService messageService) {
        return new ShipmentServiceImpl(shipmentServiceValidator, shipmentServiceAuditable,
                shipmentRepository, callerOrganizationResolver, rabbitTemplate, messageService);
    }
}
