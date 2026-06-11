package projectlx.inventory.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.ProcurementWorkflowService;
import projectlx.inventory.management.service.processor.api.ProcurementWorkflowServiceProcessor;
import projectlx.inventory.management.utils.requests.*;
import projectlx.inventory.management.utils.responses.*;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementWorkflowServiceProcessorImpl implements ProcurementWorkflowServiceProcessor {

    private final ProcurementWorkflowService procurementWorkflowService;

    @Override
    public PurchaseRequisitionResponse approveInternalStage(ApprovePurchaseRequisitionRequest request,
                                                            Locale locale, String username) {
        log.info("Processing approveInternalStage for PR: {} by user: {}", request.getId(), username);
        return procurementWorkflowService.approveInternalStage(request, locale, username);
    }

    @Override
    public PurchaseRequisitionResponse publishToSupplier(Long requisitionId, Locale locale, String username) {
        log.info("Processing publishToSupplier for PR: {} by user: {}", requisitionId, username);
        return procurementWorkflowService.publishToSupplier(requisitionId, locale, username);
    }

    @Override
    public PurchaseRequisitionResponse findSupplierVisibleRequisitions(Long supplierOrganizationId,
                                                                       Locale locale, String username) {
        log.info("Processing findSupplierVisibleRequisitions for supplier org: {}", supplierOrganizationId);
        return procurementWorkflowService.findSupplierVisibleRequisitions(supplierOrganizationId, locale, username);
    }

    @Override
    public SupplierQuoteResponse submitSupplierQuote(SubmitSupplierQuoteRequest request,
                                                     Locale locale, String username) {
        log.info("Processing submitSupplierQuote for PR: {} by supplier org: {}",
                request.getPurchaseRequisitionId(), request.getSupplierOrganizationId());
        return procurementWorkflowService.submitSupplierQuote(request, locale, username);
    }

    @Override
    public PurchaseRequisitionResponse acknowledgeSupplierQuote(AcknowledgeSupplierQuoteRequest request,
                                                                Locale locale, String username) {
        log.info("Processing acknowledgeSupplierQuote for PR: {} by user: {}",
                request.getPurchaseRequisitionId(), username);
        return procurementWorkflowService.acknowledgeSupplierQuote(request, locale, username);
    }

    @Override
    public PurchaseOrderResponse approvePurchaseOrderCustomerStage(ApprovePurchaseOrderStageRequest request,
                                                                   Locale locale, String username) {
        log.info("Processing approvePurchaseOrderCustomerStage for PO: {} by user: {}",
                request.getPurchaseOrderId(), username);
        return procurementWorkflowService.approvePurchaseOrderCustomerStage(request, locale, username);
    }

    @Override
    public PurchaseOrderResponse approvePurchaseOrderSupplierStage(ApprovePurchaseOrderStageRequest request,
                                                                   Locale locale, String username) {
        log.info("Processing approvePurchaseOrderSupplierStage for PO: {} by user: {}",
                request.getPurchaseOrderId(), username);
        return procurementWorkflowService.approvePurchaseOrderSupplierStage(request, locale, username);
    }

    @Override
    public SalesOrderResponse approveSalesOrderStage(ApproveSalesOrderStageRequest request,
                                                     Locale locale, String username) {
        log.info("Processing approveSalesOrderStage for SO: {} by user: {}", request.getSalesOrderId(), username);
        return procurementWorkflowService.approveSalesOrderStage(request, locale, username);
    }

    @Override
    public SupplierQuoteResponse findSupplierQuotes(Long supplierOrganizationId, Locale locale, String username) {
        log.info("Processing findSupplierQuotes for supplier org: {} by user: {}", supplierOrganizationId, username);
        return procurementWorkflowService.findSupplierQuotes(supplierOrganizationId, locale, username);
    }

    @Override
    public SupplierQuoteResponse findCustomerQuotes(Long customerOrganizationId, Locale locale, String username) {
        log.info("Processing findCustomerQuotes for customer org: {} by user: {}", customerOrganizationId, username);
        return procurementWorkflowService.findCustomerQuotes(customerOrganizationId, locale, username);
    }

    @Override
    public SupplierQuoteResponse findQuoteByRequisitionId(Long requisitionId, Locale locale, String username) {
        log.info("Processing findQuoteByRequisitionId for requisition: {} by user: {}", requisitionId, username);
        return procurementWorkflowService.findQuoteByRequisitionId(requisitionId, locale, username);
    }

    @Override
    public SalesOrderResponse findCustomerSalesOrders(Long customerOrganizationId, Locale locale, String username) {
        log.info("Processing findCustomerSalesOrders for customer org: {} by user: {}", customerOrganizationId, username);
        return procurementWorkflowService.findCustomerSalesOrders(customerOrganizationId, locale, username);
    }
}
