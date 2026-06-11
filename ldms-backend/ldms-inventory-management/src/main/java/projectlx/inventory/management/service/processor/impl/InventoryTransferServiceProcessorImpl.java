package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.InventoryTransferService;
import projectlx.inventory.management.service.processor.api.InventoryTransferServiceProcessor;
import projectlx.inventory.management.utils.dtos.InventoryTransferDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.EditInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.InventoryTransferMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.InventoryTransferResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class InventoryTransferServiceProcessorImpl implements InventoryTransferServiceProcessor {

    private final InventoryTransferService inventoryTransferService;
    private static final Logger logger = LoggerFactory.getLogger(InventoryTransferServiceProcessorImpl.class);

    @Override
    public InventoryTransferResponse create(CreateInventoryTransferRequest request, Locale locale, String username) {

        logger.info("Incoming request to create inventory transfer for user: {}", username);

        InventoryTransferResponse response = inventoryTransferService.create(request, locale, username);

        logger.info("Outgoing response after creating inventory transfer: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse approveTransfer(Long transferId, Long approvedByUserId, Locale locale, String username) {

        logger.info("Incoming request to approve inventory transfer id: {} by user: {}", transferId, username);

        InventoryTransferResponse response = inventoryTransferService.approveTransfer(transferId, approvedByUserId, locale, username);

        logger.info("Outgoing response after approving inventory transfer: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse rejectTransfer(Long transferId, Long rejectedByUserId, String rejectionReason,
                                                    Locale locale, String username) {

        logger.info("Incoming request to reject inventory transfer id: {} by user: {}", transferId, username);

        InventoryTransferResponse response = inventoryTransferService.rejectTransfer(
                transferId, rejectedByUserId, rejectionReason, locale, username);

        logger.info("Outgoing response after rejecting inventory transfer: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse startTransit(Long transferId, Long startedByUserId, Locale locale, String username) {

        logger.info("Incoming request to start transit for inventory transfer id: {} by user: {}", transferId, username);

        InventoryTransferResponse response = inventoryTransferService.startTransit(transferId, startedByUserId, locale, username);

        logger.info("Outgoing response after starting transit for inventory transfer: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse completeTransfer(Long transferId, Long updatedByUserId, String idempotencyKey,
                                                      Locale locale, String username) {

        logger.info("Incoming request to complete inventory transfer id: {} by user: {}", transferId, username);

        InventoryTransferResponse response = inventoryTransferService.completeTransfer(transferId, updatedByUserId, locale,
                username, idempotencyKey);

        logger.info("Outgoing response after completing inventory transfer: Success: {}", response != null && response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find inventory transfer by ID: {} for user: {}", id, username);

        InventoryTransferResponse response = inventoryTransferService.findById(id, locale, username);

        logger.info("Outgoing response after finding inventory transfer by ID: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse findAllAsList(Locale locale, String username) {

        logger.info("Incoming request to find all inventory transfers as list for user: {}", username);

        InventoryTransferResponse response = inventoryTransferService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all inventory transfers: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse update(EditInventoryTransferRequest request, String username, Locale locale) {

        logger.info("Incoming request to update inventory transfer for user: {}", username);

        InventoryTransferResponse response = inventoryTransferService.update(request, username, locale);

        logger.info("Outgoing response after updating inventory transfer: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse cancel(Long id, Locale locale, String username) {

        logger.info("Incoming request to cancel inventory transfer by ID: {} for user: {}", id, username);

        InventoryTransferResponse response = inventoryTransferService.cancel(id, locale, username);

        logger.info("Outgoing response after cancelling inventory transfer: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public InventoryTransferResponse findByMultipleFilters(InventoryTransferMultipleFiltersRequest request,
                                                           String username, Locale locale) {

        logger.info("Incoming request to find inventory transfers by multiple filters for user: {}", username);

        InventoryTransferResponse response = inventoryTransferService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding inventory transfers by filters: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public byte[] exportToCsv(List<InventoryTransferDto> items) {

        logger.info("Incoming request to export {} inventory transfers to CSV", items != null ? items.size() : 0);

        byte[] result = inventoryTransferService.exportToCsv(items);

        logger.info("Outgoing response after exporting inventory transfers to CSV: Size: {} bytes", result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<InventoryTransferDto> items) throws IOException {

        logger.info("Incoming request to export {} inventory transfers to Excel", items != null ? items.size() : 0);

        byte[] result = inventoryTransferService.exportToExcel(items);

        logger.info("Outgoing response after exporting inventory transfers to Excel: Size: {} bytes", result != null ? result.length : 0);
        return result;
    }

    @Override
    public byte[] exportToPdf(List<InventoryTransferDto> items) throws DocumentException {

        logger.info("Incoming request to export {} inventory transfers to PDF", items != null ? items.size() : 0);

        byte[] result = inventoryTransferService.exportToPdf(items);

        logger.info("Outgoing response after exporting inventory transfers to PDF: Size: {} bytes", result != null ? result.length : 0);

        return result;
    }

    @Override
    public InventoryTransferResponse completeTransferWithGrv(Long transferId, Long receivedByUserId,
                                                             String idempotencyKey, Locale locale, String username) {

        logger.info("Incoming request to complete transfer with GRV for transfer id: {} by user: {}", transferId, username);

        InventoryTransferResponse response = inventoryTransferService.completeTransferWithGrv(
                transferId, receivedByUserId, idempotencyKey, locale, username);

        logger.info("Outgoing response after completing transfer with GRV: Success: {}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public ImportSummary importInventoryTransferFromCsv(InputStream csvInputStream) throws IOException {

        logger.info("Incoming request to import inventory transfers from CSV");

        ImportSummary result = inventoryTransferService.importInventoryTransferFromCsv(csvInputStream);

        logger.info("Outgoing response after importing inventory transfers from CSV: Success: {}", result != null);

        return result;
    }
}
