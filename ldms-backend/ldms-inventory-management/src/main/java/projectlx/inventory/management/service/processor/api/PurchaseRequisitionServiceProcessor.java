package projectlx.inventory.management.service.processor.api;

import com.lowagie.text.DocumentException;
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

public interface PurchaseRequisitionServiceProcessor {

    // === CRUD OPERATIONS ===
    PurchaseRequisitionResponse create(CreatePurchaseRequisitionRequest request, Locale locale, String username);

    PurchaseRequisitionResponse findById(Long id, Locale locale, String username);

    PurchaseRequisitionResponse findAllAsList(Locale locale, String username);

    PurchaseRequisitionResponse update(EditPurchaseRequisitionRequest request, String username, Locale locale);

    PurchaseRequisitionResponse delete(Long id, Locale locale, String username);

    PurchaseRequisitionResponse findByMultipleFilters(PurchaseRequisitionMultipleFiltersRequest request, String username, Locale locale);

    // === WORKFLOW OPERATIONS ===
    PurchaseRequisitionResponse submit(Long id, Long submittedByUserId, Locale locale, String username);

    PurchaseRequisitionResponse approve(ApprovePurchaseRequisitionRequest request, Locale locale, String username);

    PurchaseRequisitionResponse reject(RejectPurchaseRequisitionRequest request, Locale locale, String username);

    PurchaseRequisitionResponse cancel(CancelPurchaseRequisitionRequest request, Locale locale, String username);

    PurchaseRequisitionResponse close(Long id, Long closedByUserId, String reason, Locale locale, String username);

    // === FULFILLMENT OPERATIONS ===
    PurchaseRequisitionResponse fulfillLine(FulfillPurchaseRequisitionLineRequest request, Locale locale, String username);

    // === PO CONVERSION ===
    PurchaseOrderResponse createPurchaseOrderFromPR(CreatePOFromPurchaseRequisitionRequest request, Locale locale, String username);

    // === UTILITY OPERATIONS ===
    PurchaseRequisitionResponse findByDepartment(Long departmentId, Locale locale, String username);

    PurchaseRequisitionResponse findPendingApprovals(Long departmentId, Locale locale, String username);

    PurchaseRequisitionResponse findApprovedPendingFulfillment(Long organizationId, Locale locale, String username);

    void expireOverdueRequisitions();

    // === EXPORT/IMPORT OPERATIONS ===
    byte[] exportToCsv(List<PurchaseRequisitionDto> items);

    byte[] exportToExcel(List<PurchaseRequisitionDto> items) throws IOException;

    byte[] exportToPdf(List<PurchaseRequisitionDto> items) throws DocumentException;

    ImportSummary importPurchaseRequisitionFromCsv(InputStream csvInputStream) throws IOException;
}
