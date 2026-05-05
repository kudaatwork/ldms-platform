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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.AdministrativeLevelServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AdministrativeLevelService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.AdministrativeLevelServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.repository.AdministrativeLevelRepository;
import projectlx.co.zw.locationsmanagementservice.repository.CountryRepository;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.AdministrativeLevelSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AdministrativeLevelDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.AdministrativeLevelMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAdministrativeLevelRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAdministrativeLevelRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AdministrativeLevelResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
@RequiredArgsConstructor
public class AdministrativeLevelServiceImpl implements AdministrativeLevelService {

    private final AdministrativeLevelServiceValidator administrativeLevelServiceValidator;
    private final AdministrativeLevelRepository administrativeLevelRepository;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final CountryRepository countryRepository;
    private final AdministrativeLevelServiceAuditable administrativeLevelServiceAuditable;
    private final MessageService messageService;

    /** CSV/PDF/XLSX export columns (align with {@link #SUPPORTED_IMPORT_HEADERS} for re-import; COUNTRY = country name). */
    private static final String[] HEADERS = {
            "ID", "NAME", "CODE", "LEVEL", "COUNTRY", "DESCRIPTION", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    /**
     * Import column names (case-insensitive). Administrative levels belong to exactly one {@link Country};
     * each row must identify that country via one of: COUNTRY_ID, ISO_ALPHA_2, ISO_ALPHA_3, COUNTRY_NAME, or COUNTRY
     * (COUNTRY matches CSV export, which writes the country display name).
     */
    private static final String[] SUPPORTED_IMPORT_HEADERS = {
            "NAME", "CODE", "LEVEL",
            "COUNTRY_ID", "ISO_ALPHA_2", "ISO_ALPHA_3", "COUNTRY_NAME", "COUNTRY",
            "DESCRIPTION"
    };

    @Override
    public AdministrativeLevelResponse create(CreateAdministrativeLevelRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = administrativeLevelServiceValidator.isCreateAdministrativeLevelRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_ADMINISTRATIVE_LEVEL_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        String normalizedName = Validators.capitalizeWords(request.getName());

        // Find the country by ID
        Optional<Country> countryOptional = countryRepository.findByIdAndEntityStatusNot(request.getCountryId(), EntityStatus.DELETED);
        
        if (countryOptional.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponse(404, false, message, null,
                    null, null);
        }

        Long countryId = countryOptional.get().getId();
        Optional<AdministrativeLevel> existingAdministrativeLevel = administrativeLevelRepository
                .findByCountry_IdAndNameAndEntityStatusNot(countryId, normalizedName, EntityStatus.DELETED);

        if (existingAdministrativeLevel.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponse(400, false, message, null,
                    null, null);
        }

        Optional<AdministrativeLevel> deletedAdministrativeLevel = administrativeLevelRepository
                .findFirstByCountry_IdAndNameAndEntityStatus(countryId, normalizedName, EntityStatus.DELETED);

        // Create or reactivate administrative level
        AdministrativeLevel administrativeLevelToBeSaved = deletedAdministrativeLevel.orElseGet(AdministrativeLevel::new);
        administrativeLevelToBeSaved.setName(normalizedName);
        administrativeLevelToBeSaved.setCode(request.getCode());
        administrativeLevelToBeSaved.setLevel(request.getLevel());
        administrativeLevelToBeSaved.setDescription(request.getDescription());
        administrativeLevelToBeSaved.setCountry(countryOptional.get());
        if (deletedAdministrativeLevel.isPresent()) {
            administrativeLevelToBeSaved.setEntityStatus(EntityStatus.ACTIVE);
        }

        AdministrativeLevel administrativeLevelSaved = deletedAdministrativeLevel.isPresent()
                ? administrativeLevelServiceAuditable.update(administrativeLevelToBeSaved, locale, username)
                : administrativeLevelServiceAuditable.create(administrativeLevelToBeSaved, locale, username);

        AdministrativeLevelDto administrativeLevelDtoReturned = toAdministrativeLevelDto(administrativeLevelSaved);

        message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildAdministrativeLevelResponse(201, true, message, administrativeLevelDtoReturned,
                null, null);
    }

    @Override
    public AdministrativeLevelResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = administrativeLevelServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildAdministrativeLevelResponseWithErrors(400, false, message, null,
                    null, null, validatorDto.getErrorMessages());
        }

        Optional<AdministrativeLevel> administrativeLevelRetrieved = administrativeLevelRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (administrativeLevelRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponse(404, false, message, null,
                    null, null);
        }

        AdministrativeLevel administrativeLevelReturned = administrativeLevelRetrieved.get();
        AdministrativeLevelDto administrativeLevelDto = toAdministrativeLevelDto(administrativeLevelReturned);

        message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildAdministrativeLevelResponse(200, true, message, administrativeLevelDto, null,
                null);
    }

    @Override
    public AdministrativeLevelResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<AdministrativeLevel> administrativeLevelList = administrativeLevelRepository.findAll().stream()
                .filter(administrativeLevel -> administrativeLevel.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        if(administrativeLevelList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildAdministrativeLevelResponse(404, false, message, null,
                    null, null);
        }

        List<AdministrativeLevelDto> administrativeLevelDtoList = new ArrayList<>();
        for (AdministrativeLevel administrativeLevel : administrativeLevelList) {
            administrativeLevelDtoList.add(toAdministrativeLevelDto(administrativeLevel));
        }

        message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildAdministrativeLevelResponse(200, true, message, null,
                administrativeLevelDtoList, null);
    }

    @Override
    public AdministrativeLevelResponse update(EditAdministrativeLevelRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = administrativeLevelServiceValidator.isRequestValidForEditing(request, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_ADMINISTRATIVE_LEVEL_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildAdministrativeLevelResponseWithErrors(400, false, message, null,
                    null, null, validatorDto.getErrorMessages());
        }

        Optional<AdministrativeLevel> administrativeLevelRetrieved = administrativeLevelRepository.findByIdAndEntityStatusNot(
                request.getId(), EntityStatus.DELETED);

        if (administrativeLevelRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponse(404, false, message, null,
                    null, null);
        }

        // Find the country by ID
        Optional<Country> countryOptional = countryRepository.findByIdAndEntityStatusNot(request.getCountryId(), EntityStatus.DELETED);
        
        if (countryOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponse(404, false, message, null,
                    null, null);
        }

        AdministrativeLevel administrativeLevelToBeEdited = administrativeLevelRetrieved.get();

        String normalizedName = Validators.capitalizeWords(request.getName());
        Long countryId = countryOptional.get().getId();
        Optional<AdministrativeLevel> duplicateNameInCountry = administrativeLevelRepository
                .findByCountry_IdAndNameAndEntityStatusNot(countryId, normalizedName, EntityStatus.DELETED);
        if (duplicateNameInCountry.isPresent()
                && !duplicateNameInCountry.get().getId().equals(administrativeLevelToBeEdited.getId())) {
            message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);
            return buildAdministrativeLevelResponse(400, false, message, null, null, null);
        }

        // Update administrative level properties from request
        administrativeLevelToBeEdited.setName(normalizedName);
        administrativeLevelToBeEdited.setDescription(request.getDescription());
        administrativeLevelToBeEdited.setCode(request.getCode());
        administrativeLevelToBeEdited.setLevel(request.getLevel());
        administrativeLevelToBeEdited.setCountry(countryOptional.get());

        AdministrativeLevel administrativeLevelEdited = administrativeLevelServiceAuditable.update(administrativeLevelToBeEdited,
                locale, username);

        AdministrativeLevelDto administrativeLevelDtoReturned = toAdministrativeLevelDto(administrativeLevelEdited);

        message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildAdministrativeLevelResponse(200, true, message, administrativeLevelDtoReturned,
                null, null);
    }

    @Override
    public AdministrativeLevelResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = administrativeLevelServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponseWithErrors(400, false, message, null,
                    null, null, validatorDto.getErrorMessages());
        }

        Optional<AdministrativeLevel> administrativeLevelRetrieved = administrativeLevelRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (administrativeLevelRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponse(404, false, message, null,
                    null, null);
        }

        AdministrativeLevel administrativeLevelToBeDeleted = administrativeLevelRetrieved.get();
        administrativeLevelToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        AdministrativeLevel administrativeLevelDeleted = administrativeLevelServiceAuditable.delete(administrativeLevelToBeDeleted,
                locale);

        AdministrativeLevelDto administrativeLevelDtoReturned = toAdministrativeLevelDto(administrativeLevelDeleted);

        message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildAdministrativeLevelResponse(200, true, message, administrativeLevelDtoReturned,
                null, null);
    }

    @Override
    public AdministrativeLevelResponse findByMultipleFilters(AdministrativeLevelMultipleFiltersRequest request, String username,
                                                             Locale locale) {

        String message = "";

        Specification<AdministrativeLevel> spec = null;
        spec = addToSpec(spec, AdministrativeLevelSpecification::deleted);

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "id"));

        if (isNotEmpty(request.getName())) {
            spec = addToSpec(request.getName(), spec, AdministrativeLevelSpecification::nameLike);
        }

        if (isNotEmpty(request.getCode())) {
            spec = addToSpec(request.getCode(), spec, AdministrativeLevelSpecification::codeLike);
        }

        if (request.getLevel() != null) {
            spec = addToSpec(request.getLevel(), spec, AdministrativeLevelSpecification::byLevel);
        }

        if (isNotEmpty(request.getDescription())) {
            spec = addToSpec(request.getDescription(), spec, AdministrativeLevelSpecification::descriptionLike);
        }

        if (request.getCountryId() != null) {
            spec = addToSpec(request.getCountryId(), spec, AdministrativeLevelSpecification::byCountry);
        }

        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, AdministrativeLevelSpecification::any);
        }

        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, AdministrativeLevelSpecification::hasEntityStatus);
        }

        Page<AdministrativeLevel> result = administrativeLevelRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildAdministrativeLevelResponse(404, false, message, null,
                    null, null);
        }

        Page<AdministrativeLevelDto> administrativeLevelDtoPage = convertAdministrativeLevelEntityToAdministrativeLevelDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildAdministrativeLevelResponse(200, true, message, null,
                null, administrativeLevelDtoPage);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<AdministrativeLevel> addToSpec(Specification<AdministrativeLevel> spec,
                                           Function<EntityStatus, Specification<AdministrativeLevel>> predicateMethod) {
        Specification<AdministrativeLevel> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<AdministrativeLevel> addToSpec(final String aString, Specification<AdministrativeLevel> spec,
                                                         Function<String,
            Specification<AdministrativeLevel>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<AdministrativeLevel> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<AdministrativeLevel> addToSpec(final Integer level, Specification<AdministrativeLevel> spec,
                                                         Function<Integer,
            Specification<AdministrativeLevel>> predicateMethod) {
        if (level != null) {
            Specification<AdministrativeLevel> localSpec = Specification.where(predicateMethod.apply(level));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<AdministrativeLevel> addToSpec(final Long countryId, Specification<AdministrativeLevel> spec,
                                                         Function<Long,
            Specification<AdministrativeLevel>> predicateMethod) {
        if (countryId != null) {
            Specification<AdministrativeLevel> localSpec = Specification.where(predicateMethod.apply(countryId));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<AdministrativeLevel> addToSpec(final EntityStatus entityStatus, Specification<AdministrativeLevel> spec,
                                                         Function<EntityStatus,
            Specification<AdministrativeLevel>> predicateMethod) {
        if (entityStatus != null) {
            Specification<AdministrativeLevel> localSpec = Specification.where(predicateMethod.apply(entityStatus));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private void applyCountryFields(AdministrativeLevel entity, AdministrativeLevelDto dto) {
        if (entity.getCountry() != null) {
            dto.setCountryId(entity.getCountry().getId());
            dto.setCountryName(entity.getCountry().getName());
        } else {
            dto.setCountryId(null);
            dto.setCountryName(null);
        }
    }

    private AdministrativeLevelDto toAdministrativeLevelDto(AdministrativeLevel entity) {
        AdministrativeLevelDto dto = new AdministrativeLevelDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setLevel(entity.getLevel());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setEntityStatus(entity.getEntityStatus());
        applyCountryFields(entity, dto);
        return dto;
    }

    private Page<AdministrativeLevelDto> convertAdministrativeLevelEntityToAdministrativeLevelDto(Page<AdministrativeLevel>
                                                                                                          administrativeLevelPage) {

        List<AdministrativeLevel> administrativeLevelList = administrativeLevelPage.getContent();
        List<AdministrativeLevelDto> administrativeLevelDtoList = new ArrayList<>();

        for (AdministrativeLevel administrativeLevel : administrativeLevelPage) {
            administrativeLevelDtoList.add(toAdministrativeLevelDto(administrativeLevel));
        }

        int page = administrativeLevelPage.getNumber();
        int size = administrativeLevelPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableAdministrativeLevels = PageRequest.of(page, size);

        return new PageImpl<>(administrativeLevelDtoList, pageableAdministrativeLevels, administrativeLevelPage.getTotalElements());
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<AdministrativeLevelDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (AdministrativeLevelDto administrativeLevel : items) {
            sb.append(administrativeLevel.getId()).append(",")
                    .append(safe(administrativeLevel.getName())).append(",")
                    .append(safe(administrativeLevel.getCode())).append(",")
                    .append(administrativeLevel.getLevel() != null ? administrativeLevel.getLevel() : "").append(",")
                    .append(safe(administrativeLevel.getCountryName())).append(",")
                    .append(safe(administrativeLevel.getDescription())).append(",")
                    .append(administrativeLevel.getCreatedAt()).append(",")
                    .append(administrativeLevel.getUpdatedAt()).append(",")
                    .append(administrativeLevel.getEntityStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<AdministrativeLevelDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Administrative Levels");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (AdministrativeLevelDto administrativeLevel : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(administrativeLevel.getId());
            row.createCell(1).setCellValue(safe(administrativeLevel.getName()));
            row.createCell(2).setCellValue(safe(administrativeLevel.getCode()));
            if (administrativeLevel.getLevel() != null) {
                row.createCell(3).setCellValue(administrativeLevel.getLevel().doubleValue());
            } else {
                row.createCell(3).setCellValue("");
            }
            row.createCell(4).setCellValue(safe(administrativeLevel.getCountryName()));
            row.createCell(5).setCellValue(safe(administrativeLevel.getDescription()));
            row.createCell(6).setCellValue(administrativeLevel.getCreatedAt() != null ? administrativeLevel.getCreatedAt().toString() : "");
            row.createCell(7).setCellValue(administrativeLevel.getUpdatedAt() != null ? administrativeLevel.getUpdatedAt().toString() : "");
            row.createCell(8).setCellValue(administrativeLevel.getEntityStatus() != null ? administrativeLevel.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<AdministrativeLevelDto> items) throws DocumentException {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("ADMINISTRATIVE LEVEL EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (AdministrativeLevelDto administrativeLevel : items) {
            table.addCell(String.valueOf(administrativeLevel.getId()));
            table.addCell(safe(administrativeLevel.getName()));
            table.addCell(safe(administrativeLevel.getCode()));
            table.addCell(administrativeLevel.getLevel() != null ? String.valueOf(administrativeLevel.getLevel()) : "");
            table.addCell(safe(administrativeLevel.getCountryName()));
            table.addCell(safe(administrativeLevel.getDescription()));
            table.addCell(administrativeLevel.getCreatedAt() != null ? administrativeLevel.getCreatedAt().toString() : "");
            table.addCell(administrativeLevel.getUpdatedAt() != null ? administrativeLevel.getUpdatedAt().toString() : "");
            table.addCell(administrativeLevel.getEntityStatus() != null ? administrativeLevel.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importAdministrativeLevelFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int total = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new BOMInputStream(csvInputStream), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                errors.add("CSV is empty or missing header row");
                return new ImportSummary(400, false, "Import failed. CSV is empty.", 0, 0, 0, errors);
            }

            List<String> headerCells = parseCsvLine(headerLine);
            if (headerCells.isEmpty()) {
                errors.add("Could not parse header row");
                return new ImportSummary(400, false, "Import failed. Invalid CSV header.", 0, 0, 0, errors);
            }

            List<String> headers = new ArrayList<>();
            for (String h : headerCells) {
                headers.add(stripUtf8Bom(h).trim());
            }

            Set<String> upperHeaders = new HashSet<>();
            for (String h : headers) {
                if (!h.isEmpty()) {
                    upperHeaders.add(h.toUpperCase(Locale.ROOT));
                }
            }
            if (!upperHeaders.contains("NAME") || !upperHeaders.contains("LEVEL")) {
                errors.add("CSV header must include NAME and LEVEL. Supported columns: "
                        + String.join(", ", SUPPORTED_IMPORT_HEADERS));
                return new ImportSummary(400, false, "Import failed. Invalid CSV header.", 0, 0, 0, errors);
            }
            if (!hasCountryScopeColumn(upperHeaders)) {
                errors.add("Each administrative level is tied to a country: include one of COUNTRY_ID, ISO_ALPHA_2, "
                        + "ISO_ALPHA_3, COUNTRY_NAME, or COUNTRY (same as export). Supported columns: "
                        + String.join(", ", SUPPORTED_IMPORT_HEADERS));
                return new ImportSummary(400, false, "Import failed. Missing country scope column.", 0, 0, 0, errors);
            }

            int physicalLineNum = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                physicalLineNum++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> rawValues;
                try {
                    rawValues = parseCsvLine(line);
                } catch (IOException e) {
                    total++;
                    failed++;
                    errors.add("Row " + physicalLineNum + ": " + e.getMessage());
                    continue;
                }

                if (rawValues.isEmpty()) {
                    continue;
                }

                total++;

                try {
                    List<String> values = reconcileRowWithHeaders(headers, rawValues);
                    if (values.size() != headers.size()) {
                        failed++;
                        errors.add("Row " + physicalLineNum + ": column count does not match header count (expected "
                                + headers.size() + " columns, found " + rawValues.size()
                                + "); quote fields that contain commas or place DESCRIPTION as the last column.");
                        continue;
                    }

                    Map<String, String> row = rowToMap(headers, values);

                    String name = cell(row, "NAME");
                    if (name == null || name.isBlank()) {
                        failed++;
                        errors.add("Row " + physicalLineNum + ": Missing administrative level name");
                        continue;
                    }

                    String levelStr = cell(row, "LEVEL");
                    Integer level;
                    if (levelStr == null || levelStr.isBlank()) {
                        failed++;
                        errors.add("Row " + physicalLineNum + ": Missing level number");
                        continue;
                    }
                    try {
                        level = Integer.parseInt(levelStr.trim());
                    } catch (NumberFormatException e) {
                        failed++;
                        errors.add("Row " + physicalLineNum + ": Invalid LEVEL value: " + levelStr);
                        continue;
                    }

                    Optional<Long> countryIdOpt = resolveCountryIdFromRow(row);
                    if (countryIdOpt.isEmpty()) {
                        failed++;
                        errors.add("Row " + physicalLineNum
                                + ": Could not resolve country from COUNTRY_ID / ISO_ALPHA_2 / ISO_ALPHA_3 / COUNTRY_NAME / COUNTRY");
                        continue;
                    }

                    CreateAdministrativeLevelRequest request = new CreateAdministrativeLevelRequest();
                    request.setName(name.trim());
                    request.setCode(blankToNull(cell(row, "CODE")));
                    request.setLevel(level);
                    request.setCountryId(countryIdOpt.get());
                    request.setDescription(blankToNull(cell(row, "DESCRIPTION")));

                    AdministrativeLevelResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        if (response.getErrorMessages() != null && !response.getErrorMessages().isEmpty()) {
                            errors.add("Row " + physicalLineNum + ": " + String.join("; ", response.getErrorMessages()));
                        } else {
                            errors.add("Row " + physicalLineNum + ": " + response.getMessage());
                        }
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + physicalLineNum + ": " + e.getMessage());
                }
            }
        }

        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " administrative levels imported."
                : "Import failed. No administrative levels were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private static String stripUtf8Bom(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        if (s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static List<String> parseCsvLine(String line) throws IOException {
        if (line == null || line.trim().isEmpty()) {
            return List.of();
        }
        try (CSVParser parser = CSVFormat.RFC4180.builder().setTrim(true).build().parse(new StringReader(line))) {
            for (CSVRecord record : parser) {
                List<String> out = new ArrayList<>(record.size());
                for (int i = 0; i < record.size(); i++) {
                    out.add(record.get(i));
                }
                return out;
            }
        }
        return List.of();
    }

    /**
     * When DESCRIPTION is the last header and the row was split on unquoted commas, merge trailing cells into the
     * description so imports match OpenOffice/Excel-style "broken" CSVs.
     */
    private static List<String> reconcileRowWithHeaders(List<String> headers, List<String> values) {
        List<String> v = new ArrayList<>(values);
        while (v.size() < headers.size()) {
            v.add("");
        }
        if (v.size() <= headers.size()) {
            return v;
        }
        int lastIdx = headers.size() - 1;
        String lastHeader = stripUtf8Bom(headers.get(lastIdx)).trim();
        if (!"DESCRIPTION".equalsIgnoreCase(lastHeader)) {
            return v;
        }
        List<String> merged = new ArrayList<>(headers.size());
        for (int i = 0; i < lastIdx; i++) {
            merged.add(v.get(i));
        }
        merged.add(String.join(",", v.subList(lastIdx, v.size())));
        return merged;
    }

    private static Map<String, String> rowToMap(List<String> headers, List<String> values) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String h = stripUtf8Bom(headers.get(i)).trim();
            if (h.isEmpty()) {
                continue;
            }
            String cellVal = i < values.size() ? values.get(i) : "";
            map.put(h.toUpperCase(Locale.ROOT), cellVal);
        }
        return map;
    }

    private static String cell(Map<String, String> row, String key) {
        return row.get(key.toUpperCase(Locale.ROOT));
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static boolean hasCountryScopeColumn(Set<String> upperHeaders) {
        return upperHeaders.contains("COUNTRY_ID")
                || upperHeaders.contains("ISO_ALPHA_2")
                || upperHeaders.contains("ISO_ALPHA_3")
                || upperHeaders.contains("COUNTRY_NAME")
                || upperHeaders.contains("COUNTRY");
    }

    private Optional<Long> resolveCountryIdFromRow(Map<String, String> row) {
        String idStr = cell(row, "COUNTRY_ID");
        if (idStr != null && !idStr.isBlank()) {
            try {
                long id = Long.parseLong(idStr.trim());
                return countryRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).map(Country::getId);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        String a2 = cell(row, "ISO_ALPHA_2");
        if (a2 != null && !a2.isBlank()) {
            return countryRepository.findByIsoAlpha2CodeAndEntityStatusNot(a2.trim(), EntityStatus.DELETED)
                    .map(Country::getId);
        }
        String a3 = cell(row, "ISO_ALPHA_3");
        if (a3 != null && !a3.isBlank()) {
            return countryRepository.findByIsoAlpha3CodeAndEntityStatusNot(a3.trim(), EntityStatus.DELETED)
                    .map(Country::getId);
        }
        String name = firstNonBlank(cell(row, "COUNTRY_NAME"), cell(row, "COUNTRY"));
        if (name != null && !name.isBlank()) {
            return countryRepository.findByNameAndEntityStatusNot(name.trim(), EntityStatus.DELETED)
                    .map(Country::getId);
        }
        return Optional.empty();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private AdministrativeLevelResponse buildAdministrativeLevelResponse(int statusCode, boolean isSuccess, String message,
                                                 AdministrativeLevelDto administrativeLevelDto, List<AdministrativeLevelDto> administrativeLevelDtoList,
                                                     Page<AdministrativeLevelDto> administrativeLevelDtoPage) {

        AdministrativeLevelResponse administrativeLevelResponse = new AdministrativeLevelResponse();

        administrativeLevelResponse.setStatusCode(statusCode);
        administrativeLevelResponse.setSuccess(isSuccess);
        administrativeLevelResponse.setMessage(message);
        administrativeLevelResponse.setAdministrativeLevelDto(administrativeLevelDto);
        administrativeLevelResponse.setAdministrativeLevelDtoList(administrativeLevelDtoList);
        administrativeLevelResponse.setAdministrativeLevelDtoPage(administrativeLevelDtoPage);

        return administrativeLevelResponse;
    }

    private AdministrativeLevelResponse buildAdministrativeLevelResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                         AdministrativeLevelDto administrativeLevelDto, List<AdministrativeLevelDto> administrativeLevelDtoList,
                                                         Page<AdministrativeLevelDto> administrativeLevelDtoPage, List<String> errorMessages) {

        AdministrativeLevelResponse administrativeLevelResponse = new AdministrativeLevelResponse();

        administrativeLevelResponse.setStatusCode(statusCode);
        administrativeLevelResponse.setSuccess(isSuccess);
        administrativeLevelResponse.setMessage(message);
        administrativeLevelResponse.setAdministrativeLevelDto(administrativeLevelDto);
        administrativeLevelResponse.setAdministrativeLevelDtoList(administrativeLevelDtoList);
        administrativeLevelResponse.setAdministrativeLevelDtoPage(administrativeLevelDtoPage);
        administrativeLevelResponse.setErrorMessages(errorMessages);

        return administrativeLevelResponse;
    }
}
