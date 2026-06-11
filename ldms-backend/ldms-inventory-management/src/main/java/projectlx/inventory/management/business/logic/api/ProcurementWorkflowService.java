package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.utils.requests.ApprovePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.ApprovePurchaseOrderStageRequest;
import projectlx.inventory.management.utils.requests.ApproveSalesOrderStageRequest;
import projectlx.inventory.management.utils.requests.AcknowledgeSupplierQuoteRequest;
import projectlx.inventory.management.utils.requests.SubmitSupplierQuoteRequest;
import projectlx.inventory.management.utils.responses.PurchaseRequisitionResponse;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;
import projectlx.inventory.management.utils.responses.SupplierQuoteResponse;

import java.util.Locale;

public interface ProcurementWorkflowService {

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

    SalesOrderResponse createSalesOrderFromPaidPurchaseOrder(Long purchaseOrderId,
                                                             Long supplierOrganizationId,
                                                             Long createdByUserId,
                                                             Locale locale, String username);

    SupplierQuoteResponse findSupplierQuotes(Long supplierOrganizationId, Locale locale, String username);

    SupplierQuoteResponse findCustomerQuotes(Long customerOrganizationId, Locale locale, String username);

    SupplierQuoteResponse findQuoteByRequisitionId(Long requisitionId, Locale locale, String username);

    SalesOrderResponse findCustomerSalesOrders(Long customerOrganizationId, Locale locale, String username);
}
