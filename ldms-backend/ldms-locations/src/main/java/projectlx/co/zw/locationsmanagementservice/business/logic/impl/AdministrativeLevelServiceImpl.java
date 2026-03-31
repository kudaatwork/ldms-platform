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
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdministrativeLevelServiceImpl implements AdministrativeLevelService {

    private final AdministrativeLevelServiceValidator administrativeLevelServiceValidator;
    private final AdministrativeLevelRepository administrativeLevelRepository;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final CountryRepository countryRepository;
    private final AdministrativeLevelServiceAuditable administrativeLevelServiceAuditable;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "NAME", "DESCRIPTION", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    private static final String[] CSV_HEADERS = {
            "NAME", "DESCRIPTION"
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

        // Check if a similar administrative level already exists
        List<AdministrativeLevel> existingAdministrativeLevels = administrativeLevelRepository.findAll().stream()
                .filter(administrativeLevel -> administrativeLevel.getEntityStatus() != EntityStatus.DELETED &&
                        administrativeLevel.getName().equalsIgnoreCase(request.getName()))
                .collect(Collectors.toList());

        if (!existingAdministrativeLevels.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildAdministrativeLevelResponse(400, false, message, null,
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

        // Create new administrative level
        AdministrativeLevel administrativeLevelToBeSaved = new AdministrativeLevel();
        administrativeLevelToBeSaved.setName(request.getName());
        administrativeLevelToBeSaved.setCode(request.getCode());
        administrativeLevelToBeSaved.setLevel(request.getLevel());
        administrativeLevelToBeSaved.setDescription(request.getDescription());
        administrativeLevelToBeSaved.setCountry(countryOptional.get());

        AdministrativeLevel administrativeLevelSaved = administrativeLevelServiceAuditable.create(administrativeLevelToBeSaved,
                locale, username);

        AdministrativeLevelDto administrativeLevelDtoReturned = modelMapper.map(administrativeLevelSaved, AdministrativeLevelDto.class);

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
        AdministrativeLevelDto administrativeLevelDto = modelMapper.map(administrativeLevelReturned, AdministrativeLevelDto.class);

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

        List<AdministrativeLevelDto> administrativeLevelDtoList = modelMapper.map(administrativeLevelList,
                new TypeToken<List<AdministrativeLevelDto>>(){}.getType());

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

        // Update administrative level properties from request
        administrativeLevelToBeEdited.setName(Validators.capitalizeWords(request.getName()));
        administrativeLevelToBeEdited.setDescription(request.getDescription());
        administrativeLevelToBeEdited.setCode(request.getCode());
        administrativeLevelToBeEdited.setLevel(request.getLevel());
        administrativeLevelToBeEdited.setCountry(countryOptional.get());

        AdministrativeLevel administrativeLevelEdited = administrativeLevelServiceAuditable.update(administrativeLevelToBeEdited,
                locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        AdministrativeLevelDto administrativeLevelDtoReturned = modelMapper.map(administrativeLevelEdited, AdministrativeLevelDto.class);

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

        AdministrativeLevelDto administrativeLevelDtoReturned = modelMapper.map(administrativeLevelDeleted, AdministrativeLevelDto.class);

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

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

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

    private Page<AdministrativeLevelDto> convertAdministrativeLevelEntityToAdministrativeLevelDto(Page<AdministrativeLevel>
                                                                                                          administrativeLevelPage) {

        List<AdministrativeLevel> administrativeLevelList = administrativeLevelPage.getContent();
        List<AdministrativeLevelDto> administrativeLevelDtoList = new ArrayList<>();

        for (AdministrativeLevel administrativeLevel : administrativeLevelPage) {
            AdministrativeLevelDto administrativeLevelDto = modelMapper.map(administrativeLevel, AdministrativeLevelDto.class);
            administrativeLevelDtoList.add(administrativeLevelDto);
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
            row.createCell(2).setCellValue(safe(administrativeLevel.getDescription()));
            row.createCell(3).setCellValue(administrativeLevel.getCreatedAt() != null ? administrativeLevel.getCreatedAt().toString() : "");
            row.createCell(4).setCellValue(administrativeLevel.getUpdatedAt() != null ? administrativeLevel.getUpdatedAt().toString() : "");
            row.createCell(5).setCellValue(administrativeLevel.getEntityStatus() != null ? administrativeLevel.getEntityStatus().toString() : "");
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
        int success = 0, failed = 0, total = 0;

        // Load OpenCV native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Create a temporary file from the input stream
        File tempFile = File.createTempFile("administrative_level_import", ".csv");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = csvInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        // Read CSV file using OpenCV
        Mat csvData = new Mat();

        // Convert the CSV file to a string for processing
        String csvContent = new String(Files.readAllBytes(tempFile.toPath()), StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\\r?\\n");

        // Extract headers
        if (lines.length > 0) {
            String[] headers = lines[0].split(",");
            Map<String, Integer> headerMap = new HashMap<>();

            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim(), i);
            }

            // Process data rows
            total = lines.length - 1; // Exclude header row

            for (int rowIndex = 1; rowIndex < lines.length; rowIndex++) {
                try {
                    String[] values = lines[rowIndex].split(",");

                    CreateAdministrativeLevelRequest request = new CreateAdministrativeLevelRequest();

                    // Get values by header name
                    Integer nameIndex = headerMap.get("NAME");
                    if (nameIndex != null && nameIndex < values.length) {
                        request.setName(values[nameIndex].trim());
                    }

                    Integer descriptionIndex = headerMap.get("DESCRIPTION");
                    if (descriptionIndex != null && descriptionIndex < values.length) {
                        request.setDescription(values[descriptionIndex].trim());
                    }

                    // Optional fields - check if they exist in the CSV
                    Integer codeIndex = headerMap.get("CODE");
                    if (codeIndex != null) { // Equivalent to record.isMapped("CODE")
                        if (codeIndex < values.length) {
                            request.setCode(values[codeIndex].trim());
                        }
                    }

                    Integer levelIndex = headerMap.get("LEVEL");
                    if (levelIndex != null) { // Equivalent to record.isMapped("LEVEL")
                        if (levelIndex < values.length) {
                            try {
                                request.setLevel(Integer.parseInt(values[levelIndex].trim()));
                            } catch (NumberFormatException e) {
                                // Handle invalid level format
                                request.setLevel(null);
                            }
                        }
                    }

                    AdministrativeLevelResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + rowIndex + ": " + response.getMessage());
                    }

                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + rowIndex + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        // Delete the temporary file
        tempFile.delete();

        // Determine status code and success flag based on import results
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " administrative levels imported."
                : "Import failed. No administrative levels were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
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
