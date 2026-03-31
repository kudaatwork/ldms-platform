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
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.GeoCoordinatesServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.GeoCoordinatesService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.GeoCoordinatesServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.GeoCoordinatesSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.GeoCoordinatesDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.GeoCoordinatesCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.GeoCoordinatesMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.GeoCoordinatesResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GeoCoordinatesServiceImpl implements GeoCoordinatesService {

    private final GeoCoordinatesServiceValidator geoCoordinatesServiceValidator;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final GeoCoordinatesServiceAuditable geoCoordinatesServiceAuditable;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "LATITUDE", "LONGITUDE", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    private static final String[] CSV_HEADERS = {
            "LATITUDE", "LONGITUDE"
    };

    @Override
    public GeoCoordinatesResponse create(CreateGeoCoordinatesRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = geoCoordinatesServiceValidator.isCreateGeoCoordinatesRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildGeoCoordinatesResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        // Check if coordinates with the same latitude and longitude already exist
        List<GeoCoordinates> existingCoordinates = geoCoordinatesRepository.findAll().stream()
                .filter(gc -> gc.getEntityStatus() != EntityStatus.DELETED)
                .filter(gc -> gc.getLatitude().equals(request.getLatitude()) 
                        && gc.getLongitude().equals(request.getLongitude()))
                .collect(Collectors.toList());

        if (!existingCoordinates.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildGeoCoordinatesResponse(400, false, message, null,
                    null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        GeoCoordinates geoCoordinatesToBeSaved = modelMapper.map(request, GeoCoordinates.class);

        GeoCoordinates geoCoordinatesSaved = geoCoordinatesServiceAuditable.create(geoCoordinatesToBeSaved, locale, username);

        GeoCoordinatesDto geoCoordinatesDtoReturned = modelMapper.map(geoCoordinatesSaved, GeoCoordinatesDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildGeoCoordinatesResponse(201, true, message, geoCoordinatesDtoReturned, null,
                null);
    }

    @Override
    public GeoCoordinatesResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = geoCoordinatesServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildGeoCoordinatesResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<GeoCoordinates> geoCoordinatesRetrieved = geoCoordinatesRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (geoCoordinatesRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildGeoCoordinatesResponse(404, false, message, null, null,
                    null);
        }

        GeoCoordinates geoCoordinatesReturned = geoCoordinatesRetrieved.get();
        GeoCoordinatesDto geoCoordinatesDto = modelMapper.map(geoCoordinatesReturned, GeoCoordinatesDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildGeoCoordinatesResponse(200, true, message, geoCoordinatesDto, null,
                null);
    }

    @Override
    public GeoCoordinatesResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<GeoCoordinates> geoCoordinatesList = geoCoordinatesRepository.findAll().stream()
                .filter(geoCoordinates -> geoCoordinates.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        if(geoCoordinatesList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildGeoCoordinatesResponse(404, false, message, null,
                    null, null);
        }

        List<GeoCoordinatesDto> geoCoordinatesDtoList = modelMapper.map(geoCoordinatesList,
                new TypeToken<List<GeoCoordinatesDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildGeoCoordinatesResponse(200, true, message, null, geoCoordinatesDtoList,
                null);
    }

    @Override
    public GeoCoordinatesResponse update(EditGeoCoordinatesRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = geoCoordinatesServiceValidator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_COUNTRY_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildGeoCoordinatesResponseWithErrors(400, false, message, null,
                    null, null, validatorDto.getErrorMessages());
        }

        Optional<GeoCoordinates> geoCoordinatesRetrieved = geoCoordinatesRepository.findByIdAndEntityStatusNot(request.getId(),
                EntityStatus.DELETED);

        if (geoCoordinatesRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildGeoCoordinatesResponse(404, false, message, null, null,
                    null);
        }

        GeoCoordinates geoCoordinatesToBeEdited = geoCoordinatesRetrieved.get();

        // Update geo coordinates properties from request
        geoCoordinatesToBeEdited.setLatitude(request.getLatitude());
        geoCoordinatesToBeEdited.setLongitude(request.getLongitude());

        GeoCoordinates geoCoordinatesEdited = geoCoordinatesServiceAuditable.update(geoCoordinatesToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        GeoCoordinatesDto geoCoordinatesDtoReturned = modelMapper.map(geoCoordinatesEdited, GeoCoordinatesDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildGeoCoordinatesResponse(200, true, message, geoCoordinatesDtoReturned, null,
                null);
    }

    @Override
    public GeoCoordinatesResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = geoCoordinatesServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildGeoCoordinatesResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<GeoCoordinates> geoCoordinatesRetrieved = geoCoordinatesRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (geoCoordinatesRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildGeoCoordinatesResponse(404, false, message, null, null,
                    null);
        }

        GeoCoordinates geoCoordinatesToBeDeleted = geoCoordinatesRetrieved.get();
        geoCoordinatesToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        GeoCoordinates geoCoordinatesDeleted = geoCoordinatesServiceAuditable.delete(geoCoordinatesToBeDeleted, locale);

        GeoCoordinatesDto geoCoordinatesDtoReturned = modelMapper.map(geoCoordinatesDeleted, GeoCoordinatesDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildGeoCoordinatesResponse(200, true, message, geoCoordinatesDtoReturned,
                null, null);
    }

    @Override
    public GeoCoordinatesResponse findByMultipleFilters(GeoCoordinatesMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<GeoCoordinates> spec = null;
        spec = addToSpec(spec, GeoCoordinatesSpecification::deleted);

        ValidatorDto validatorDto = geoCoordinatesServiceValidator.isRequestValidToRetrieveGeoCoordinatesByMultipleFilters(
                request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_COUNTRY_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildGeoCoordinatesResponseWithErrors(400, false, message, null,
                    null, null, validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (request.getLatitude() != null) {
            spec = addToSpec(request.getLatitude(), spec, GeoCoordinatesSpecification::latitudeEquals);
        }

        if (request.getLongitude() != null) {
            spec = addToSpec(request.getLongitude(), spec, GeoCoordinatesSpecification::longitudeEquals);
        }

        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, GeoCoordinatesSpecification::hasEntityStatus);
        }

        Page<GeoCoordinates> result = geoCoordinatesRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildGeoCoordinatesResponse(404, false, message, null,
                    null, null);
        }

        Page<GeoCoordinatesDto> geoCoordinatesDtoPage = convertGeoCoordinatesEntityToGeoCoordinatesDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildGeoCoordinatesResponse(200, true, message, null,
                null, geoCoordinatesDtoPage);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<GeoCoordinatesDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (GeoCoordinatesDto geoCoordinates : items) {
            sb.append(geoCoordinates.getId()).append(",")
                    .append(geoCoordinates.getLatitude()).append(",")
                    .append(geoCoordinates.getLongitude()).append(",")
                    .append(geoCoordinates.getCreatedAt()).append(",")
                    .append(geoCoordinates.getUpdatedAt()).append(",")
                    .append(geoCoordinates.getEntityStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<GeoCoordinatesDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("GeoCoordinates");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (GeoCoordinatesDto geoCoordinates : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(geoCoordinates.getId());
            row.createCell(1).setCellValue(geoCoordinates.getLatitude() != null ? geoCoordinates.getLatitude().toString() : "");
            row.createCell(2).setCellValue(geoCoordinates.getLongitude() != null ? geoCoordinates.getLongitude().toString() : "");
            row.createCell(3).setCellValue(geoCoordinates.getCreatedAt() != null ? geoCoordinates.getCreatedAt().toString() : "");
            row.createCell(4).setCellValue(geoCoordinates.getUpdatedAt() != null ? geoCoordinates.getUpdatedAt().toString() : "");
            row.createCell(5).setCellValue(geoCoordinates.getEntityStatus() != null ? geoCoordinates.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<GeoCoordinatesDto> items) throws DocumentException {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("GEO COORDINATES EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (GeoCoordinatesDto geoCoordinates : items) {
            table.addCell(String.valueOf(geoCoordinates.getId()));
            table.addCell(geoCoordinates.getLatitude() != null ? geoCoordinates.getLatitude().toString() : "");
            table.addCell(geoCoordinates.getLongitude() != null ? geoCoordinates.getLongitude().toString() : "");
            table.addCell(geoCoordinates.getCreatedAt() != null ? geoCoordinates.getCreatedAt().toString() : "");
            table.addCell(geoCoordinates.getUpdatedAt() != null ? geoCoordinates.getUpdatedAt().toString() : "");
            table.addCell(geoCoordinates.getEntityStatus() != null ? geoCoordinates.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importGeoCoordinatesFromCsv(InputStream csvInputStream) throws IOException {

        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            // Configure CSV to Bean mapping
            HeaderColumnNameMappingStrategy<GeoCoordinatesCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(GeoCoordinatesCsvDto.class);

            CsvToBean<GeoCoordinatesCsvDto> csvToBean = new CsvToBeanBuilder<GeoCoordinatesCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            // Parse CSV to list of DTOs
            List<GeoCoordinatesCsvDto> geoCoordinatesList = csvToBean.parse();
            total = geoCoordinatesList.size();

            int rowNum = 1; // Start from 1 to account for header
            for (GeoCoordinatesCsvDto geoCoordinatesDto : geoCoordinatesList) {
                rowNum++;
                try {
                    if (geoCoordinatesDto.getLatitude() == null || geoCoordinatesDto.getLongitude() == null) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing latitude or longitude");
                        continue;
                    }

                    CreateGeoCoordinatesRequest request = new CreateGeoCoordinatesRequest();
                    request.setLatitude(geoCoordinatesDto.getLatitude());
                    request.setLongitude(geoCoordinatesDto.getLongitude());

                    GeoCoordinatesResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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

        // Determine status code and success flag based on import results
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " geo coordinates imported."
                : "Import failed. No geo coordinates were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private GeoCoordinatesResponse buildGeoCoordinatesResponse(int statusCode, boolean isSuccess, String message,
                                                 GeoCoordinatesDto geoCoordinatesDto, List<GeoCoordinatesDto> geoCoordinatesDtoList,
                                                 Page<GeoCoordinatesDto> geoCoordinatesDtoPage) {

        GeoCoordinatesResponse geoCoordinatesResponse = new GeoCoordinatesResponse();

        geoCoordinatesResponse.setStatusCode(statusCode);
        geoCoordinatesResponse.setSuccess(isSuccess);
        geoCoordinatesResponse.setMessage(message);
        geoCoordinatesResponse.setGeoCoordinatesDto(geoCoordinatesDto);
        geoCoordinatesResponse.setGeoCoordinatesDtoList(geoCoordinatesDtoList);
        geoCoordinatesResponse.setGeoCoordinatesDtoPage(geoCoordinatesDtoPage);

        return geoCoordinatesResponse;
    }

    private GeoCoordinatesResponse buildGeoCoordinatesResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                         GeoCoordinatesDto geoCoordinatesDto, List<GeoCoordinatesDto> geoCoordinatesDtoList,
                                                         Page<GeoCoordinatesDto> geoCoordinatesDtoPage, List<String> errorMessages) {

        GeoCoordinatesResponse geoCoordinatesResponse = new GeoCoordinatesResponse();

        geoCoordinatesResponse.setStatusCode(statusCode);
        geoCoordinatesResponse.setSuccess(isSuccess);
        geoCoordinatesResponse.setMessage(message);
        geoCoordinatesResponse.setGeoCoordinatesDto(geoCoordinatesDto);
        geoCoordinatesResponse.setGeoCoordinatesDtoList(geoCoordinatesDtoList);
        geoCoordinatesResponse.setGeoCoordinatesDtoPage(geoCoordinatesDtoPage);
        geoCoordinatesResponse.setErrorMessages(errorMessages);

        return geoCoordinatesResponse;
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<GeoCoordinates> addToSpec(Specification<GeoCoordinates> spec,
                                           Function<EntityStatus, Specification<GeoCoordinates>> predicateMethod) {
        Specification<GeoCoordinates> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<GeoCoordinates> addToSpec(final String aString, Specification<GeoCoordinates> spec, Function<String,
            Specification<GeoCoordinates>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<GeoCoordinates> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<GeoCoordinates> addToSpec(final EntityStatus entityStatus, Specification<GeoCoordinates> spec, Function<EntityStatus,
            Specification<GeoCoordinates>> predicateMethod) {
        if (entityStatus != null) {
            Specification<GeoCoordinates> localSpec = Specification.where(predicateMethod.apply(entityStatus));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<GeoCoordinates> addToSpec(final BigDecimal value, Specification<GeoCoordinates> spec, Function<BigDecimal,
            Specification<GeoCoordinates>> predicateMethod) {
        if (value != null) {
            Specification<GeoCoordinates> localSpec = Specification.where(predicateMethod.apply(value));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<GeoCoordinatesDto> convertGeoCoordinatesEntityToGeoCoordinatesDto(Page<GeoCoordinates> geoCoordinatesPage) {

        List<GeoCoordinates> geoCoordinatesList = geoCoordinatesPage.getContent();
        List<GeoCoordinatesDto> geoCoordinatesDtoList = new ArrayList<>();

        for (GeoCoordinates geoCoordinates : geoCoordinatesPage) {
            GeoCoordinatesDto geoCoordinatesDto = modelMapper.map(geoCoordinates, GeoCoordinatesDto.class);
            geoCoordinatesDtoList.add(geoCoordinatesDto);
        }

        int page = geoCoordinatesPage.getNumber();
        int size = geoCoordinatesPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableGeoCoordinates = PageRequest.of(page, size);

        return new PageImpl<>(geoCoordinatesDtoList, pageableGeoCoordinates, geoCoordinatesPage.getTotalElements());
    }
}
