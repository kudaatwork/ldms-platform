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
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.CityServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.CityService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.CityServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.City;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.utils.GeoCoordinateCsvImportHelper;
import projectlx.co.zw.locationsmanagementservice.repository.CityRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.CitySpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CityCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CityDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CityMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CityResponse;
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
public class CityServiceImpl implements CityService {

    private final CityServiceValidator cityServiceValidator;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final CityServiceAuditable cityServiceAuditable;
    private final LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService;
    private final MessageService messageService;

    private static final String[] EXPORT_HEADERS = {
            "ID", "NAME", "CODE", "DISTRICT ID", "DISTRICT", "PROVINCE", "COUNTRY",
            "LATITUDE", "LONGITUDE", "TIMEZONE", "POSTAL CODE", "CREATED AT", "MODIFIED AT", "ENTITY STATUS"
    };

    @Override
    public CityResponse create(CreateCityRequest request, Locale locale, String username) {
        ValidatorDto validatorDto = cityServiceValidator.isCreateCityRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_CITY_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildCityResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildCityResponse(400, false, message, null, null, null);
        }
        Optional<District> districtOpt = districtRepository.findByIdAndEntityStatusNot(request.getDistrictId(), EntityStatus.DELETED);
        if (districtOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildCityResponse(400, false, message, null, null, null);
        }
        District district = districtOpt.get();
        String normalizedName = Validators.capitalizeWords(request.getName());
        Optional<City> activeSameName = cityRepository.findByNameAndDistrictAndEntityStatusNot(normalizedName, district, EntityStatus.DELETED);
        if (activeSameName.isPresent()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CITY_ALREADY_EXISTS.getCode(), new String[]{}, locale);
            return buildCityResponse(400, false, message, null, null, null);
        }
        Optional<City> deletedCity = cityRepository.findByNameAndDistrict(normalizedName, district)
                .filter(c -> c.getEntityStatus() == EntityStatus.DELETED);

        City entity = deletedCity.orElseGet(City::new);
        entity.setName(normalizedName);
        entity.setCode(request.getCode());
        entity.setDistrict(district);
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setTimezone(request.getTimezone());
        entity.setPostalCode(request.getPostalCode());
        entity.setCreatedBy(username);
        entity.setModifiedBy(username);
        if (deletedCity.isPresent()) {
            entity.setEntityStatus(EntityStatus.ACTIVE);
        }
        City saved = deletedCity.isPresent()
                ? cityServiceAuditable.update(entity, locale, username)
                : cityServiceAuditable.create(entity, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_CITY_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildCityResponse(201, true, message, toCityDto(saved), null, null);
    }

    @Override
    public CityResponse findById(Long id, Locale locale, String username) {
        ValidatorDto validatorDto = cityServiceValidator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildCityResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Optional<City> found = cityRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (found.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CITY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildCityResponse(404, false, message, null, null, null);
        }
        String message = messageService.getMessage(I18Code.MESSAGE_CITY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildCityResponse(200, true, message, toCityDto(found.get()), null, null);
    }

    @Override
    public CityResponse findAllAsList(Locale locale, String username) {
        List<City> list = cityRepository.findAllByEntityStatusNot(EntityStatus.DELETED);
        if (list.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CITY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildCityResponse(404, false, message, null, null, null);
        }
        List<CityDto> dtos = list.stream().map(this::toCityDto).collect(Collectors.toList());
        String message = messageService.getMessage(I18Code.MESSAGE_CITY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildCityResponse(200, true, message, null, dtos, null);
    }

    @Override
    public CityResponse update(EditCityRequest request, String username, Locale locale) {
        ValidatorDto validatorDto = cityServiceValidator.isRequestValidForEditing(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_UPDATE_CITY_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildCityResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildCityResponse(400, false, message, null, null, null);
        }
        Optional<City> found = cityRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (found.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CITY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildCityResponse(404, false, message, null, null, null);
        }
        City entity = found.get();
        entity.setName(Validators.capitalizeWords(request.getName()));
        entity.setCode(request.getCode());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setTimezone(request.getTimezone());
        entity.setPostalCode(request.getPostalCode());
        entity.setModifiedBy(username);
        if (request.getDistrictId() != null) {
            Optional<District> d = districtRepository.findByIdAndEntityStatusNot(request.getDistrictId(), EntityStatus.DELETED);
            if (d.isPresent()) {
                entity.setDistrict(d.get());
            }
        }
        City saved = cityServiceAuditable.update(entity, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_CITY_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildCityResponse(200, true, message, toCityDto(saved), null, null);
    }

    @Override
    @Transactional
    public CityResponse delete(Long id, Locale locale, String username) {
        ValidatorDto validatorDto = cityServiceValidator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildCityResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Optional<City> found = cityRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (found.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CITY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildCityResponse(404, false, message, null, null, null);
        }
        City entity = found.get();

        locationHierarchyCascadeSoftDeleteService.cascadeBeforeDeletingCity(entity.getId(), locale);

        entity.setEntityStatus(EntityStatus.DELETED);
        City saved = cityServiceAuditable.delete(entity, locale);
        String message = messageService.getMessage(I18Code.MESSAGE_CITY_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildCityResponse(200, true, message, toCityDto(saved), null, null);
    }

    @Override
    public CityResponse findByMultipleFilters(CityMultipleFiltersRequest request, String username, Locale locale) {
        Specification<City> spec = null;
        spec = addToSpec(spec, CitySpecification::deleted);
        ValidatorDto validatorDto = cityServiceValidator.isRequestValidToRetrieveCitiesByMultipleFilters(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_UPDATE_CITY_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildCityResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "id"));
        if (isNotEmpty(request.getName())) {
            spec = addToSpec(request.getName(), spec, CitySpecification::nameLike);
        }
        if (isNotEmpty(request.getCode())) {
            spec = addToSpec(request.getCode(), spec, CitySpecification::codeLike);
        }
        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, CitySpecification::any);
        }
        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, CitySpecification::hasEntityStatus);
        }
        if (request.getDistrictId() != null) {
            Specification<City> byDistrict = CitySpecification.byDistrict(request.getDistrictId());
            spec = spec == null ? byDistrict : spec.and(byDistrict);
        }
        Page<City> result = cityRepository.findAll(spec, pageable);
        if (result.getContent().isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CITY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildCityResponse(404, false, message, null, null, null);
        }
        Page<CityDto> page = toDtoPage(result);
        String message = messageService.getMessage(I18Code.MESSAGE_CITY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildCityResponse(200, true, message, null, null, page);
    }

    @Override
    public byte[] exportToCsv(List<CityDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", EXPORT_HEADERS)).append("\n");
        for (CityDto c : items) {
            sb.append(c.getId()).append(",")
                    .append(safe(c.getName())).append(",")
                    .append(safe(c.getCode())).append(",")
                    .append(c.getDistrictId() != null ? c.getDistrictId() : "").append(",")
                    .append(safe(c.getDistrictName())).append(",")
                    .append(safe(c.getProvinceName())).append(",")
                    .append(safe(c.getCountryName())).append(",")
                    .append(c.getLatitude() != null ? c.getLatitude() : "").append(",")
                    .append(c.getLongitude() != null ? c.getLongitude() : "").append(",")
                    .append(safe(c.getTimezone())).append(",")
                    .append(safe(c.getPostalCode())).append(",")
                    .append(c.getCreatedAt() != null ? c.getCreatedAt() : "").append(",")
                    .append(c.getUpdatedAt() != null ? c.getUpdatedAt() : "").append(",")
                    .append(c.getEntityStatus() != null ? c.getEntityStatus() : "").append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<CityDto> items) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Cities");
        Row header = sheet.createRow(0);
        for (int i = 0; i < EXPORT_HEADERS.length; i++) {
            header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
        }
        int rowIdx = 1;
        for (CityDto c : items) {
            Row row = sheet.createRow(rowIdx++);
            int col = 0;
            row.createCell(col++).setCellValue(c.getId() != null ? c.getId() : 0);
            row.createCell(col++).setCellValue(safe(c.getName()));
            row.createCell(col++).setCellValue(safe(c.getCode()));
            row.createCell(col++).setCellValue(c.getDistrictId() != null ? c.getDistrictId() : 0);
            row.createCell(col++).setCellValue(safe(c.getDistrictName()));
            row.createCell(col++).setCellValue(safe(c.getProvinceName()));
            row.createCell(col++).setCellValue(safe(c.getCountryName()));
            row.createCell(col++).setCellValue(c.getLatitude() != null ? c.getLatitude().doubleValue() : 0);
            row.createCell(col++).setCellValue(c.getLongitude() != null ? c.getLongitude().doubleValue() : 0);
            row.createCell(col++).setCellValue(safe(c.getTimezone()));
            row.createCell(col++).setCellValue(safe(c.getPostalCode()));
            row.createCell(col++).setCellValue(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            row.createCell(col++).setCellValue(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
            row.createCell(col++).setCellValue(c.getEntityStatus() != null ? c.getEntityStatus().toString() : "");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<CityDto> items) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("CITY EXPORT", font));
        document.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(EXPORT_HEADERS.length);
        for (String h : EXPORT_HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }
        for (CityDto c : items) {
            table.addCell(String.valueOf(c.getId()));
            table.addCell(safe(c.getName()));
            table.addCell(safe(c.getCode()));
            table.addCell(c.getDistrictId() != null ? c.getDistrictId().toString() : "");
            table.addCell(safe(c.getDistrictName()));
            table.addCell(safe(c.getProvinceName()));
            table.addCell(safe(c.getCountryName()));
            table.addCell(c.getLatitude() != null ? c.getLatitude().toString() : "");
            table.addCell(c.getLongitude() != null ? c.getLongitude().toString() : "");
            table.addCell(safe(c.getTimezone()));
            table.addCell(safe(c.getPostalCode()));
            table.addCell(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            table.addCell(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
            table.addCell(c.getEntityStatus() != null ? c.getEntityStatus().toString() : "");
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importCityFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int total = 0;

        try (Reader reader = new InputStreamReader(new BOMInputStream(csvInputStream), StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<CityCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(CityCsvDto.class);

            CsvToBean<CityCsvDto> csvToBean = new CsvToBeanBuilder<CityCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<CityCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (CityCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getName() == null || row.getName().isBlank()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing city name");
                        continue;
                    }
                    if (row.getDistrictId() == null) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing district ID");
                        continue;
                    }

                    Optional<District> districtOpt =
                            districtRepository.findByIdFetchingGeoCoordinates(row.getDistrictId(), EntityStatus.DELETED);
                    if (districtOpt.isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": District not found for ID " + row.getDistrictId());
                        continue;
                    }

                    GeoCoordinateCsvImportHelper.ResolvedGeo geo = GeoCoordinateCsvImportHelper.resolve(
                            null,
                            row.getLatitude(),
                            row.getLongitude(),
                            districtOpt.get().getGeoCoordinates());

                    CreateCityRequest request = new CreateCityRequest();
                    request.setName(row.getName().trim());
                    request.setCode(blankToNull(row.getCode()));
                    request.setDistrictId(row.getDistrictId());
                    request.setLatitude(geo.latitude());
                    request.setLongitude(geo.longitude());
                    request.setTimezone(blankToNull(row.getTimezone()));
                    request.setPostalCode(blankToNull(row.getPostalCode()));

                    CityResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
                ? "Import completed successfully. " + success + " out of " + total + " cities imported."
                : "Import failed. No cities were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<City> addToSpec(Specification<City> spec, Function<EntityStatus, Specification<City>> predicateMethod) {
        Specification<City> local = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        return spec == null ? local : spec.and(local);
    }

    private Specification<City> addToSpec(String value, Specification<City> spec, Function<String, Specification<City>> fn) {
        if (value != null && !value.isEmpty()) {
            Specification<City> local = Specification.where(fn.apply(value));
            return spec == null ? local : spec.and(local);
        }
        return spec;
    }

    private Specification<City> addToSpec(EntityStatus status, Specification<City> spec, Function<EntityStatus, Specification<City>> fn) {
        if (status != null) {
            Specification<City> local = Specification.where(fn.apply(status));
            return spec == null ? local : spec.and(local);
        }
        return spec;
    }

    private Page<CityDto> toDtoPage(Page<City> page) {
        List<CityDto> content = page.getContent().stream().map(this::toCityDto).collect(Collectors.toList());
        Pageable p = PageRequest.of(page.getNumber(), page.getSize() <= 0 ? 10 : page.getSize());
        return new PageImpl<>(content, p, page.getTotalElements());
    }

    private CityDto toCityDto(City entity) {
        CityDto dto = new CityDto();
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
        return dto;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private CityResponse buildCityResponse(int statusCode, boolean success, String message,
                                           CityDto dto, List<CityDto> list, Page<CityDto> page) {
        CityResponse r = new CityResponse();
        r.setStatusCode(statusCode);
        r.setSuccess(success);
        r.setMessage(message);
        r.setCityDto(dto);
        r.setCityDtoList(list);
        r.setCityDtoPage(page);
        return r;
    }

    private CityResponse buildCityResponseWithErrors(int statusCode, boolean success, String message,
                                                     CityDto dto, List<CityDto> list, Page<CityDto> page, List<String> errors) {
        CityResponse r = buildCityResponse(statusCode, success, message, dto, list, page);
        r.setErrorMessages(errors);
        return r;
    }
}
