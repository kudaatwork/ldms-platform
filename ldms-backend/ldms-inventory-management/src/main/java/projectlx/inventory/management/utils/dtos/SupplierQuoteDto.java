package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.inventory.management.model.SupplierQuoteSource;
import projectlx.inventory.management.model.SupplierQuoteStatus;
import projectlx.co.zw.shared_library.model.PaymentTerm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierQuoteDto {
    private Long id;
    private String quoteNumber;
    private Long purchaseRequisitionId;
    private String requisitionNumber;
    private Long supplierOrganizationId;
    private Long customerOrganizationId;
    private SupplierQuoteStatus status;
    private SupplierQuoteSource quoteSource;
    private Long externalDocumentId;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private PaymentTerm paymentTerm;
    private String deliveryTerms;
    private LocalDate validityUntil;
    private String notes;
    private LocalDateTime submittedAt;
    private Long submittedByUserId;
    private List<SupplierQuoteLineDto> lines;
}
