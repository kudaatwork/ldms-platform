package projectlx.co.zw.locationsmanagementservice.business.logic.api;

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

public interface VillageService {

    VillageResponse create(CreateVillageRequest request, Locale locale, String username);

    VillageResponse findById(Long id, Locale locale, String username);

    VillageResponse findAllAsList(Locale locale, String username);

    VillageResponse update(EditVillageRequest request, String username, Locale locale);

    VillageResponse delete(Long id, Locale locale, String username);

    VillageResponse findByMultipleFilters(VillageMultipleFiltersRequest request, String username, Locale locale);

    byte[] exportToCsv(List<VillageDto> items);

    byte[] exportToExcel(List<VillageDto> items) throws IOException;

    byte[] exportToPdf(List<VillageDto> items) throws DocumentException;

    ImportSummary importVillageFromCsv(InputStream csvInputStream) throws IOException;
}
