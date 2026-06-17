package projectlx.fuel.expenses.business.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fuel.expenses.business.auditable.api.FuelSessionServiceAuditable;
import projectlx.fuel.expenses.business.auditable.api.FuelTelemetryLogServiceAuditable;
import projectlx.fuel.expenses.business.auditable.api.OperationalFundRequestServiceAuditable;
import projectlx.fuel.expenses.business.auditable.impl.FuelSessionServiceAuditableImpl;
import projectlx.fuel.expenses.business.auditable.impl.FuelTelemetryLogServiceAuditableImpl;
import projectlx.fuel.expenses.business.auditable.impl.OperationalFundRequestServiceAuditableImpl;
import projectlx.fuel.expenses.business.logic.api.FuelSessionService;
import projectlx.fuel.expenses.business.logic.api.FuelTelemetryLogService;
import projectlx.fuel.expenses.business.logic.api.OperationalFundRequestService;
import projectlx.fuel.expenses.business.logic.impl.FuelSessionServiceImpl;
import projectlx.fuel.expenses.business.logic.impl.FuelTelemetryLogServiceImpl;
import projectlx.fuel.expenses.business.logic.impl.OperationalFundRequestServiceImpl;
import projectlx.fuel.expenses.business.logic.support.CallerOrganizationResolver;
import projectlx.fuel.expenses.business.logic.support.FuelExpensesMapper;
import projectlx.fuel.expenses.business.logic.support.FundRequestNumberGenerator;
import projectlx.fuel.expenses.business.validator.api.FuelSessionServiceValidator;
import projectlx.fuel.expenses.business.validator.api.FuelTelemetryLogServiceValidator;
import projectlx.fuel.expenses.business.validator.api.OperationalFundRequestServiceValidator;
import projectlx.fuel.expenses.business.validator.impl.FuelSessionServiceValidatorImpl;
import projectlx.fuel.expenses.business.validator.impl.FuelTelemetryLogServiceValidatorImpl;
import projectlx.fuel.expenses.business.validator.impl.OperationalFundRequestServiceValidatorImpl;
import projectlx.fuel.expenses.clients.TripTrackingServiceClient;
import projectlx.fuel.expenses.repository.FuelSessionRepository;
import projectlx.fuel.expenses.repository.FuelTelemetryLogRepository;
import projectlx.fuel.expenses.repository.OperationalFundRequestRepository;

@Configuration
public class BusinessConfig {

    // ── FuelSession ──────────────────────────────────────────────

    @Bean
    public FuelSessionServiceValidator fuelSessionServiceValidator(MessageService messageService) {
        return new FuelSessionServiceValidatorImpl(messageService);
    }

    @Bean
    public FuelSessionServiceAuditable fuelSessionServiceAuditable(FuelSessionRepository fuelSessionRepository) {
        return new FuelSessionServiceAuditableImpl(fuelSessionRepository);
    }

    @Bean
    public FuelSessionService fuelSessionService(FuelSessionServiceValidator fuelSessionServiceValidator,
                                                 FuelSessionServiceAuditable fuelSessionServiceAuditable,
                                                 FuelSessionRepository fuelSessionRepository,
                                                 RabbitTemplate rabbitTemplate,
                                                 MessageService messageService,
                                                 FuelTelemetryLogService fuelTelemetryLogService,
                                                 TripTrackingServiceClient tripTrackingServiceClient) {
        return new FuelSessionServiceImpl(
                fuelSessionServiceValidator,
                fuelSessionServiceAuditable,
                fuelSessionRepository,
                rabbitTemplate,
                messageService,
                fuelTelemetryLogService,
                tripTrackingServiceClient);
    }

    // ── FuelTelemetryLog ─────────────────────────────────────────

    @Bean
    public FuelTelemetryLogServiceValidator fuelTelemetryLogServiceValidator(MessageService messageService) {
        return new FuelTelemetryLogServiceValidatorImpl(messageService);
    }

    @Bean
    public FuelTelemetryLogServiceAuditable fuelTelemetryLogServiceAuditable(
            FuelTelemetryLogRepository fuelTelemetryLogRepository) {
        return new FuelTelemetryLogServiceAuditableImpl(fuelTelemetryLogRepository);
    }

    @Bean
    public FuelTelemetryLogService fuelTelemetryLogService(
            FuelTelemetryLogServiceValidator fuelTelemetryLogServiceValidator,
            FuelTelemetryLogServiceAuditable fuelTelemetryLogServiceAuditable,
            FuelTelemetryLogRepository fuelTelemetryLogRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            FuelExpensesMapper fuelExpensesMapper,
            MessageService messageService) {
        return new FuelTelemetryLogServiceImpl(
                fuelTelemetryLogServiceValidator,
                fuelTelemetryLogServiceAuditable,
                fuelTelemetryLogRepository,
                callerOrganizationResolver,
                fuelExpensesMapper,
                messageService);
    }

    // ── OperationalFundRequest ───────────────────────────────────

    @Bean
    public OperationalFundRequestServiceValidator operationalFundRequestServiceValidator(
            MessageService messageService) {
        return new OperationalFundRequestServiceValidatorImpl(messageService);
    }

    @Bean
    public OperationalFundRequestServiceAuditable operationalFundRequestServiceAuditable(
            OperationalFundRequestRepository operationalFundRequestRepository) {
        return new OperationalFundRequestServiceAuditableImpl(operationalFundRequestRepository);
    }

    @Bean
    public OperationalFundRequestService operationalFundRequestService(
            OperationalFundRequestServiceValidator operationalFundRequestServiceValidator,
            OperationalFundRequestServiceAuditable operationalFundRequestServiceAuditable,
            OperationalFundRequestRepository operationalFundRequestRepository,
            FuelSessionRepository fuelSessionRepository,
            FuelTelemetryLogService fuelTelemetryLogService,
            CallerOrganizationResolver callerOrganizationResolver,
            FundRequestNumberGenerator fundRequestNumberGenerator,
            FuelExpensesMapper fuelExpensesMapper,
            RabbitTemplate rabbitTemplate,
            MessageService messageService,
            TripTrackingServiceClient tripTrackingServiceClient) {
        return new OperationalFundRequestServiceImpl(
                operationalFundRequestServiceValidator,
                operationalFundRequestServiceAuditable,
                operationalFundRequestRepository,
                fuelSessionRepository,
                fuelTelemetryLogService,
                callerOrganizationResolver,
                fundRequestNumberGenerator,
                fuelExpensesMapper,
                rabbitTemplate,
                messageService,
                tripTrackingServiceClient);
    }
}
