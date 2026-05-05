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
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.AdministrativeLevelServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.ProvinceServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.ProvinceService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.ProvinceServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.model.LocalizedName;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.locationsmanagementservice.repository.AdministrativeLevelRepository;
import projectlx.co.zw.locationsmanagementservice.repository.CountryRepository;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.ProvinceRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.AdministrativeLevelSpecification;
import projectlx.co.zw.locationsmanagementservice.repository.specification.ProvinceSpecification;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import projectlx.co.zw.locationsmanagementservice.utils.GeoCoordinateCsvImportHelper;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ProvinceCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ProvinceDto;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.ProvinceMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.ProvinceResponse;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ProvinceServiceImpl implements ProvinceService {

    private final ProvinceServiceValidator provinceServiceValidator;
    private final ProvinceRepository provinceRepository;
    private final CountryRepository countryRepository;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final AdministrativeLevelRepository administrativeLevelRepository;
    private final AdministrativeLevelServiceAuditable administrativeLevelServiceAuditable;
    private final LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService;
    private final ProvinceServiceAuditable provinceServiceAuditable;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "NAME", "CODE", "COUNTRY", "ADMIN LEVEL", "COUNTRY ID", "ADMINISTRATIVE LEVEL ID",
            "GEO COORDINATES ID", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    @Override
    @Transactional
    public ProvinceResponse create(CreateProvinceRequest request, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = provinceServiceValidator.isCreateProvinceRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PROVINCE_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildProvinceResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Country> countryRetrieved =
                countryRepository.findByIdAndEntityStatusNot(request.getCountryId(), EntityStatus.DELETED);

        if (countryRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);
            return buildProvinceResponse(404, false, message, null,
                    null, null);
        }

        Country country = countryRetrieved.get();
        String normalizedName = Validators.capitalizeWords(request.getName());
        Optional<Province> provinceActiveInCountry =
                provinceRepository.findByNameAndCountryAndEntityStatusNot(normalizedName, country, EntityStatus.DELETED);

        if (provinceActiveInCountry.isPresent()) {
            message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);
            return buildProvinceResponse(409, false, message, null,
                    null, null);
        }

        Optional<Province> deletedProvince = provinceRepository.findByNameAndCountry(normalizedName, country)
                .filter(province -> province.getEntityStatus() == EntityStatus.DELETED);

        request.setName(normalizedName);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Province provinceToBeSaved = deletedProvince.orElseGet(Province::new);
        if (deletedProvince.isPresent()) {
            modelMapper.map(request, provinceToBeSaved);
        } else {
            provinceToBeSaved = modelMapper.map(request, Province.class);
        }
        provinceToBeSaved.setCountry(country);

        Long administrativeLevelId = request.getAdministrativeLevelId();
        if (administrativeLevelId != null && administrativeLevelId <= 0) {
            administrativeLevelId = null;
            request.setAdministrativeLevelId(null);
        }
        if (administrativeLevelId != null) {
            Optional<AdministrativeLevel> administrativeLevelOptional = administrativeLevelRepository
                    .findByIdAndEntityStatusNot(administrativeLevelId, EntityStatus.DELETED);
            if (administrativeLevelOptional.isEmpty()) {
                message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(), new String[]{},
                        locale);
                return buildProvinceResponse(404, false, message, null,
                        null, null);
            }
            provinceToBeSaved.setAdministrativeLevel(administrativeLevelOptional.get());
        }
        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildProvinceResponse(400, false, message, null, null, null);
        }

        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildProvinceResponse(400, false, message, null, null, null);
        }
        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                request.getGeoCoordinatesId(), request.getLatitude(), request.getLongitude());
        if (request.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildProvinceResponse(404, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
            provinceToBeSaved.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        if (deletedProvince.isPresent()) {
            provinceToBeSaved.setEntityStatus(EntityStatus.ACTIVE);
        }
        Province provinceSaved = deletedProvince.isPresent()
                ? provinceServiceAuditable.update(provinceToBeSaved, locale, username)
                : provinceServiceAuditable.create(provinceToBeSaved, locale, username);
        ProvinceDto provinceDtoReturned = toProvinceDto(provinceSaved);
        message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);
        return buildProvinceResponse(201, true, message, provinceDtoReturned, null,
                null);
    }

    @Override
    public ProvinceResponse findById(Long id, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = provinceServiceValidator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildProvinceResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Optional<Province> provinceRetrieved = provinceRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (provinceRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildProvinceResponse(404, false, message, null, null, null);
        }
        ProvinceDto provinceDto = toProvinceDto(provinceRetrieved.get());
        message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildProvinceResponse(200, true, message, provinceDto, null, null);
    }

    @Override
    public ProvinceResponse findAllAsList(Locale locale, String username) {
        String message;
        List<Province> provinceList = provinceRepository.findAll().stream()
                .filter(province -> province.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());
        if (provinceList.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildProvinceResponse(404, false, message, null, null, null);
        }
        List<ProvinceDto> provinceDtoList = provinceList.stream().map(this::toProvinceDto).collect(Collectors.toList());
        message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildProvinceResponse(200, true, message, null, provinceDtoList, null);
    }

    @Override
    @Transactional
    public ProvinceResponse update(EditProvinceRequest request, String username, Locale locale) {
        String message;
        ValidatorDto validatorDto = provinceServiceValidator.isRequestValidForEditing(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PROVINCE_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildProvinceResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Optional<Province> provinceRetrieved = provinceRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (provinceRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildProvinceResponse(404, false, message, null, null, null);
        }
        Province provinceToBeEdited = provinceRetrieved.get();
        provinceToBeEdited.setName(Validators.capitalizeWords(request.getName()));
        provinceToBeEdited.setCode(request.getCode());

        if (request.getCountryId() != null) {
            countryRepository.findByIdAndEntityStatusNot(request.getCountryId(), EntityStatus.DELETED)
                    .ifPresent(provinceToBeEdited::setCountry);
        }
        if (request.getAdministrativeLevelId() != null) {
            administrativeLevelRepository.findByIdAndEntityStatusNot(request.getAdministrativeLevelId(), EntityStatus.DELETED)
                    .ifPresent(provinceToBeEdited::setAdministrativeLevel);
        }
        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                request.getGeoCoordinatesId(), request.getLatitude(), request.getLongitude());
        if (request.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildProvinceResponse(404, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
            provinceToBeEdited.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        Province provinceEdited = provinceServiceAuditable.update(provinceToBeEdited, locale, username);
        ProvinceDto provinceDtoReturned = toProvinceDto(provinceEdited);
        message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildProvinceResponse(200, true, message, provinceDtoReturned, null, null);
    }

    @Override
    @Transactional
    public ProvinceResponse delete(Long id, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = provinceServiceValidator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildProvinceResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }
        Optional<Province> provinceRetrieved = provinceRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (provinceRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildProvinceResponse(404, false, message, null, null, null);
        }
        Province provinceToBeDeleted = provinceRetrieved.get();

        locationHierarchyCascadeSoftDeleteService.cascadeBeforeDeletingProvince(provinceToBeDeleted.getId(), locale, username);

        provinceToBeDeleted.setEntityStatus(EntityStatus.DELETED);
        Province provinceDeleted = provinceServiceAuditable.delete(provinceToBeDeleted, locale);
        ProvinceDto provinceDtoReturned = toProvinceDto(provinceDeleted);
        message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildProvinceResponse(200, true, message, provinceDtoReturned, null, null);
    }

    @Override
    public ProvinceResponse findByMultipleFilters(ProvinceMultipleFiltersRequest request, String username, Locale locale) {
        String message;
        Specification<Province> spec = Specification.where(ProvinceSpecification.deleted(EntityStatus.DELETED));

        if (isNotEmpty(request.getName())) {
            spec = spec.and(ProvinceSpecification.nameLike(request.getName()));
        }
        if (isNotEmpty(request.getCode())) {
            spec = spec.and(ProvinceSpecification.codeLike(request.getCode()));
        }
        if (isNotEmpty(request.getSearchValue())) {
            spec = spec.and(ProvinceSpecification.any(request.getSearchValue()));
        }
        if (request.getEntityStatus() != null) {
            spec = spec.and(ProvinceSpecification.hasEntityStatus(request.getEntityStatus()));
        }

        if (request.getCountryId() != null) {
            spec = spec.and(ProvinceSpecification.byCountry(request.getCountryId()));
        }

        if (request.getAdministrativeLevelId() != null) {
            spec = spec.and(ProvinceSpecification.byAdministrativeLevel(request.getAdministrativeLevelId()));
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "id"));
        Page<Province> result = provinceRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildProvinceResponse(404, false, message, null, null, null);
        }

        Page<ProvinceDto> provinceDtoPage = result.map(this::toProvinceDto);
        message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildProvinceResponse(200, true, message, null, null, provinceDtoPage);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<ProvinceDto> items) {
        if (items == null) {
            items = Collections.emptyList();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (ProvinceDto province : items) {
            sb.append(province.getId()).append(",")
                    .append(safe(province.getName())).append(",")
                    .append(safe(province.getCode())).append(",")
                    .append(safe(province.getCountryName())).append(",")
                    .append(safe(province.getAdministrativeLevelName())).append(",")
                    .append(province.getCountryId() != null ? province.getCountryId() : "").append(",")
                    .append(province.getAdministrativeLevelId() != null ? province.getAdministrativeLevelId() : "").append(",")
                    .append(province.getGeoCoordinatesId() != null ? province.getGeoCoordinatesId() : "").append(",")
                    .append(province.getCreatedAt()).append(",")
                    .append(province.getUpdatedAt()).append(",")
                    .append(province.getEntityStatus()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<ProvinceDto> items) throws IOException {
        if (items == null) {
            items = Collections.emptyList();
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Provinces");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIdx = 1;
            for (ProvinceDto province : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(province.getId());
                row.createCell(1).setCellValue(safe(province.getName()));
                row.createCell(2).setCellValue(safe(province.getCode()));
                row.createCell(3).setCellValue(safe(province.getCountryName()));
                row.createCell(4).setCellValue(safe(province.getAdministrativeLevelName()));
                row.createCell(5).setCellValue(province.getCountryId() != null ? province.getCountryId() : 0);
                row.createCell(6).setCellValue(province.getAdministrativeLevelId() != null ? province.getAdministrativeLevelId() : 0);
                row.createCell(7).setCellValue(province.getGeoCoordinatesId() != null ? province.getGeoCoordinatesId() : 0);
                row.createCell(8).setCellValue(province.getCreatedAt() != null ? province.getCreatedAt().toString() : "");
                row.createCell(9).setCellValue(province.getUpdatedAt() != null ? province.getUpdatedAt().toString() : "");
                row.createCell(10).setCellValue(province.getEntityStatus() != null ? province.getEntityStatus().toString() : "");
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<ProvinceDto> items) throws DocumentException {
        if (items == null) {
            items = Collections.emptyList();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();
            Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("PROVINCE EXPORT", font));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(HEADERS.length);
            for (String header : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, font));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }
            for (ProvinceDto province : items) {
                table.addCell(String.valueOf(province.getId()));
                table.addCell(safe(province.getName()));
                table.addCell(safe(province.getCode()));
                table.addCell(safe(province.getCountryName()));
                table.addCell(safe(province.getAdministrativeLevelName()));
                table.addCell(province.getCountryId() != null ? province.getCountryId().toString() : "");
                table.addCell(province.getAdministrativeLevelId() != null ? province.getAdministrativeLevelId().toString() : "");
                table.addCell(province.getGeoCoordinatesId() != null ? province.getGeoCoordinatesId().toString() : "");
                table.addCell(province.getCreatedAt() != null ? province.getCreatedAt().toString() : "");
                table.addCell(province.getUpdatedAt() != null ? province.getUpdatedAt().toString() : "");
                table.addCell(province.getEntityStatus() != null ? province.getEntityStatus().toString() : "");
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImportSummary importProvinceFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<ProvinceCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(ProvinceCsvDto.class);

            CsvToBean<ProvinceCsvDto> csvToBean = new CsvToBeanBuilder<ProvinceCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<ProvinceCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (ProvinceCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getName() == null || row.getName().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing province name");
                        continue;
                    }
                    if (row.getCountryId() == null) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing country ID");
                        continue;
                    }

                    Optional<Country> countryOpt =
                            countryRepository.findByIdFetchingGeoCoordinates(row.getCountryId(), EntityStatus.DELETED);
                    if (countryOpt.isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Country not found for ID " + row.getCountryId());
                        continue;
                    }
                    Country country = countryOpt.get();

                    CreateProvinceRequest request = new CreateProvinceRequest();
                    request.setName(row.getName());
                    request.setCode(row.getCode());
                    request.setCountryId(row.getCountryId());
                    Long adminLevelId =
                            resolveAdministrativeLevelIdForProvinceImport(row.getAdministrativeLevelId(), row.getCountryId());
                    if (adminLevelId == null) {
                        adminLevelId = ensureDefaultProvinceAdministrativeLevelForCountry(country, Locale.ENGLISH, "IMPORT_SCRIPT");
                    }
                    request.setAdministrativeLevelId(adminLevelId);
                    applyProvinceImportGeoRequest(request, row, country);

                    ProvinceResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + rowNum + ": " + response.getMessage());
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
                ? "Import completed successfully. " + success + " out of " + total + " provinces imported."
                : "Import failed. No provinces were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * CSV templates often use placeholder administrative level IDs (e.g. {@code 1}) that do not exist in the
     * target database. OpenCSV may also bind blank cells to {@code 0}. For import, when the supplied id is
     * missing, non-positive, or not tied to the row's country, use that country's default province tier
     * administrative level: prefer {@code level == 1} (lowest id), else the active level with the smallest
     * {@code level} value {@code >= 1}. Import may create a tier-1 level when none exist (see
     * {@link #ensureDefaultProvinceAdministrativeLevelForCountry}).
     */
    private Long resolveAdministrativeLevelIdForProvinceImport(Long requestedId, Long countryId) {
        Long normalized = requestedId == null || requestedId <= 0 ? null : requestedId;
        Optional<Long> defaultForCountry = resolveDefaultProvinceAdministrativeLevelId(countryId);
        if (normalized == null) {
            return defaultForCountry.orElse(null);
        }
        Optional<AdministrativeLevel> byId =
                administrativeLevelRepository.findByIdAndEntityStatusNot(normalized, EntityStatus.DELETED);
        if (byId.isEmpty()) {
            return defaultForCountry.orElse(null);
        }
        Country levelCountry = byId.get().getCountry();
        if (levelCountry == null || !countryId.equals(levelCountry.getId())) {
            return defaultForCountry.orElse(null);
        }
        return normalized;
    }

    private Optional<Long> resolveDefaultProvinceAdministrativeLevelId(Long countryId) {
        if (countryId == null || countryId <= 0) {
            return Optional.empty();
        }
        Specification<AdministrativeLevel> base = Specification.where(AdministrativeLevelSpecification.byCountry(countryId))
                .and(AdministrativeLevelSpecification.deleted(EntityStatus.DELETED));
        Specification<AdministrativeLevel> tier1Spec = base.and(AdministrativeLevelSpecification.byLevel(1));
        List<AdministrativeLevel> tier1 = administrativeLevelRepository.findAll(tier1Spec);
        if (!tier1.isEmpty()) {
            return tier1.stream()
                    .min(Comparator.comparing(AdministrativeLevel::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(AdministrativeLevel::getId);
        }
        Specification<AdministrativeLevel> positiveSpec = base.and((root, query, cb) -> cb.ge(root.get("level"), 1));
        List<AdministrativeLevel> withPositiveLevel = administrativeLevelRepository.findAll(positiveSpec);
        if (withPositiveLevel.isEmpty()) {
            return Optional.empty();
        }
        return withPositiveLevel.stream()
                .min(Comparator.comparing(AdministrativeLevel::getLevel)
                        .thenComparing(AdministrativeLevel::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AdministrativeLevel::getId);
    }

    /**
     * When a country has no suitable administrative level row for provinces, create a tier-1 definition
     * with a name derived from the country's ISO α2 code (unique within that country).
     */
    private Long ensureDefaultProvinceAdministrativeLevelForCountry(Country country, Locale locale, String username) {
        if (country == null || country.getId() == null) {
            return null;
        }
        String iso = country.getIsoAlpha2Code() != null ? country.getIsoAlpha2Code().trim().toUpperCase(Locale.ROOT) : "XX";
        String proposedName = "Province (" + iso + ")";
        Optional<AdministrativeLevel> activeInCountry = administrativeLevelRepository
                .findByCountry_IdAndNameAndEntityStatusNot(country.getId(), proposedName, EntityStatus.DELETED);
        if (activeInCountry.isPresent()) {
            return activeInCountry.get().getId();
        }
        Optional<AdministrativeLevel> byName = administrativeLevelRepository
                .findFirstByCountry_IdAndNameAndEntityStatus(country.getId(), proposedName, EntityStatus.DELETED);
        if (byName.isPresent()) {
            AdministrativeLevel revive = byName.get();
            revive.setEntityStatus(EntityStatus.ACTIVE);
            revive.setCode("ADM1");
            revive.setLevel(1);
            revive.setDescription("Reactivated for province CSV import");
            revive.setCountry(country);
            return administrativeLevelServiceAuditable.update(revive, locale, username).getId();
        }
        AdministrativeLevel level = new AdministrativeLevel();
        level.setName(proposedName);
        level.setCode("ADM1");
        level.setLevel(1);
        level.setDescription("Auto-created when importing provinces (CSV)");
        level.setCountry(country);
        return administrativeLevelServiceAuditable.create(level, locale, username).getId();
    }

    private void applyProvinceImportGeoRequest(CreateProvinceRequest request, ProvinceCsvDto row, Country country) {
        GeoCoordinateCsvImportHelper.ResolvedGeo r = GeoCoordinateCsvImportHelper.resolve(
                row.getGeoCoordinatesId(),
                row.getLatitude(),
                row.getLongitude(),
                country.getGeoCoordinates());
        request.setGeoCoordinatesId(r.geoCoordinatesId());
        request.setLatitude(r.latitude());
        request.setLongitude(r.longitude());
    }

    private Optional<GeoCoordinates> resolveGeoCoordinates(Long geoCoordinatesId, BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                return Optional.empty();
            }
            GeoCoordinates geoCoordinates = new GeoCoordinates();
            geoCoordinates.setLatitude(latitude);
            geoCoordinates.setLongitude(longitude);
            GeoCoordinates saved = geoCoordinatesRepository.save(geoCoordinates);
            return Optional.of(saved);
        }
        if (geoCoordinatesId == null) {
            return Optional.empty();
        }
        return geoCoordinatesRepository.findByIdAndEntityStatusNot(geoCoordinatesId, EntityStatus.DELETED);
    }

    private ProvinceDto toProvinceDto(Province entity) {
        ProvinceDto dto = new ProvinceDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        if (entity.getCountry() != null) {
            dto.setCountryId(entity.getCountry().getId());
            dto.setCountryName(entity.getCountry().getName());
        }
        if (entity.getAdministrativeLevel() != null) {
            dto.setAdministrativeLevelId(entity.getAdministrativeLevel().getId());
            dto.setAdministrativeLevelName(entity.getAdministrativeLevel().getName());
        }
        dto.setGeoCoordinatesId(entity.getGeoCoordinates() != null ? entity.getGeoCoordinates().getId() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setEntityStatus(entity.getEntityStatus());
        if (entity.getLocalizedNames() != null && Hibernate.isInitialized(entity.getLocalizedNames())) {
            dto.setLocalizedNameIds(entity.getLocalizedNames().stream().map(LocalizedName::getId).collect(Collectors.toList()));
        }
        return dto;
    }

    private ProvinceResponse buildProvinceResponse(int statusCode, boolean isSuccess, String message,
                                                   ProvinceDto provinceDto, List<ProvinceDto> provinceDtoList,
                                                   Page<ProvinceDto> provinceDtoPage) {
        ProvinceResponse provinceResponse = new ProvinceResponse();
        provinceResponse.setStatusCode(statusCode);
        provinceResponse.setSuccess(isSuccess);
        provinceResponse.setMessage(message);
        provinceResponse.setProvinceDto(provinceDto);
        provinceResponse.setProvinceDtoList(provinceDtoList);
        provinceResponse.setProvinceDtoPage(provinceDtoPage);
        return provinceResponse;
    }

    private ProvinceResponse buildProvinceResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                             ProvinceDto provinceDto, List<ProvinceDto> provinceDtoList,
                                                             Page<ProvinceDto> provinceDtoPage, List<String> errorMessages) {
        ProvinceResponse provinceResponse = buildProvinceResponse(statusCode, isSuccess, message, provinceDto, provinceDtoList, provinceDtoPage);
        provinceResponse.setErrorMessages(errorMessages);
        return provinceResponse;
    }
}
