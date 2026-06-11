package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.InventoryTransferDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.InventoryTransferMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.EditInventoryTransferRequest;
import projectlx.inventory.management.utils.responses.InventoryTransferResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface InventoryTransferService {
    InventoryTransferResponse create(CreateInventoryTransferRequest request, Locale locale, String username);
    InventoryTransferResponse approveTransfer(Long transferId, Long approvedByUserId, Locale locale, String username);
    InventoryTransferResponse rejectTransfer(Long transferId, Long rejectedByUserId, String rejectionReason,
                                             Locale locale, String username);
    InventoryTransferResponse startTransit(Long transferId, Long startedByUserId, Locale locale, String username);
    InventoryTransferResponse completeTransfer(Long transferId, Long updatedByUserId, Locale locale, String username,
                                               String idempotencyKey);

    /**
     * Completes an IN_TRANSIT inventory transfer and auto-creates a GRV at the destination warehouse.
     * Used by the shipment/trip flow where no Purchase Order is involved.
     * Idempotent: repeated calls with the same key return the existing result.
     */
    InventoryTransferResponse completeTransferWithGrv(Long transferId, Long receivedByUserId,
                                                      String idempotencyKey, Locale locale, String username);
    InventoryTransferResponse findById(Long id, Locale locale, String username);
    InventoryTransferResponse findAllAsList(Locale locale, String username);
    InventoryTransferResponse update(EditInventoryTransferRequest request, String username, Locale locale);
    InventoryTransferResponse cancel(Long id, Locale locale, String username);
    InventoryTransferResponse findByMultipleFilters(InventoryTransferMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<InventoryTransferDto> items);
    byte[] exportToExcel(List<InventoryTransferDto> items) throws IOException;
    byte[] exportToPdf(List<InventoryTransferDto> items) throws DocumentException;
    ImportSummary importInventoryTransferFromCsv(InputStream csvInputStream) throws IOException;
}
