package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.logic.api.PurchaseRequisitionService;
import projectlx.inventory.management.service.processor.api.PurchaseRequisitionServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.PurchaseRequisitionDto;
import projectlx.inventory.management.utils.requests.ApprovePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CancelPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CreatePOFromPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CreatePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.FulfillPurchaseRequisitionLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseRequisitionMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.RejectPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.inventory.management.utils.responses.PurchaseRequisitionResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class PurchaseRequisitionServiceProcessorImpl implements PurchaseRequisitionServiceProcessor {

    private final PurchaseRequisitionService purchaseRequisitionService;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseRequisitionServiceProcessorImpl.class);

    // === CRUD OPERATIONS ===

    @Override
    public PurchaseRequisitionResponse create(CreatePurchaseRequisitionRequest request, Locale locale, String username) {
        logger.info("Incoming request to create purchase requisition for user: {}", username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.create(request, locale, username);
        logger.info("Outgoing response after creating purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find purchase requisition by ID: {} for user: {}", id, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.findById(id, locale, username);
        logger.info("Outgoing response after finding purchase requisition by ID: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all purchase requisitions as list for user: {}", username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.findAllAsList(locale, username);
        logger.info("Outgoing response after finding all purchase requisitions: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse update(EditPurchaseRequisitionRequest request, String username, Locale locale) {
        logger.info("Incoming request to update purchase requisition for user: {}", username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.update(request, username, locale);
        logger.info("Outgoing response after updating purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete purchase requisition by ID: {} for user: {}", id, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.delete(id, locale, username);
        logger.info("Outgoing response after deleting purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse findByMultipleFilters(PurchaseRequisitionMultipleFiltersRequest request,
                                                              String username, Locale locale) {
        logger.info("Incoming request to find purchase requisitions by multiple filters for user: {}", username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.findByMultipleFilters(request, username, locale);
        logger.info("Outgoing response after finding purchase requisitions by filters: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    // === WORKFLOW OPERATIONS ===

    @Override
    public PurchaseRequisitionResponse submit(Long id, Long submittedByUserId, Locale locale, String username) {
        logger.info("Incoming request to submit purchase requisition ID: {} for user: {}", id, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.submit(id, submittedByUserId, locale, username);
        logger.info("Outgoing response after submitting purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse approve(ApprovePurchaseRequisitionRequest request, Locale locale, String username) {
        logger.info("Incoming request to approve purchase requisition ID: {} for user: {}",
                request != null ? request.getId() : null, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.approve(request, locale, username);
        logger.info("Outgoing response after approving purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse reject(RejectPurchaseRequisitionRequest request, Locale locale, String username) {
        logger.info("Incoming request to reject purchase requisition ID: {} for user: {}",
                request != null ? request.getId() : null, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.reject(request, locale, username);
        logger.info("Outgoing response after rejecting purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse cancel(CancelPurchaseRequisitionRequest request, Locale locale, String username) {
        logger.info("Incoming request to cancel purchase requisition ID: {} for user: {}",
                request != null ? request.getId() : null, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.cancel(request, locale, username);
        logger.info("Outgoing response after cancelling purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse close(Long id, Long closedByUserId, String reason, Locale locale, String username) {
        logger.info("Incoming request to close purchase requisition ID: {} for user: {}", id, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.close(id, closedByUserId, reason, locale, username);
        logger.info("Outgoing response after closing purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    // === FULFILLMENT OPERATIONS ===

    @Override
    public PurchaseRequisitionResponse fulfillLine(FulfillPurchaseRequisitionLineRequest request, Locale locale, String username) {
        logger.info("Incoming request to fulfill purchase requisition line for PR ID: {}, Line ID: {} for user: {}",
                request != null ? request.getPurchaseRequisitionId() : null,
                request != null ? request.getLineId() : null, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.fulfillLine(request, locale, username);
        logger.info("Outgoing response after fulfilling purchase requisition line: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    // === PO CONVERSION ===

    @Override
    public PurchaseOrderResponse createPurchaseOrderFromPR(CreatePOFromPurchaseRequisitionRequest request,
                                                            Locale locale, String username) {
        logger.info("Incoming request to create PO from purchase requisition ID: {} for user: {}",
                request != null ? request.getPurchaseRequisitionId() : null, username);
        PurchaseOrderResponse response = purchaseRequisitionService.createPurchaseOrderFromPR(request, locale, username);
        logger.info("Outgoing response after creating PO from purchase requisition: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    // === UTILITY OPERATIONS ===

    @Override
    public PurchaseRequisitionResponse findByDepartment(Long departmentId, Locale locale, String username) {
        logger.info("Incoming request to find purchase requisitions by department ID: {} for user: {}",
                departmentId, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.findByDepartment(departmentId, locale, username);
        logger.info("Outgoing response after finding purchase requisitions by department: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse findPendingApprovals(Long departmentId, Locale locale, String username) {
        logger.info("Incoming request to find pending approval purchase requisitions for department ID: {} for user: {}",
                departmentId, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.findPendingApprovals(departmentId, locale, username);
        logger.info("Outgoing response after finding pending approval purchase requisitions: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public PurchaseRequisitionResponse findApprovedPendingFulfillment(Long organizationId, Locale locale, String username) {
        logger.info("Incoming request to find approved pending fulfillment purchase requisitions for org ID: {} for user: {}",
                organizationId, username);
        PurchaseRequisitionResponse response = purchaseRequisitionService.findApprovedPendingFulfillment(organizationId, locale, username);
        logger.info("Outgoing response after finding approved pending fulfillment purchase requisitions: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public void expireOverdueRequisitions() {
        logger.info("Incoming request to expire overdue purchase requisitions");
        purchaseRequisitionService.expireOverdueRequisitions();
        logger.info("Completed expiring overdue purchase requisitions");
    }

    // === EXPORT/IMPORT OPERATIONS ===

    @Override
    public byte[] exportToCsv(List<PurchaseRequisitionDto> items) {
        logger.info("Incoming request to export {} purchase requisitions to CSV", items != null ? items.size() : 0);
        // TODO: Implement CSV export in service layer
        logger.info("Export to CSV not yet implemented");
        return new byte[0];
    }

    @Override
    public byte[] exportToExcel(List<PurchaseRequisitionDto> items) throws IOException {
        logger.info("Incoming request to export {} purchase requisitions to Excel", items != null ? items.size() : 0);
        // TODO: Implement Excel export in service layer
        logger.info("Export to Excel not yet implemented");
        return new byte[0];
    }

    @Override
    public byte[] exportToPdf(List<PurchaseRequisitionDto> items) throws DocumentException {
        logger.info("Incoming request to export {} purchase requisitions to PDF", items != null ? items.size() : 0);
        // TODO: Implement PDF export in service layer
        logger.info("Export to PDF not yet implemented");
        return new byte[0];
    }

    @Override
    public ImportSummary importPurchaseRequisitionFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import purchase requisitions from CSV");
        // TODO: Implement CSV import in service layer
        logger.info("Import from CSV not yet implemented");
        return null;
    }
}
