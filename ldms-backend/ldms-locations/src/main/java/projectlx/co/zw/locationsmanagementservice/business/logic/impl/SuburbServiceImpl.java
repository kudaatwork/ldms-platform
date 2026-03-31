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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
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
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.SuburbServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.SuburbService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.SuburbServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.repository.AdministrativeLevelRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.SuburbSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.SuburbDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.SuburbMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.SuburbResponse;
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

@Service
@RequiredArgsConstructor
public class SuburbServiceImpl implements SuburbService {

    private final SuburbServiceValidator suburbServiceValidator;
    private final SuburbRepository suburbRepository;
    private final DistrictRepository districtRepository;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final AdministrativeLevelRepository administrativeLevelRepository;
    private final SuburbServiceAuditable suburbServiceAuditable;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "NAME", "CODE", "DISTRICT ID", "POSTAL CODE", "ADMINISTRATIVE LEVEL ID", "GEO COORDINATES ID", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    private static final String[] CSV_HEADERS = {
            "NAME", "CODE", "DISTRICT ID", "POSTAL CODE", "ADMINISTRATIVE LEVEL ID", "GEO COORDINATES ID"
    };

    @Override
    public SuburbResponse create(CreateSuburbRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = suburbServiceValidator.isCreateSuburbRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_SUBURB_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildSuburbResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Suburb> suburbRetrieved =
                suburbRepository.findAll().stream()
                        .filter(s -> s.getName().equalsIgnoreCase(request.getName()) && s.getEntityStatus() != EntityStatus.DELETED)
                        .findFirst();

        if (suburbRetrieved.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_SUBURB_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildSuburbResponse(400, false, message, null,
                    null, null);
        }

        // Validate district exists
        Optional<District> districtRetrieved = 
                districtRepository.findById(request.getDistrictId());

        if (districtRetrieved.isEmpty() || districtRetrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildSuburbResponse(400, false, message, null,
                    null, null);
        }

        request.setName(Validators.capitalizeWords(request.getName()));
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Suburb suburbToBeSaved = modelMapper.map(request, Suburb.class);

        // Set the district
        suburbToBeSaved.setDistrict(districtRetrieved.get());

        // Set geo coordinates if provided
        if (request.getGeoCoordinatesId() != null) {

            Optional<GeoCoordinates> geoCoordinatesOptional = geoCoordinatesRepository
                    .findByIdAndEntityStatusNot(request.getGeoCoordinatesId(), EntityStatus.DELETED);

            if (geoCoordinatesOptional.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{},
                        locale);

                return buildSuburbResponse(400, false, message, null,
                        null, null);
            }

            suburbToBeSaved.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        if (request.getAdministrativeLevelId() != null) {

            Optional<AdministrativeLevel> administrativeLevelOptional = administrativeLevelRepository
                    .findByIdAndEntityStatusNot(request.getAdministrativeLevelId(), EntityStatus.DELETED);

            if (administrativeLevelOptional.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(), new String[]{},
                        locale);

                return buildSuburbResponse(400, false, message, null,
                        null, null);
            }

            suburbToBeSaved.setAdministrativeLevel(administrativeLevelOptional.get());
        }

        Suburb suburbSaved = suburbServiceAuditable.create(suburbToBeSaved, locale, username);

        SuburbDto suburbDtoReturned = modelMapper.map(suburbSaved, SuburbDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SUBURB_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildSuburbResponse(201, true, message, suburbDtoReturned, null,
                null);
    }

    @Override
    public SuburbResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = suburbServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildSuburbResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Suburb> suburbRetrieved = suburbRepository.findById(id);

        if (suburbRetrieved.isEmpty() || suburbRetrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_SUBURB_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildSuburbResponse(404, false, message, null, null,
                    null);
        }

        Suburb suburbReturned = suburbRetrieved.get();
        SuburbDto suburbDto = modelMapper.map(suburbReturned, SuburbDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SUBURB_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildSuburbResponse(200, true, message, suburbDto, null,
                null);
    }

    @Override
    public SuburbResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<Suburb> suburbList = suburbRepository.findAll().stream()
                .filter(suburb -> suburb.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        if(suburbList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SUBURB_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildSuburbResponse(404, false, message, null,
                    null, null);
        }

        List<SuburbDto> suburbDtoList = modelMapper.map(suburbList, new TypeToken<List<SuburbDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_SUBURB_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildSuburbResponse(200, true, message, null, suburbDtoList,
                null);
    }

    @Override
    public SuburbResponse update(EditSuburbRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = suburbServiceValidator.isRequestValidForEditing(request, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildSuburbResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Suburb> suburbRetrieved = suburbRepository.findById(request.getId());

        if (suburbRetrieved.isEmpty() || suburbRetrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_SUBURB_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildSuburbResponse(404, false, message, null, null,
                    null);
        }

        Suburb suburbToBeEdited = suburbRetrieved.get();

        // Update suburb properties from request
        suburbToBeEdited.setName(Validators.capitalizeWords(request.getName()));
        suburbToBeEdited.setCode(request.getCode());
        suburbToBeEdited.setPostalCode(request.getPostalCode());

        // Handle district if provided
        if (request.getDistrictId() != null) {
            Optional<District> districtOptional = districtRepository.findById(request.getDistrictId());
            if (districtOptional.isPresent() && districtOptional.get().getEntityStatus() != EntityStatus.DELETED) {
                suburbToBeEdited.setDistrict(districtOptional.get());
            }
        }

        // Handle geo coordinates if provided
        if (request.getGeoCoordinatesId() != null) {
            Optional<GeoCoordinates> geoCoordinatesOptional = geoCoordinatesRepository.findById(request.getGeoCoordinatesId());
            if (geoCoordinatesOptional.isPresent()) {
                suburbToBeEdited.setGeoCoordinates(geoCoordinatesOptional.get());
            }
        }

        // Handle administrative level if provided
        if (request.getAdministrativeLevelId() != null) {
            Optional<AdministrativeLevel> administrativeLevelOptional = administrativeLevelRepository.findById(request.getAdministrativeLevelId());
            if (administrativeLevelOptional.isPresent()) {
                suburbToBeEdited.setAdministrativeLevel(administrativeLevelOptional.get());
            }
        }

        Suburb suburbEdited = suburbServiceAuditable.update(suburbToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SuburbDto suburbDtoReturned = modelMapper.map(suburbEdited, SuburbDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SUBURB_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildSuburbResponse(200, true, message, suburbDtoReturned, null,
                null);
    }

    @Override
    public SuburbResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = suburbServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildSuburbResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Suburb> suburbRetrieved = suburbRepository.findById(id);

        if (suburbRetrieved.isEmpty() || suburbRetrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_SUBURB_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildSuburbResponse(404, false, message, null, null,
                    null);
        }

        Suburb suburbToBeDeleted = suburbRetrieved.get();
        suburbToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        Suburb suburbDeleted = suburbServiceAuditable.delete(suburbToBeDeleted, locale, username);

        SuburbDto suburbDtoReturned = modelMapper.map(suburbDeleted, SuburbDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SUBURB_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildSuburbResponse(200, true, message, suburbDtoReturned, null,
                null);
    }

    @Override
    public SuburbResponse findByMultipleFilters(SuburbMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<Suburb> spec = null;
        spec = addToSpec(spec, SuburbSpecification::deleted);

        ValidatorDto validatorDto = suburbServiceValidator.isRequestValidForEditing(null, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildSuburbResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (isNotEmpty(request.getName())) {
            spec = addToSpec(request.getName(), spec, SuburbSpecification::nameLike);
        }

        if (isNotEmpty(request.getCode())) {
            spec = addToSpec(request.getCode(), spec, SuburbSpecification::codeLike);
        }

        if (isNotEmpty(request.getPostalCode())) {
            spec = addToSpec(request.getPostalCode(), spec, SuburbSpecification::postalCodeLike);
        }

        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, SuburbSpecification::any);
        }

        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, SuburbSpecification::hasEntityStatus);
        }

        Page<Suburb> result = suburbRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SUBURB_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildSuburbResponse(404, false, message, null, null, null);
        }

        Page<SuburbDto> suburbDtoPage = convertSuburbEntityToSuburbDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_SUBURB_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildSuburbResponse(200, true, message, null, null, suburbDtoPage);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<SuburbDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (SuburbDto suburb : items) {
            sb.append(suburb.getId()).append(",")
                    .append(safe(suburb.getName())).append(",")
                    .append(safe(suburb.getCode())).append(",")
                    .append(suburb.getDistrictId()).append(",")
                    .append(safe(suburb.getPostalCode())).append(",")
                    .append(suburb.getAdministrativeLevelId()).append(",")
                    .append(suburb.getGeoCoordinatesId()).append(",")
                    .append(suburb.getCreatedAt()).append(",")
                    .append(suburb.getUpdatedAt()).append(",")
                    .append(suburb.getEntityStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<SuburbDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Suburbs");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (SuburbDto suburb : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(suburb.getId());
            row.createCell(1).setCellValue(safe(suburb.getName()));
            row.createCell(2).setCellValue(safe(suburb.getCode()));
            row.createCell(3).setCellValue(suburb.getDistrictId() != null ? suburb.getDistrictId() : 0);
            row.createCell(4).setCellValue(safe(suburb.getPostalCode()));
            row.createCell(5).setCellValue(suburb.getAdministrativeLevelId() != null ? suburb.getAdministrativeLevelId() : 0);
            row.createCell(6).setCellValue(suburb.getGeoCoordinatesId() != null ? suburb.getGeoCoordinatesId() : 0);
            row.createCell(7).setCellValue(suburb.getCreatedAt() != null ? suburb.getCreatedAt().toString() : "");
            row.createCell(8).setCellValue(suburb.getUpdatedAt() != null ? suburb.getUpdatedAt().toString() : "");
            row.createCell(9).setCellValue(suburb.getEntityStatus() != null ? suburb.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<SuburbDto> items) throws DocumentException {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("SUBURB EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (SuburbDto suburb : items) {
            table.addCell(String.valueOf(suburb.getId()));
            table.addCell(safe(suburb.getName()));
            table.addCell(safe(suburb.getCode()));
            table.addCell(suburb.getDistrictId() != null ? suburb.getDistrictId().toString() : "");
            table.addCell(safe(suburb.getPostalCode()));
            table.addCell(suburb.getAdministrativeLevelId() != null ? suburb.getAdministrativeLevelId().toString() : "");
            table.addCell(suburb.getGeoCoordinatesId() != null ? suburb.getGeoCoordinatesId().toString() : "");
            table.addCell(suburb.getCreatedAt() != null ? suburb.getCreatedAt().toString() : "");
            table.addCell(suburb.getUpdatedAt() != null ? suburb.getUpdatedAt().toString() : "");
            table.addCell(suburb.getEntityStatus() != null ? suburb.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importSuburbFromCsv(InputStream csvInputStream) throws IOException {

        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        // Load OpenCV native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Create a temporary file from the input stream
        File tempFile = File.createTempFile("suburb_import", ".csv");
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

                    CreateSuburbRequest request = new CreateSuburbRequest();

                    // Get values by header name
                    Integer nameIndex = headerMap.get("NAME");
                    if (nameIndex != null && nameIndex < values.length) {
                        request.setName(values[nameIndex].trim());
                    }

                    Integer codeIndex = headerMap.get("CODE");
                    if (codeIndex != null && codeIndex < values.length) {
                        request.setCode(values[codeIndex].trim());
                    }

                    Integer postalCodeIndex = headerMap.get("POSTAL CODE");
                    if (postalCodeIndex != null && postalCodeIndex < values.length) {
                        request.setPostalCode(values[postalCodeIndex].trim());
                    }

                    Integer districtIdIndex = headerMap.get("DISTRICT ID");
                    if (districtIdIndex != null && districtIdIndex < values.length) {
                        String districtIdStr = values[districtIdIndex].trim();
                        if (districtIdStr != null && !districtIdStr.isEmpty()) {
                            request.setDistrictId(Long.parseLong(districtIdStr));
                        }
                    }

                    Integer adminLevelIdIndex = headerMap.get("ADMINISTRATIVE LEVEL ID");
                    if (adminLevelIdIndex != null && adminLevelIdIndex < values.length) {
                        String adminLevelIdStr = values[adminLevelIdIndex].trim();
                        if (adminLevelIdStr != null && !adminLevelIdStr.isEmpty()) {
                            request.setAdministrativeLevelId(Long.parseLong(adminLevelIdStr));
                        }
                    }

                    Integer geoCoordinatesIdIndex = headerMap.get("GEO COORDINATES ID");
                    if (geoCoordinatesIdIndex != null && geoCoordinatesIdIndex < values.length) {
                        String geoCoordinatesIdStr = values[geoCoordinatesIdIndex].trim();
                        if (geoCoordinatesIdStr != null && !geoCoordinatesIdStr.isEmpty()) {
                            request.setGeoCoordinatesId(Long.parseLong(geoCoordinatesIdStr));
                        }
                    }

                    SuburbResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
                ? "Import completed successfully. " + success + " out of " + total + " suburbs imported."
                : "Import failed. No suburbs were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<Suburb> addToSpec(Specification<Suburb> spec,
                                           Function<EntityStatus, Specification<Suburb>> predicateMethod) {
        Specification<Suburb> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<Suburb> addToSpec(final String aString, Specification<Suburb> spec, Function<String,
            Specification<Suburb>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<Suburb> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<Suburb> addToSpec(final EntityStatus entityStatus, Specification<Suburb> spec, Function<EntityStatus,
            Specification<Suburb>> predicateMethod) {
        if (entityStatus != null) {
            Specification<Suburb> localSpec = Specification.where(predicateMethod.apply(entityStatus));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<SuburbDto> convertSuburbEntityToSuburbDto(Page<Suburb> suburbPage) {

        List<Suburb> suburbList = suburbPage.getContent();
        List<SuburbDto> suburbDtoList = new ArrayList<>();

        for (Suburb suburb : suburbPage) {
            SuburbDto suburbDto = modelMapper.map(suburb, SuburbDto.class);
            suburbDtoList.add(suburbDto);
        }

        int page = suburbPage.getNumber();
        int size = suburbPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableSuburbs = PageRequest.of(page, size);

        return new PageImpl<>(suburbDtoList, pageableSuburbs, suburbPage.getTotalElements());
    }

    private SuburbResponse buildSuburbResponse(int statusCode, boolean isSuccess, String message,
                                               SuburbDto suburbDto, List<SuburbDto> suburbDtoList,
                                                   Page<SuburbDto> suburbDtoPage) {
        SuburbResponse suburbResponse = new SuburbResponse();

        suburbResponse.setStatusCode(statusCode);
        suburbResponse.setSuccess(isSuccess);
        suburbResponse.setMessage(message);
        suburbResponse.setSuburbDto(suburbDto);
        suburbResponse.setSuburbDtoList(suburbDtoList);
        suburbResponse.setSuburbDtoPage(suburbDtoPage);

        return suburbResponse;
    }

    private SuburbResponse buildSuburbResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                       SuburbDto suburbDto, List<SuburbDto> suburbDtoList,
                                                       Page<SuburbDto> suburbDtoPage, List<String> errorMessages) {
        SuburbResponse suburbResponse = new SuburbResponse();

        suburbResponse.setStatusCode(statusCode);
        suburbResponse.setSuccess(isSuccess);
        suburbResponse.setMessage(message);
        suburbResponse.setSuburbDto(suburbDto);
        suburbResponse.setSuburbDtoList(suburbDtoList);
        suburbResponse.setSuburbDtoPage(suburbDtoPage);
        suburbResponse.setErrorMessages(errorMessages);

        return suburbResponse;
    }
}
