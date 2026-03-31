package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ProvinceDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.ProvinceMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.ProvinceResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface ProvinceService {
    ProvinceResponse create(CreateProvinceRequest request, Locale locale, String username);
    ProvinceResponse findById(Long id, Locale locale, String username);
    ProvinceResponse findAllAsList(Locale locale, String username);
    ProvinceResponse update(EditProvinceRequest request, String username, Locale locale);
    ProvinceResponse delete(Long id, Locale locale, String username);
    ProvinceResponse findByMultipleFilters(ProvinceMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<ProvinceDto> items);
    byte[] exportToExcel(List<ProvinceDto> items) throws IOException;
    byte[] exportToPdf(List<ProvinceDto> items) throws DocumentException;
    ImportSummary importProvinceFromCsv(InputStream csvInputStream) throws IOException;
}