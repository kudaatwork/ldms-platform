package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.SuburbDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.SuburbMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.SuburbResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface SuburbService {
    SuburbResponse create(CreateSuburbRequest request, Locale locale, String username);
    SuburbResponse findById(Long id, Locale locale, String username);
    SuburbResponse findAllAsList(Locale locale, String username);
    SuburbResponse update(EditSuburbRequest request, String username, Locale locale);
    SuburbResponse delete(Long id, Locale locale, String username);
    SuburbResponse findByMultipleFilters(SuburbMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<SuburbDto> items);
    byte[] exportToExcel(List<SuburbDto> items) throws IOException;
    byte[] exportToPdf(List<SuburbDto> items) throws DocumentException;
    ImportSummary importSuburbFromCsv(InputStream csvInputStream) throws IOException;
}