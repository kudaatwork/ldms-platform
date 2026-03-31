package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ProvinceDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.ProvinceMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.ProvinceResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface ProvinceServiceProcessor {
    ProvinceResponse create(CreateProvinceRequest request, Locale locale, String username);
    ProvinceResponse update(EditProvinceRequest request, Locale locale, String username);
    ProvinceResponse findById(Long id, Locale locale, String username);
    ProvinceResponse findAll(Locale locale, String username);
    ProvinceResponse delete(Long id, Locale locale, String username);
    ProvinceResponse findByMultipleFilters(ProvinceMultipleFiltersRequest request, String username, Locale locale);

    // --- Corrected Signatures ---
    byte[] exportToCsv(ProvinceMultipleFiltersRequest request, Locale locale, String username);
    byte[] exportToExcel(ProvinceMultipleFiltersRequest request, Locale locale, String username) throws IOException;
    byte[] exportToPdf(ProvinceMultipleFiltersRequest request, Locale locale, String username) throws DocumentException;

    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}