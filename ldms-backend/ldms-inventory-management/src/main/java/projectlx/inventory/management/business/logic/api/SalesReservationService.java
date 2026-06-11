package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.SalesReservationDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.SalesReservationMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateSalesReservationRequest;
import projectlx.inventory.management.utils.requests.EditSalesReservationRequest;
import projectlx.inventory.management.utils.responses.SalesReservationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface SalesReservationService {
    SalesReservationResponse create(CreateSalesReservationRequest request, Locale locale, String username);
    SalesReservationResponse findById(Long id, Locale locale, String username);
    SalesReservationResponse findAllAsList(Locale locale, String username);
    SalesReservationResponse update(EditSalesReservationRequest request, String username, Locale locale);
    SalesReservationResponse delete(Long id, Locale locale, String username);
    SalesReservationResponse findByMultipleFilters(SalesReservationMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<SalesReservationDto> items);
    byte[] exportToExcel(List<SalesReservationDto> items) throws IOException;
    byte[] exportToPdf(List<SalesReservationDto> items) throws DocumentException;
    ImportSummary importSalesReservationFromCsv(InputStream csvInputStream) throws IOException;
}
