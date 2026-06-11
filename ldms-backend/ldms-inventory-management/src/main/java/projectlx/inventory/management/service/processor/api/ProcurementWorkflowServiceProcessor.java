package projectlx.inventory.management.service.processor.api;

import projectlx.inventory.management.utils.requests.*;
import projectlx.inventory.management.utils.responses.*;

import java.util.Locale;

public interface ProcurementWorkflowServiceProcessor {

    PurchaseRequisitionResponse approveInternalStage(ApprovePurchaseRequisitionRequest request,
                                                     Locale locale, String username);

    PurchaseRequisitionResponse publishToSupplier(Long requisitionId, Locale locale, String username);

    PurchaseRequisitionResponse findSupplierVisibleRequisitions(Long supplierOrganizationId,
                                                                Locale locale, String username);

    SupplierQuoteResponse submitSupplierQuote(SubmitSupplierQuoteRequest request, Locale locale, String username);

    PurchaseRequisitionResponse acknowledgeSupplierQuote(AcknowledgeSupplierQuoteRequest request,
                                                         Locale locale, String username);

    PurchaseOrderResponse approvePurchaseOrderCustomerStage(ApprovePurchaseOrderStageRequest request,
                                                            Locale locale, String username);

    PurchaseOrderResponse approvePurchaseOrderSupplierStage(ApprovePurchaseOrderStageRequest request,
                                                            Locale locale, String username);

    SalesOrderResponse approveSalesOrderStage(ApproveSalesOrderStageRequest request,
                                              Locale locale, String username);

    SupplierQuoteResponse findSupplierQuotes(Long supplierOrganizationId, Locale locale, String username);

    SupplierQuoteResponse findCustomerQuotes(Long customerOrganizationId, Locale locale, String username);

    SupplierQuoteResponse findQuoteByRequisitionId(Long requisitionId, Locale locale, String username);

    SalesOrderResponse findCustomerSalesOrders(Long customerOrganizationId, Locale locale, String username);
}
