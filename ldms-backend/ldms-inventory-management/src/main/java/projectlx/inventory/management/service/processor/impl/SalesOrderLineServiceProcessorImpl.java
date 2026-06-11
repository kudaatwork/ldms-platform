package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.service.processor.api.SalesOrderLineServiceProcessor;
import projectlx.inventory.management.utils.dtos.SalesOrderLineDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.SalesOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.SalesOrderLineResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SalesOrderLineServiceProcessorImpl implements SalesOrderLineServiceProcessor {

    @Override
    public SalesOrderLineResponse create(CreateSalesOrderLineRequest request, Locale locale, String username) {
        return null;
    }

    @Override
    public SalesOrderLineResponse findById(Long id, Locale locale, String username) {
        return null;
    }

    @Override
    public SalesOrderLineResponse findAllAsList(Locale locale, String username) {
        return null;
    }

    @Override
    public SalesOrderLineResponse update(EditSalesOrderLineRequest request, String username, Locale locale) {
        return null;
    }

    @Override
    public SalesOrderLineResponse delete(Long id, Locale locale, String username) {
        return null;
    }

    @Override
    public SalesOrderLineResponse findByMultipleFilters(SalesOrderLineMultipleFiltersRequest request, String username, Locale locale) {
        return null;
    }

    @Override
    public byte[] exportToCsv(List<SalesOrderLineDto> items) {
        return new byte[0];
    }

    @Override
    public byte[] exportToExcel(List<SalesOrderLineDto> items) throws IOException {
        return new byte[0];
    }

    @Override
    public byte[] exportToPdf(List<SalesOrderLineDto> items) throws DocumentException {
        return new byte[0];
    }

    @Override
    public ImportSummary importSalesOrderLineFromCsv(InputStream csvInputStream) throws IOException {
        return null;
    }
}
