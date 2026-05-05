package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.VillageDto;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.VillageMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.VillageResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface VillageServiceProcessor {

    VillageResponse create(CreateVillageRequest request, Locale locale, String username);

    VillageResponse update(EditVillageRequest request, Locale locale, String username);

    VillageResponse findById(Long id, Locale locale, String username);

    VillageResponse findAll(Locale locale, String username);

    VillageResponse delete(Long id, Locale locale, String username);

    VillageResponse findByMultipleFilters(VillageMultipleFiltersRequest request, String username, Locale locale);

    byte[] exportToCsv(List<VillageDto> dtoList);

    byte[] exportToExcel(List<VillageDto> dtoList) throws IOException;

    byte[] exportToPdf(List<VillageDto> dtoList) throws DocumentException;

    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}
