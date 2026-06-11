package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.model.PurchaseReturn;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.PurchaseReturnDto;
import projectlx.inventory.management.utils.requests.CreatePurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.PurchaseReturnMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.PurchaseReturnResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface PurchaseReturnService {

    PurchaseReturnResponse create(CreatePurchaseReturnRequest request, Locale locale, String username);
    PurchaseReturnResponse findById(Long id, Locale locale, String username);
    Optional<PurchaseReturn> findById(Long id);
    PurchaseReturnResponse findAllAsList(Locale locale, String username);
    PurchaseReturnResponse update(EditPurchaseReturnRequest request, String username, Locale locale);
    PurchaseReturnResponse delete(Long id, Locale locale, String username);
    PurchaseReturnResponse findByMultipleFilters(PurchaseReturnMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<PurchaseReturnDto> items);
    byte[] exportToExcel(List<PurchaseReturnDto> items) throws IOException;
    byte[] exportToPdf(List<PurchaseReturnDto> items) throws DocumentException;
    ImportSummary importPurchaseReturnFromCsv(InputStream csvInputStream) throws IOException;
}