package projectlx.billing.payments.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.billing.payments.business.logic.api.CurrencyManagementService;
import projectlx.billing.payments.business.logic.api.DriverExpenseReconciliationService;
import projectlx.billing.payments.business.logic.api.InvoiceService;
import projectlx.billing.payments.business.logic.api.PaymentService;
import projectlx.billing.payments.business.logic.api.PlatformDashboardService;
import projectlx.billing.payments.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.billing.payments.service.processor.impl.PlatformDashboardServiceProcessorImpl;
import projectlx.billing.payments.service.processor.api.DriverExpenseServiceProcessor;
import projectlx.billing.payments.service.processor.api.InvoiceServiceProcessor;
import projectlx.billing.payments.service.processor.api.PaymentServiceProcessor;
import projectlx.billing.payments.business.logic.api.PlatformWalletBillingService;
import projectlx.billing.payments.service.processor.api.PlatformWalletBillingServiceProcessor;
import projectlx.billing.payments.service.processor.impl.CurrencyManagementServiceProcessorImpl;
import projectlx.billing.payments.service.processor.impl.DriverExpenseServiceProcessorImpl;
import projectlx.billing.payments.service.processor.impl.InvoiceServiceProcessorImpl;
import projectlx.billing.payments.service.processor.impl.PlatformWalletBillingServiceProcessorImpl;
import projectlx.billing.payments.service.processor.impl.PaymentServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public PlatformDashboardServiceProcessor platformDashboardServiceProcessor(
            PlatformDashboardService platformDashboardService) {
        return new PlatformDashboardServiceProcessorImpl(platformDashboardService);
    }

    @Bean
    public CurrencyManagementServiceProcessor currencyManagementServiceProcessor(CurrencyManagementService service) {
        return new CurrencyManagementServiceProcessorImpl(service);
    }

    @Bean
    public InvoiceServiceProcessor invoiceServiceProcessor(InvoiceService service) {
        return new InvoiceServiceProcessorImpl(service);
    }

    @Bean
    public PaymentServiceProcessor paymentServiceProcessor(PaymentService service) {
        return new PaymentServiceProcessorImpl(service);
    }

    @Bean
    public DriverExpenseServiceProcessor driverExpenseServiceProcessor(DriverExpenseReconciliationService service) {
        return new DriverExpenseServiceProcessorImpl(service);
    }

    @Bean
    public PlatformWalletBillingServiceProcessor platformWalletBillingServiceProcessor(PlatformWalletBillingService service) {
        return new PlatformWalletBillingServiceProcessorImpl(service);
    }
}
