package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.inventory.management.model.SupplierQuoteSource;
import projectlx.co.zw.shared_library.model.PaymentTerm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SubmitSupplierQuoteRequest {
    private Long purchaseRequisitionId;
    private Long supplierOrganizationId;
    private Long submittedByUserId;
    private SupplierQuoteSource quoteSource;
    private Long externalDocumentId;
    private String currency;
    private BigDecimal taxAmount;
    private PaymentTerm paymentTerm;
    private String deliveryTerms;
    private LocalDate validityUntil;
    private String notes;
    private List<SupplierQuoteLineRequest> lines;

    @Getter
    @Setter
    public static class SupplierQuoteLineRequest {
        private Long purchaseRequisitionLineId;
        private Long productId;
        private BigDecimal quotedQuantity;
        private BigDecimal unitPrice;
        private Integer leadTimeDays;
        private String notes;
    }
}
