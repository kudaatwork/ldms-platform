package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.DistrictDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.DistrictMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.DistrictResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface DistrictServiceProcessor {
    DistrictResponse create(CreateDistrictRequest request, Locale locale, String username);
    DistrictResponse update(EditDistrictRequest request, Locale locale, String username);
    DistrictResponse findById(Long id, Locale locale, String username);
    DistrictResponse findAll(Locale locale, String username);
    DistrictResponse delete(Long id, Locale locale, String username);
    DistrictResponse findByMultipleFilters(DistrictMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<DistrictDto> dtoList);
    byte[] exportToExcel(List<DistrictDto> dtoList) throws IOException;
    byte[] exportToPdf(List<DistrictDto> dtoList) throws DocumentException;
    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}