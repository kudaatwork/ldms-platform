package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.enums.DriverExpenseReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriverExpenseReconciliationDto {
    private Long id;
    private String expenseReference;
    private Long driverId;
    private Long organizationId;
    private Long tripId;
    private String transactionCurrencyCode;
    private String baseCurrencyCode;
    private Long exchangeRateSnapshotId;
    private BigDecimal amountTransaction;
    private BigDecimal amountBase;
    private String expenseCategory;
    private LocalDate expenseDate;
    private DriverExpenseReconciliationStatus status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
}
