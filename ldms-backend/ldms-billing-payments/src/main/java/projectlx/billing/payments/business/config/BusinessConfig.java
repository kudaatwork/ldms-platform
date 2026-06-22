package projectlx.billing.payments.business.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.billing.payments.business.auditable.api.CountryCurrencySettingServiceAuditable;
import projectlx.billing.payments.business.auditable.api.DriverExpenseReconciliationServiceAuditable;
import projectlx.billing.payments.business.auditable.api.ExchangeRateServiceAuditable;
import projectlx.billing.payments.business.auditable.api.InvoiceLineServiceAuditable;
import projectlx.billing.payments.business.auditable.api.InvoiceServiceAuditable;
import projectlx.billing.payments.business.auditable.api.OrganizationBillingSettingServiceAuditable;
import projectlx.billing.payments.business.auditable.api.OrganizationCurrencySettingServiceAuditable;
import projectlx.billing.payments.business.auditable.api.PaymentServiceAuditable;
import projectlx.billing.payments.business.auditable.api.PlatformActionChargeServiceAuditable;
import projectlx.billing.payments.business.auditable.api.PlatformWalletServiceAuditable;
import projectlx.billing.payments.business.auditable.api.SubscriptionPackageServiceAuditable;
import projectlx.billing.payments.business.auditable.api.UsageChargeRecordServiceAuditable;
import projectlx.billing.payments.business.auditable.api.WalletDepositServiceAuditable;
import projectlx.billing.payments.business.auditable.api.WalletTransactionServiceAuditable;
import projectlx.billing.payments.business.auditable.impl.CountryCurrencySettingServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.DriverExpenseReconciliationServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.ExchangeRateServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.InvoiceLineServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.InvoiceServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.OrganizationBillingSettingServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.OrganizationCurrencySettingServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.PaymentServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.PlatformActionChargeServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.PlatformWalletServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.SubscriptionPackageServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.UsageChargeRecordServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.WalletDepositServiceAuditableImpl;
import projectlx.billing.payments.business.auditable.impl.WalletTransactionServiceAuditableImpl;
import projectlx.billing.payments.business.logic.api.PlatformDashboardService;
import projectlx.billing.payments.business.logic.impl.PlatformDashboardServiceImpl;
import projectlx.billing.payments.business.logic.support.PlatformDashboardSupport;
import projectlx.billing.payments.business.logic.support.PlatformRevenueSupport;
import projectlx.billing.payments.repository.InvoiceRepository;
import projectlx.billing.payments.business.logic.api.BillingVerificationSettingsService;
import projectlx.billing.payments.business.logic.api.CurrencyManagementService;
import projectlx.billing.payments.business.logic.api.DriverExpenseReconciliationService;
import projectlx.billing.payments.business.logic.api.InvoiceService;
import projectlx.billing.payments.business.logic.api.PaymentService;
import projectlx.billing.payments.business.logic.api.PlatformWalletBillingService;
import projectlx.billing.payments.business.logic.impl.BillingVerificationSettingsServiceImpl;
import projectlx.billing.payments.business.logic.impl.CurrencyManagementServiceImpl;
import projectlx.billing.payments.business.logic.impl.DriverExpenseReconciliationServiceImpl;
import projectlx.billing.payments.business.logic.impl.InvoiceServiceImpl;
import projectlx.billing.payments.business.logic.impl.PaymentServiceImpl;
import projectlx.billing.payments.business.logic.impl.PlatformWalletBillingServiceImpl;
import projectlx.billing.payments.business.logic.support.BillingApproverSupport;
import projectlx.billing.payments.business.logic.support.BillingVerificationStageResolver;
import projectlx.billing.payments.business.logic.support.CallerOrganizationResolver;
import projectlx.billing.payments.business.logic.support.CurrencyConversionSupport;
import projectlx.billing.payments.business.logic.support.OrganizationNameResolver;
import projectlx.billing.payments.business.logic.support.PaymentVerificationSupport;
import projectlx.billing.payments.business.logic.support.OrganizationCurrencySupport;
import projectlx.billing.payments.business.logic.support.PlatformWalletUsageNotifier;
import projectlx.billing.payments.business.logic.support.WalletBillingEventPublisher;
import projectlx.billing.payments.business.logic.support.WalletDepositReceiptNotifier;
import projectlx.billing.payments.clients.OrganizationManagementServiceClient;
import projectlx.billing.payments.business.validator.api.CurrencyManagementServiceValidator;
import projectlx.billing.payments.business.validator.api.DriverExpenseReconciliationServiceValidator;
import projectlx.billing.payments.business.validator.api.InvoiceServiceValidator;
import projectlx.billing.payments.business.validator.api.PaymentServiceValidator;
import projectlx.billing.payments.business.validator.api.PlatformWalletBillingServiceValidator;
import projectlx.billing.payments.business.validator.impl.CurrencyManagementServiceValidatorImpl;
import projectlx.billing.payments.business.validator.impl.DriverExpenseReconciliationServiceValidatorImpl;
import projectlx.billing.payments.business.validator.impl.InvoiceServiceValidatorImpl;
import projectlx.billing.payments.business.validator.impl.PaymentServiceValidatorImpl;
import projectlx.billing.payments.business.validator.impl.PlatformWalletBillingServiceValidatorImpl;
import projectlx.billing.payments.clients.InventoryManagementServiceClient;
import projectlx.billing.payments.clients.UserManagementServiceClient;
import projectlx.billing.payments.repository.CountryCurrencySettingRepository;
import projectlx.billing.payments.repository.CurrencyRepository;
import projectlx.billing.payments.repository.DriverExpenseReconciliationRepository;
import projectlx.billing.payments.repository.ExchangeRateRepository;
import projectlx.billing.payments.repository.InvoiceLineRepository;
import projectlx.billing.payments.repository.InvoiceRepository;
import projectlx.billing.payments.repository.OrganizationBillingSettingRepository;
import projectlx.billing.payments.repository.OrganizationCurrencySettingRepository;
import projectlx.billing.payments.repository.PaymentVerificationReviewRepository;
import projectlx.billing.payments.repository.PaymentRepository;
import projectlx.billing.payments.repository.PlatformActionChargeRepository;
import projectlx.billing.payments.repository.PlatformWalletRepository;
import projectlx.billing.payments.repository.SubscriptionPackageRepository;
import projectlx.billing.payments.repository.UsageChargeRecordRepository;
import projectlx.billing.payments.repository.WalletDepositRepository;
import projectlx.billing.payments.repository.WalletTransactionRepository;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@Configuration
public class BusinessConfig {

    // =====================================================================
    // AUDITABLES
    // =====================================================================

    @Bean
    public InvoiceServiceAuditable invoiceServiceAuditable(InvoiceRepository invoiceRepository) {
        return new InvoiceServiceAuditableImpl(invoiceRepository);
    }

    @Bean
    public InvoiceLineServiceAuditable invoiceLineServiceAuditable(InvoiceLineRepository invoiceLineRepository) {
        return new InvoiceLineServiceAuditableImpl(invoiceLineRepository);
    }

    @Bean
    public PaymentServiceAuditable paymentServiceAuditable(PaymentRepository paymentRepository) {
        return new PaymentServiceAuditableImpl(paymentRepository);
    }

    @Bean
    public DriverExpenseReconciliationServiceAuditable driverExpenseReconciliationServiceAuditable(
            DriverExpenseReconciliationRepository driverExpenseReconciliationRepository) {
        return new DriverExpenseReconciliationServiceAuditableImpl(driverExpenseReconciliationRepository);
    }

    @Bean
    public CountryCurrencySettingServiceAuditable countryCurrencySettingServiceAuditable(
            CountryCurrencySettingRepository countryCurrencySettingRepository) {
        return new CountryCurrencySettingServiceAuditableImpl(countryCurrencySettingRepository);
    }

    @Bean
    public ExchangeRateServiceAuditable exchangeRateServiceAuditable(ExchangeRateRepository exchangeRateRepository) {
        return new ExchangeRateServiceAuditableImpl(exchangeRateRepository);
    }

    @Bean
    public OrganizationCurrencySettingServiceAuditable organizationCurrencySettingServiceAuditable(
            OrganizationCurrencySettingRepository organizationCurrencySettingRepository) {
        return new OrganizationCurrencySettingServiceAuditableImpl(organizationCurrencySettingRepository);
    }

    @Bean
    public OrganizationBillingSettingServiceAuditable organizationBillingSettingServiceAuditable(
            OrganizationBillingSettingRepository organizationBillingSettingRepository) {
        return new OrganizationBillingSettingServiceAuditableImpl(organizationBillingSettingRepository);
    }

    @Bean
    public WalletDepositServiceAuditable walletDepositServiceAuditable(WalletDepositRepository walletDepositRepository) {
        return new WalletDepositServiceAuditableImpl(walletDepositRepository);
    }

    @Bean
    public PlatformWalletServiceAuditable platformWalletServiceAuditable(PlatformWalletRepository platformWalletRepository) {
        return new PlatformWalletServiceAuditableImpl(platformWalletRepository);
    }

    @Bean
    public WalletTransactionServiceAuditable walletTransactionServiceAuditable(
            WalletTransactionRepository walletTransactionRepository) {
        return new WalletTransactionServiceAuditableImpl(walletTransactionRepository);
    }

    @Bean
    public PlatformActionChargeServiceAuditable platformActionChargeServiceAuditable(
            PlatformActionChargeRepository platformActionChargeRepository) {
        return new PlatformActionChargeServiceAuditableImpl(platformActionChargeRepository);
    }

    @Bean
    public SubscriptionPackageServiceAuditable subscriptionPackageServiceAuditable(
            SubscriptionPackageRepository subscriptionPackageRepository) {
        return new SubscriptionPackageServiceAuditableImpl(subscriptionPackageRepository);
    }

    @Bean
    public UsageChargeRecordServiceAuditable usageChargeRecordServiceAuditable(
            UsageChargeRecordRepository usageChargeRecordRepository) {
        return new UsageChargeRecordServiceAuditableImpl(usageChargeRecordRepository);
    }

    // =====================================================================
    // VALIDATORS
    // =====================================================================

    @Bean
    public InvoiceServiceValidator invoiceServiceValidator(MessageService messageService) {
        return new InvoiceServiceValidatorImpl(messageService);
    }

    @Bean
    public PaymentServiceValidator paymentServiceValidator(MessageService messageService) {
        return new PaymentServiceValidatorImpl(messageService);
    }

    @Bean
    public DriverExpenseReconciliationServiceValidator driverExpenseReconciliationServiceValidator(
            MessageService messageService) {
        return new DriverExpenseReconciliationServiceValidatorImpl(messageService);
    }

    @Bean
    public CurrencyManagementServiceValidator currencyManagementServiceValidator(MessageService messageService) {
        return new CurrencyManagementServiceValidatorImpl(messageService);
    }

    @Bean
    public PlatformWalletBillingServiceValidator platformWalletBillingServiceValidator(MessageService messageService) {
        return new PlatformWalletBillingServiceValidatorImpl(messageService);
    }

    // =====================================================================
    // SERVICES
    // =====================================================================

    @Bean
    public InvoiceService invoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceLineRepository invoiceLineRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            CurrencyConversionSupport currencyConversionSupport,
            InventoryManagementServiceClient inventoryManagementServiceClient,
            MessageService messageService,
            InvoiceServiceAuditable invoiceServiceAuditable,
            InvoiceLineServiceAuditable invoiceLineServiceAuditable,
            InvoiceServiceValidator invoiceServiceValidator) {
        return new InvoiceServiceImpl(
                invoiceRepository,
                invoiceLineRepository,
                callerOrganizationResolver,
                currencyConversionSupport,
                inventoryManagementServiceClient,
                messageService,
                invoiceServiceAuditable,
                invoiceLineServiceAuditable,
                invoiceServiceValidator);
    }

    @Bean
    public BillingVerificationStageResolver billingVerificationStageResolver(
            OrganizationBillingSettingRepository organizationBillingSettingRepository) {
        return new BillingVerificationStageResolver(organizationBillingSettingRepository);
    }

    @Bean
    public PaymentVerificationSupport paymentVerificationSupport(
            PaymentVerificationReviewRepository paymentVerificationReviewRepository) {
        return new PaymentVerificationSupport(paymentVerificationReviewRepository);
    }

    @Bean
    public BillingVerificationSettingsService billingVerificationSettingsService(
            OrganizationBillingSettingRepository organizationBillingSettingRepository,
            OrganizationBillingSettingServiceAuditable organizationBillingSettingServiceAuditable,
            CallerOrganizationResolver callerOrganizationResolver,
            BillingVerificationStageResolver billingVerificationStageResolver,
            OrganizationNameResolver organizationNameResolver) {
        return new BillingVerificationSettingsServiceImpl(
                organizationBillingSettingRepository,
                organizationBillingSettingServiceAuditable,
                callerOrganizationResolver,
                billingVerificationStageResolver,
                organizationNameResolver);
    }

    @Bean
    public PaymentService paymentService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            CurrencyConversionSupport currencyConversionSupport,
            MessageService messageService,
            RabbitTemplate rabbitTemplate,
            PaymentServiceAuditable paymentServiceAuditable,
            InvoiceServiceAuditable invoiceServiceAuditable,
            PaymentServiceValidator paymentServiceValidator,
            BillingApproverSupport billingApproverSupport,
            BillingVerificationStageResolver billingVerificationStageResolver,
            PaymentVerificationSupport paymentVerificationSupport,
            UserManagementServiceClient userManagementServiceClient) {
        return new PaymentServiceImpl(
                paymentRepository,
                invoiceRepository,
                callerOrganizationResolver,
                currencyConversionSupport,
                messageService,
                rabbitTemplate,
                paymentServiceAuditable,
                invoiceServiceAuditable,
                paymentServiceValidator,
                billingApproverSupport,
                billingVerificationStageResolver,
                paymentVerificationSupport,
                userManagementServiceClient);
    }

    @Bean
    public DriverExpenseReconciliationService driverExpenseReconciliationService(
            DriverExpenseReconciliationRepository driverExpenseReconciliationRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            MessageService messageService,
            DriverExpenseReconciliationServiceAuditable driverExpenseReconciliationServiceAuditable,
            DriverExpenseReconciliationServiceValidator driverExpenseReconciliationServiceValidator) {
        return new DriverExpenseReconciliationServiceImpl(
                driverExpenseReconciliationRepository,
                callerOrganizationResolver,
                messageService,
                driverExpenseReconciliationServiceAuditable,
                driverExpenseReconciliationServiceValidator);
    }

    @Bean
    public CurrencyManagementService currencyManagementService(
            CurrencyRepository currencyRepository,
            CountryCurrencySettingRepository countryCurrencySettingRepository,
            ExchangeRateRepository exchangeRateRepository,
            CurrencyConversionSupport currencyConversionSupport,
            OrganizationCurrencySupport organizationCurrencySupport,
            OrganizationCurrencySettingRepository organizationCurrencySettingRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            MessageService messageService,
            CountryCurrencySettingServiceAuditable countryCurrencySettingServiceAuditable,
            ExchangeRateServiceAuditable exchangeRateServiceAuditable,
            OrganizationCurrencySettingServiceAuditable organizationCurrencySettingServiceAuditable,
            CurrencyManagementServiceValidator currencyManagementServiceValidator) {
        return new CurrencyManagementServiceImpl(
                currencyRepository,
                countryCurrencySettingRepository,
                exchangeRateRepository,
                currencyConversionSupport,
                organizationCurrencySupport,
                organizationCurrencySettingRepository,
                callerOrganizationResolver,
                messageService,
                countryCurrencySettingServiceAuditable,
                exchangeRateServiceAuditable,
                organizationCurrencySettingServiceAuditable,
                currencyManagementServiceValidator);
    }

    @Bean
    public WalletBillingEventPublisher walletBillingEventPublisher(RabbitTemplate rabbitTemplate) {
        return new WalletBillingEventPublisher(rabbitTemplate);
    }

    @Bean
    public PlatformWalletUsageNotifier platformWalletUsageNotifier(
            RabbitTemplate rabbitTemplate,
            OrganizationManagementServiceClient organizationManagementServiceClient) {
        return new PlatformWalletUsageNotifier(rabbitTemplate, organizationManagementServiceClient);
    }

    @Bean
    public PlatformWalletBillingService platformWalletBillingService(
            PlatformWalletRepository platformWalletRepository,
            OrganizationBillingSettingRepository organizationBillingSettingRepository,
            PlatformActionChargeRepository platformActionChargeRepository,
            SubscriptionPackageRepository subscriptionPackageRepository,
            WalletDepositRepository walletDepositRepository,
            WalletTransactionRepository walletTransactionRepository,
            UsageChargeRecordRepository usageChargeRecordRepository,
            CallerOrganizationResolver callerOrganizationResolver,
            OrganizationCurrencySupport organizationCurrencySupport,
            MessageService messageService,
            PlatformWalletServiceAuditable platformWalletServiceAuditable,
            OrganizationBillingSettingServiceAuditable organizationBillingSettingServiceAuditable,
            PlatformActionChargeServiceAuditable platformActionChargeServiceAuditable,
            SubscriptionPackageServiceAuditable subscriptionPackageServiceAuditable,
            WalletDepositServiceAuditable walletDepositServiceAuditable,
            WalletTransactionServiceAuditable walletTransactionServiceAuditable,
            UsageChargeRecordServiceAuditable usageChargeRecordServiceAuditable,
            PlatformWalletBillingServiceValidator platformWalletBillingServiceValidator,
            WalletBillingEventPublisher walletBillingEventPublisher,
            WalletDepositReceiptNotifier walletDepositReceiptNotifier,
            PlatformWalletUsageNotifier platformWalletUsageNotifier,
            OrganizationNameResolver organizationNameResolver) {
        return new PlatformWalletBillingServiceImpl(
                platformWalletRepository,
                organizationBillingSettingRepository,
                platformActionChargeRepository,
                subscriptionPackageRepository,
                walletDepositRepository,
                walletTransactionRepository,
                usageChargeRecordRepository,
                callerOrganizationResolver,
                organizationCurrencySupport,
                messageService,
                platformWalletServiceAuditable,
                organizationBillingSettingServiceAuditable,
                platformActionChargeServiceAuditable,
                subscriptionPackageServiceAuditable,
                walletDepositServiceAuditable,
                walletTransactionServiceAuditable,
                usageChargeRecordServiceAuditable,
                platformWalletBillingServiceValidator,
                walletBillingEventPublisher,
                walletDepositReceiptNotifier,
                platformWalletUsageNotifier,
                organizationNameResolver);
    }

    @Bean
    public PlatformDashboardSupport platformDashboardSupport(InvoiceRepository invoiceRepository) {
        return new PlatformDashboardSupport(invoiceRepository);
    }

    @Bean
    public PlatformRevenueSupport platformRevenueSupport(
            UsageChargeRecordRepository usageChargeRecordRepository,
            WalletDepositRepository walletDepositRepository,
            OrganizationBillingSettingRepository organizationBillingSettingRepository,
            PlatformWalletRepository platformWalletRepository,
            PlatformActionChargeRepository platformActionChargeRepository,
            OrganizationNameResolver organizationNameResolver) {
        return new PlatformRevenueSupport(
                usageChargeRecordRepository,
                walletDepositRepository,
                organizationBillingSettingRepository,
                platformWalletRepository,
                platformActionChargeRepository,
                organizationNameResolver);
    }

    @Bean
    public PlatformDashboardService platformDashboardService(
            PlatformDashboardSupport platformDashboardSupport,
            PlatformRevenueSupport platformRevenueSupport) {
        return new PlatformDashboardServiceImpl(platformDashboardSupport, platformRevenueSupport);
    }
}
