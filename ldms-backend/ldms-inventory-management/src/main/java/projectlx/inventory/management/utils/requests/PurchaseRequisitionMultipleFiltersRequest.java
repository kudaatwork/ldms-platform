package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FulfillmentMethod;
import projectlx.inventory.management.model.PriorityLevel;
import projectlx.inventory.management.model.PurchaseRequisitionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
public class PurchaseRequisitionMultipleFiltersRequest {

    // Pagination
    private Integer page;
    private Integer size;

    // Search
    private String searchTerm;

    // Filters
    private Long organizationId;
    private Long departmentId;
    private Long requestedByUserId;
    private Long approvedByUserId;
    private PurchaseRequisitionStatus status;
    private PriorityLevel priority;
    private FulfillmentMethod fulfillmentMethod;

    // Date ranges
    private LocalDate requisitionDateFrom;
    private LocalDate requisitionDateTo;
    private LocalDate requiredByDateFrom;
    private LocalDate requiredByDateTo;
    private LocalDate expiryDateFrom;
    private LocalDate expiryDateTo;

    // Amount ranges
    private BigDecimal estimatedTotalMin;
    private BigDecimal estimatedTotalMax;

    // Other filters
    private String costCenter;
    private String projectCode;
    private Long preferredSupplierId;
    private Long targetWarehouseId;
    private Boolean budgetAvailable;

    // Sort
    private String sortBy;
    private String sortDirection;
}
