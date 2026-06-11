package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.SalesReservationService;
import projectlx.inventory.management.service.processor.api.SalesReservationServiceProcessor;
import projectlx.inventory.management.utils.dtos.SalesReservationDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateSalesReservationRequest;
import projectlx.inventory.management.utils.requests.EditSalesReservationRequest;
import projectlx.inventory.management.utils.requests.SalesReservationMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.SalesReservationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SalesReservationServiceProcessorImpl implements SalesReservationServiceProcessor {

    private final SalesReservationService salesReservationService;
    private static final Logger logger = LoggerFactory.getLogger(SalesReservationServiceProcessorImpl.class);

    @Override
    public SalesReservationResponse create(CreateSalesReservationRequest request, Locale locale, String username) {
        logger.info("Incoming request to create sales reservation for user: {}", username);

        SalesReservationResponse response = salesReservationService.create(request, locale, username);

        logger.info("Outgoing response after creating sales reservation: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesReservationResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find sales reservation by ID: {} for user: {}", id, username);

        SalesReservationResponse response = salesReservationService.findById(id, locale, username);

        logger.info("Outgoing response after finding sales reservation by ID: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesReservationResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all sales reservations as list for user: {}", username);

        SalesReservationResponse response = salesReservationService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all sales reservations: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesReservationResponse update(EditSalesReservationRequest request, String username, Locale locale) {
        logger.info("Incoming request to update sales reservation for user: {}", username);

        SalesReservationResponse response = salesReservationService.update(request, username, locale);

        logger.info("Outgoing response after updating sales reservation: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesReservationResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete sales reservation by ID: {} for user: {}", id, username);

        SalesReservationResponse response = salesReservationService.delete(id, locale, username);

        logger.info("Outgoing response after deleting sales reservation: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesReservationResponse findByMultipleFilters(SalesReservationMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find sales reservations by multiple filters for user: {}", username);

        SalesReservationResponse response = salesReservationService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding sales reservations by filters: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public byte[] exportToCsv(List<SalesReservationDto> items) {
        logger.info("Incoming request to export {} sales reservations to CSV",
                items != null ? items.size() : 0);

        byte[] result = salesReservationService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<SalesReservationDto> items) throws IOException {
        logger.info("Incoming request to export {} sales reservations to Excel",
                items != null ? items.size() : 0);

        byte[] result = salesReservationService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<SalesReservationDto> items) throws DocumentException {
        logger.info("Incoming request to export {} sales reservations to PDF",
                items != null ? items.size() : 0);

        byte[] result = salesReservationService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importSalesReservationFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import sales reservations from CSV");

        ImportSummary result = salesReservationService.importSalesReservationFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: Success: {}",
                result != null);

        return result;
    }
}
