package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.VillageServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.VillageService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.VillageServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.City;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.model.Village;
import projectlx.co.zw.locationsmanagementservice.repository.CityRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.repository.VillageRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.VillageSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.GeoCoordinateCsvImportHelper;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.VillageCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.VillageDto;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.VillageMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.VillageResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class VillageServiceImpl implements VillageService {

    private final VillageServiceValidator villageServiceValidator;
    private final VillageRepository villageRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final SuburbRepository suburbRepository;
    private final VillageServiceAuditable villageServiceAuditable;
    private final LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService;
    private final MessageService messageService;

    private static final String[] EXPORT_HEADERS = {
            "ID", "NAME", "CODE", "CITY ID", "CITY", "DISTRICT ID", "DISTRICT", "PROVINCE", "COUNTRY",
            "SUBURB ID", "LATITUDE", "LONGITUDE", "TIMEZONE", "POSTAL CODE", "CREATED AT", "MODIFIED AT", "ENTITY STATUS"
    };

    @Override
    public VillageResponse create(CreateVillageRequest request, Locale locale, String username) {
        ValidatorDto validatorDto = villageServiceValidator.isCreateVillageRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_VILLAGE_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildVillageResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildVillageResponse(400, false, message, null, null, null);
        }
        Optional<String> hierarchyError = validateHierarchy(request.getCityId(), request.getDistrictId(), request.getSuburbId(), locale);
        if (hierarchyError.isPresent()) {
            return buildVillageResponse(400, false, hierarchyError.get(), null, null, null);
        }
        Optional<City> cityOpt = cityRepository.findByIdAndEntityStatusNot(request.getCityId(), EntityStatus.DELETED);
        City city = cityOpt.get();
        District district = districtRepository.findByIdAndEntityStatusNot(request.getDistrictId(), EntityStatus.DELETED).get();
        String normalizedName = Validators.capitalizeWords(request.getName());
        Optional<Village> activeSame = villageRepository.findByNameAndCityAndEntityStatusNot(normalizedName, city, EntityStatus.DELETED);
        if (activeSame.isPresent()) {
            String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_ALREADY_EXISTS.getCode(), new String[]{}, locale);
            return buildVillageResponse(400, false, message, null, null, null);
        }
        Optional<Village> deletedVillage = villageRepository.findByNameAndCity(normalizedName, city)
                .filter(v -> v.getEntityStatus() == EntityStatus.DELETED);

        Village entity = deletedVillage.orElseGet(Village::new);
        entity.setName(normalizedName);
        entity.setCode(request.getCode());
        entity.setCity(city);
        entity.setDistrict(district);
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setTimezone(request.getTimezone());
        entity.setPostalCode(request.getPostalCode());
        entity.setCreatedBy(username);
        entity.setModifiedBy(username);
        if (request.getSuburbId() != null) {
            entity.setSuburb(suburbRepository.findByIdAndEntityStatusNot(request.getSuburbId(), EntityStatus.DELETED).orElse(null));
        } else {
            entity.setSuburb(null);
        }
        if (deletedVillage.isPresent()) {
            entity.setEntityStatus(EntityStatus.ACTIVE);
        }
        Village saved = deletedVillage.isPresent()
                ? villageServiceAuditable.update(entity, locale, username)
                : villageServiceAuditable.create(entity, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildVillageResponse(201, true, message, toVillageDto(saved), null, null);
    }

    @Override
    public VillageResponse findById(Long id, Locale locale, String username) {
        ValidatorDto validatorDto = villageServiceValidator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildVillageResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Optional<Village> found = villageRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (found.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildVillageResponse(404, false, message, null, null, null);
        }
        String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildVillageResponse(200, true, message, toVillageDto(found.get()), null, null);
    }

    @Override
    public VillageResponse findAllAsList(Locale locale, String username) {
        List<Village> list = villageRepository.findAllByEntityStatusNot(EntityStatus.DELETED);
        if (list.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildVillageResponse(404, false, message, null, null, null);
        }
        List<VillageDto> dtos = list.stream().map(this::toVillageDto).collect(Collectors.toList());
        String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildVillageResponse(200, true, message, null, dtos, null);
    }

    @Override
    public VillageResponse update(EditVillageRequest request, String username, Locale locale) {
        ValidatorDto validatorDto = villageServiceValidator.isRequestValidForEditing(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_UPDATE_VILLAGE_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildVillageResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildVillageResponse(400, false, message, null, null, null);
        }
        Optional<Village> found = villageRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (found.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildVillageResponse(404, false, message, null, null, null);
        }
        Long cityId = request.getCityId() != null ? request.getCityId() : found.get().getCity().getId();
        Long districtId = request.getDistrictId() != null ? request.getDistrictId() : found.get().getDistrict().getId();
        Long suburbId = request.getSuburbId() != null ? request.getSuburbId() : (found.get().getSuburb() != null ? found.get().getSuburb().getId() : null);
        Optional<String> hierarchyError = validateHierarchy(cityId, districtId, suburbId, locale);
        if (hierarchyError.isPresent()) {
            return buildVillageResponse(400, false, hierarchyError.get(), null, null, null);
        }
        Village entity = found.get();
        entity.setName(Validators.capitalizeWords(request.getName()));
        entity.setCode(request.getCode());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setTimezone(request.getTimezone());
        entity.setPostalCode(request.getPostalCode());
        entity.setModifiedBy(username);
        if (request.getCityId() != null) {
            entity.setCity(cityRepository.findByIdAndEntityStatusNot(request.getCityId(), EntityStatus.DELETED).orElse(entity.getCity()));
        }
        if (request.getDistrictId() != null) {
            entity.setDistrict(districtRepository.findByIdAndEntityStatusNot(request.getDistrictId(), EntityStatus.DELETED).orElse(entity.getDistrict()));
        }
        if (request.getSuburbId() != null) {
            entity.setSuburb(suburbRepository.findByIdAndEntityStatusNot(request.getSuburbId(), EntityStatus.DELETED).orElse(null));
        }
        Village saved = villageServiceAuditable.update(entity, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildVillageResponse(200, true, message, toVillageDto(saved), null, null);
    }

    @Override
    @Transactional
    public VillageResponse delete(Long id, Locale locale, String username) {
        ValidatorDto validatorDto = villageServiceValidator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildVillageResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Optional<Village> found = villageRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (found.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildVillageResponse(404, false, message, null, null, null);
        }
        Village entity = found.get();

        locationHierarchyCascadeSoftDeleteService.purgeAddressesLinkedToVillage(entity.getId(), locale);

        entity.setEntityStatus(EntityStatus.DELETED);
        Village saved = villageServiceAuditable.delete(entity, locale);
        String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildVillageResponse(200, true, message, toVillageDto(saved), null, null);
    }

    @Override
    public VillageResponse findByMultipleFilters(VillageMultipleFiltersRequest request, String username, Locale locale) {
        Specification<Village> spec = null;
        spec = addToSpec(spec, VillageSpecification::deleted);
        ValidatorDto validatorDto = villageServiceValidator.isRequestValidToRetrieveVillagesByMultipleFilters(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_UPDATE_VILLAGE_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildVillageResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "id"));
        if (isNotEmpty(request.getName())) {
            spec = addToSpec(request.getName(), spec, VillageSpecification::nameLike);
        }
        if (isNotEmpty(request.getCode())) {
            spec = addToSpec(request.getCode(), spec, VillageSpecification::codeLike);
        }
        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, VillageSpecification::any);
        }
        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, VillageSpecification::hasEntityStatus);
        }
        if (request.getDistrictId() != null) {
            Specification<Village> byDistrict = VillageSpecification.byDistrict(request.getDistrictId());
            spec = spec == null ? byDistrict : spec.and(byDistrict);
        }
        if (request.getCityId() != null) {
            Specification<Village> byCity = VillageSpecification.byCity(request.getCityId());
            spec = spec == null ? byCity : spec.and(byCity);
        }
        Page<Village> result = villageRepository.findAll(spec, pageable);
        if (result.getContent().isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildVillageResponse(404, false, message, null, null, null);
        }
        Page<VillageDto> page = toDtoPage(result);
        String message = messageService.getMessage(I18Code.MESSAGE_VILLAGE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildVillageResponse(200, true, message, null, null, page);
    }

    @Override
    public byte[] exportToCsv(List<VillageDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", EXPORT_HEADERS)).append("\n");
        for (VillageDto v : items) {
            sb.append(v.getId()).append(",")
                    .append(safe(v.getName())).append(",")
                    .append(safe(v.getCode())).append(",")
                    .append(v.getCityId() != null ? v.getCityId() : "").append(",")
                    .append(safe(v.getCityName())).append(",")
                    .append(v.getDistrictId() != null ? v.getDistrictId() : "").append(",")
                    .append(safe(v.getDistrictName())).append(",")
                    .append(safe(v.getProvinceName())).append(",")
                    .append(safe(v.getCountryName())).append(",")
                    .append(v.getSuburbId() != null ? v.getSuburbId() : "").append(",")
                    .append(v.getLatitude() != null ? v.getLatitude() : "").append(",")
                    .append(v.getLongitude() != null ? v.getLongitude() : "").append(",")
                    .append(safe(v.getTimezone())).append(",")
                    .append(safe(v.getPostalCode())).append(",")
                    .append(v.getCreatedAt() != null ? v.getCreatedAt() : "").append(",")
                    .append(v.getUpdatedAt() != null ? v.getUpdatedAt() : "").append(",")
                    .append(v.getEntityStatus() != null ? v.getEntityStatus() : "").append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<VillageDto> items) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Villages");
        Row header = sheet.createRow(0);
        for (int i = 0; i < EXPORT_HEADERS.length; i++) {
            header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
        }
        int rowIdx = 1;
        for (VillageDto v : items) {
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            row.createCell(c++).setCellValue(v.getId() != null ? v.getId() : 0);
            row.createCell(c++).setCellValue(safe(v.getName()));
            row.createCell(c++).setCellValue(safe(v.getCode()));
            row.createCell(c++).setCellValue(v.getCityId() != null ? v.getCityId() : 0);
            row.createCell(c++).setCellValue(safe(v.getCityName()));
            row.createCell(c++).setCellValue(v.getDistrictId() != null ? v.getDistrictId() : 0);
            row.createCell(c++).setCellValue(safe(v.getDistrictName()));
            row.createCell(c++).setCellValue(safe(v.getProvinceName()));
            row.createCell(c++).setCellValue(safe(v.getCountryName()));
            row.createCell(c++).setCellValue(v.getSuburbId() != null ? v.getSuburbId() : 0);
            row.createCell(c++).setCellValue(v.getLatitude() != null ? v.getLatitude().doubleValue() : 0);
            row.createCell(c++).setCellValue(v.getLongitude() != null ? v.getLongitude().doubleValue() : 0);
            row.createCell(c++).setCellValue(safe(v.getTimezone()));
            row.createCell(c++).setCellValue(safe(v.getPostalCode()));
            row.createCell(c++).setCellValue(v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
            row.createCell(c++).setCellValue(v.getUpdatedAt() != null ? v.getUpdatedAt().toString() : "");
            row.createCell(c++).setCellValue(v.getEntityStatus() != null ? v.getEntityStatus().toString() : "");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<VillageDto> items) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("VILLAGE EXPORT", font));
        document.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(EXPORT_HEADERS.length);
        for (String h : EXPORT_HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }
        for (VillageDto v : items) {
            table.addCell(String.valueOf(v.getId()));
            table.addCell(safe(v.getName()));
            table.addCell(safe(v.getCode()));
            table.addCell(v.getCityId() != null ? v.getCityId().toString() : "");
            table.addCell(safe(v.getCityName()));
            table.addCell(v.getDistrictId() != null ? v.getDistrictId().toString() : "");
            table.addCell(safe(v.getDistrictName()));
            table.addCell(safe(v.getProvinceName()));
            table.addCell(safe(v.getCountryName()));
            table.addCell(v.getSuburbId() != null ? v.getSuburbId().toString() : "");
            table.addCell(v.getLatitude() != null ? v.getLatitude().toString() : "");
            table.addCell(v.getLongitude() != null ? v.getLongitude().toString() : "");
            table.addCell(safe(v.getTimezone()));
            table.addCell(safe(v.getPostalCode()));
            table.addCell(v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
            table.addCell(v.getUpdatedAt() != null ? v.getUpdatedAt().toString() : "");
            table.addCell(v.getEntityStatus() != null ? v.getEntityStatus().toString() : "");
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importVillageFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int total = 0;

        try (Reader reader = new InputStreamReader(new BOMInputStream(csvInputStream), StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<VillageCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(VillageCsvDto.class);

            CsvToBean<VillageCsvDto> csvToBean = new CsvToBeanBuilder<VillageCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<VillageCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (VillageCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getName() == null || row.getName().isBlank()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing village name");
                        continue;
                    }
                    if (row.getCityId() == null) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing city ID");
                        continue;
                    }
                    if (row.getDistrictId() == null) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing district ID");
                        continue;
                    }

                    Long suburbIdForValidation = suburbIdOrNull(row.getSuburbId());
                    Optional<String> hierarchyError =
                            validateHierarchy(row.getCityId(), row.getDistrictId(), suburbIdForValidation, Locale.ENGLISH);
                    if (hierarchyError.isPresent()) {
                        failed++;
                        errors.add("Row " + rowNum + ": " + hierarchyError.get());
                        continue;
                    }

                    GeoCoordinates parentGeo = parentGeoForVillageCsv(row.getCityId(), row.getDistrictId());
                    GeoCoordinateCsvImportHelper.ResolvedGeo geo = GeoCoordinateCsvImportHelper.resolve(
                            null,
                            row.getLatitude(),
                            row.getLongitude(),
                            parentGeo);

                    CreateVillageRequest request = new CreateVillageRequest();
                    request.setName(row.getName().trim());
                    request.setCode(blankToNull(row.getCode()));
                    request.setCityId(row.getCityId());
                    request.setDistrictId(row.getDistrictId());
                    request.setSuburbId(suburbIdForValidation);
                    request.setLatitude(geo.latitude());
                    request.setLongitude(geo.longitude());
                    request.setTimezone(blankToNull(row.getTimezone()));
                    request.setPostalCode(blankToNull(row.getPostalCode()));

                    VillageResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        if (response.getErrorMessages() != null && !response.getErrorMessages().isEmpty()) {
                            errors.add("Row " + rowNum + ": " + String.join("; ", response.getErrorMessages()));
                        } else {
                            errors.add("Row " + rowNum + ": " + response.getMessage());
                        }
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + rowNum + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " villages imported."
                : "Import failed. No villages were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    /**
     * In-memory point for {@link GeoCoordinateCsvImportHelper}; not persisted. Uses city lat/long, else district geo.
     */
    private GeoCoordinates parentGeoForVillageCsv(Long cityId, Long districtId) {
        Optional<City> cityOpt = cityRepository.findByIdAndEntityStatusNot(cityId, EntityStatus.DELETED);
        if (cityOpt.isEmpty()) {
            return null;
        }
        City city = cityOpt.get();
        if (city.getLatitude() != null && city.getLongitude() != null) {
            GeoCoordinates synthetic = new GeoCoordinates();
            synthetic.setLatitude(city.getLatitude());
            synthetic.setLongitude(city.getLongitude());
            return synthetic;
        }
        return districtRepository.findByIdFetchingGeoCoordinates(districtId, EntityStatus.DELETED)
                .map(District::getGeoCoordinates)
                .orElse(null);
    }

    private static Long suburbIdOrNull(Long suburbId) {
        if (suburbId == null || suburbId <= 0) {
            return null;
        }
        return suburbId;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private Optional<String> validateHierarchy(Long cityId, Long districtId, Long suburbId, Locale locale) {
        Optional<City> cityOpt = cityRepository.findByIdAndEntityStatusNot(cityId, EntityStatus.DELETED);
        if (cityOpt.isEmpty()) {
            return Optional.of(messageService.getMessage(I18Code.MESSAGE_CITY_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        City city = cityOpt.get();
        if (!city.getDistrict().getId().equals(districtId)) {
            return Optional.of(messageService.getMessage(I18Code.MESSAGE_VILLAGE_CITY_DISTRICT_MISMATCH.getCode(), new String[]{}, locale));
        }
        if (districtRepository.findByIdAndEntityStatusNot(districtId, EntityStatus.DELETED).isEmpty()) {
            return Optional.of(messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (suburbId != null) {
            Optional<Suburb> suburbOpt = suburbRepository.findByIdAndEntityStatusNot(suburbId, EntityStatus.DELETED);
            if (suburbOpt.isEmpty()) {
                return Optional.of(messageService.getMessage(I18Code.MESSAGE_SUBURB_NOT_FOUND.getCode(), new String[]{}, locale));
            }
            if (!suburbOpt.get().getDistrict().getId().equals(districtId)) {
                return Optional.of(messageService.getMessage(I18Code.MESSAGE_VILLAGE_SUBURB_DISTRICT_MISMATCH.getCode(), new String[]{}, locale));
            }
        }
        return Optional.empty();
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<Village> addToSpec(Specification<Village> spec, Function<EntityStatus, Specification<Village>> predicateMethod) {
        Specification<Village> local = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        return spec == null ? local : spec.and(local);
    }

    private Specification<Village> addToSpec(String value, Specification<Village> spec, Function<String, Specification<Village>> fn) {
        if (value != null && !value.isEmpty()) {
            Specification<Village> local = Specification.where(fn.apply(value));
            return spec == null ? local : spec.and(local);
        }
        return spec;
    }

    private Specification<Village> addToSpec(EntityStatus status, Specification<Village> spec, Function<EntityStatus, Specification<Village>> fn) {
        if (status != null) {
            Specification<Village> local = Specification.where(fn.apply(status));
            return spec == null ? local : spec.and(local);
        }
        return spec;
    }

    private Page<VillageDto> toDtoPage(Page<Village> page) {
        List<VillageDto> content = page.getContent().stream().map(this::toVillageDto).collect(Collectors.toList());
        Pageable p = PageRequest.of(page.getNumber(), page.getSize() <= 0 ? 10 : page.getSize());
        return new PageImpl<>(content, p, page.getTotalElements());
    }

    private VillageDto toVillageDto(Village entity) {
        VillageDto dto = new VillageDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setTimezone(entity.getTimezone());
        dto.setPostalCode(entity.getPostalCode());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getModifiedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setEntityStatus(entity.getEntityStatus());
        if (entity.getCity() != null) {
            dto.setCityId(entity.getCity().getId());
            dto.setCityName(entity.getCity().getName());
        }
        if (entity.getDistrict() != null) {
            dto.setDistrictId(entity.getDistrict().getId());
            dto.setDistrictName(entity.getDistrict().getName());
            if (entity.getDistrict().getProvince() != null) {
                dto.setProvinceId(entity.getDistrict().getProvince().getId());
                dto.setProvinceName(entity.getDistrict().getProvince().getName());
                if (entity.getDistrict().getProvince().getCountry() != null) {
                    dto.setCountryId(entity.getDistrict().getProvince().getCountry().getId());
                    dto.setCountryName(entity.getDistrict().getProvince().getCountry().getName());
                }
            }
        }
        if (entity.getSuburb() != null) {
            dto.setSuburbId(entity.getSuburb().getId());
            dto.setSuburbName(entity.getSuburb().getName());
        }
        return dto;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private VillageResponse buildVillageResponse(int statusCode, boolean success, String message,
                                               VillageDto dto, List<VillageDto> list, Page<VillageDto> page) {
        VillageResponse r = new VillageResponse();
        r.setStatusCode(statusCode);
        r.setSuccess(success);
        r.setMessage(message);
        r.setVillageDto(dto);
        r.setVillageDtoList(list);
        r.setVillageDtoPage(page);
        return r;
    }

    private VillageResponse buildVillageResponseWithErrors(int statusCode, boolean success, String message,
                                                           VillageDto dto, List<VillageDto> list, Page<VillageDto> page, List<String> errors) {
        VillageResponse r = buildVillageResponse(statusCode, success, message, dto, list, page);
        r.setErrorMessages(errors);
        return r;
    }
}
