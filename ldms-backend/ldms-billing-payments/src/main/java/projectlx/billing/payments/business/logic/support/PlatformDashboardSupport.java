package projectlx.billing.payments.business.logic.support;

import projectlx.billing.payments.repository.InvoiceRepository;
import projectlx.billing.payments.utils.dtos.PlatformBillingDashboardDto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PlatformDashboardSupport {

    private final InvoiceRepository invoiceRepository;

    public PlatformDashboardSupport(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public PlatformBillingDashboardDto buildDashboardSnapshot() {
        BigDecimal pendingBase = invoiceRepository.sumPendingInvoiceBaseAmount();
        if (pendingBase == null) {
            pendingBase = BigDecimal.ZERO;
        }
        long cents = pendingBase.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        PlatformBillingDashboardDto dto = new PlatformBillingDashboardDto();
        dto.setPendingInvoicesCents(cents);
        return dto;
    }
}
