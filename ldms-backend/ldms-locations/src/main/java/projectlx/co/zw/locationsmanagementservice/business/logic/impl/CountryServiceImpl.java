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
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
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
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.CountryServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.CountryService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.CountryServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.repository.CountryRepository;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.CountrySpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CountryDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CountryCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CountryMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CountryResponse;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryServiceValidator countryServiceValidator;
    private final CountryRepository countryRepository;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final CountryServiceAuditable countryServiceAuditable;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "NAME", "ISO ALPHA-2 CODE", "ISO ALPHA-3 CODE", "DIAL CODE", "TIMEZONE", "CURRENCY CODE", "CREATED AT", "UPDATED AT", "ENTITY STATUS", "GEOCOORDINATES ID"
    };

    private static final String[] CSV_HEADERS = {
            "NAME", "ISO ALPHA-2 CODE", "ISO ALPHA-3 CODE", "DIAL CODE", "TIMEZONE", "CURRENCY CODE", "GEOCOORDINATES ID"
    };


    @Override
    public CountryResponse create(CreateCountryRequest createCountryRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = countryServiceValidator.isCreateCountryRequestValid(createCountryRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildCountryResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        String normalizedName = Validators.capitalizeWords(createCountryRequest.getName().trim());
        String normalizedIsoAlpha2Code = createCountryRequest.getIsoAlpha2Code().trim().toUpperCase();
        String normalizedIsoAlpha3Code = createCountryRequest.getIsoAlpha3Code().trim().toUpperCase();

        createCountryRequest.setName(normalizedName);
        createCountryRequest.setIsoAlpha2Code(normalizedIsoAlpha2Code);
        createCountryRequest.setIsoAlpha3Code(normalizedIsoAlpha3Code);

        Optional<Country> activeByName = countryRepository.findByNameAndEntityStatusNot(normalizedName, EntityStatus.DELETED);
        Optional<Country> activeByIso2 = countryRepository.findByIsoAlpha2CodeAndEntityStatusNot(normalizedIsoAlpha2Code, EntityStatus.DELETED);
        Optional<Country> activeByIso3 = countryRepository.findByIsoAlpha3CodeAndEntityStatusNot(normalizedIsoAlpha3Code, EntityStatus.DELETED);

        if (activeByName.isPresent() || activeByIso2.isPresent() || activeByIso3.isPresent()) {
            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);
            return buildCountryResponse(400, false, message, null,
                    null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        if ((createCountryRequest.getLatitude() == null) != (createCountryRequest.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildCountryResponse(400, false, message, null, null, null);
        }

        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                createCountryRequest.getGeoCoordinatesId(),
                createCountryRequest.getLatitude(),
                createCountryRequest.getLongitude());
        if (createCountryRequest.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildCountryResponse(400, false, message, null, null, null);
        }
        Optional<Country> deletedByName = countryRepository.findByName(normalizedName)
                .filter(country -> country.getEntityStatus() == EntityStatus.DELETED);
        Optional<Country> deletedByIso2 = countryRepository.findByIsoAlpha2Code(normalizedIsoAlpha2Code)
                .filter(country -> country.getEntityStatus() == EntityStatus.DELETED);
        Optional<Country> deletedByIso3 = countryRepository.findByIsoAlpha3Code(normalizedIsoAlpha3Code)
                .filter(country -> country.getEntityStatus() == EntityStatus.DELETED);

        Country countrySaved;
        Optional<Country> deletedCountryOptional = deletedByName.isPresent() ? deletedByName
                : deletedByIso2.isPresent() ? deletedByIso2
                : deletedByIso3;

        if (deletedCountryOptional.isPresent()) {
            Country countryToBeReactivated = deletedCountryOptional.get();
            countryToBeReactivated.setName(normalizedName);
            countryToBeReactivated.setIsoAlpha2Code(normalizedIsoAlpha2Code);
            countryToBeReactivated.setIsoAlpha3Code(normalizedIsoAlpha3Code);
            countryToBeReactivated.setDialCode(createCountryRequest.getDialCode());
            countryToBeReactivated.setTimezone(createCountryRequest.getTimezone());
            countryToBeReactivated.setCurrencyCode(createCountryRequest.getCurrencyCode());
            countryToBeReactivated.setEntityStatus(EntityStatus.ACTIVE);
            if (geoCoordinatesOptional.isPresent()) {
                countryToBeReactivated.setGeoCoordinates(geoCoordinatesOptional.get());
            }
            countrySaved = countryServiceAuditable.update(countryToBeReactivated, locale, username);
        } else {
            Country countryToBeSaved = modelMapper.map(createCountryRequest, Country.class);
            if (geoCoordinatesOptional.isPresent()) {
                countryToBeSaved.setGeoCoordinates(geoCoordinatesOptional.get());
            }
            countrySaved = countryServiceAuditable.create(countryToBeSaved, locale, username);
        }

        CountryDto countryDtoReturned = modelMapper.map(countrySaved, CountryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildCountryResponse(201, true, message, countryDtoReturned, null,
                null);
    }

    @Override
    public CountryResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = countryServiceValidator.isIdValid(id, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildCountryResponseWithErrors(400, false, message, null, null,
                    null, validatorDto != null ? validatorDto.getErrorMessages() : new ArrayList<>());
        }

        Optional<Country> countryRetrieved = countryRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (countryRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildCountryResponse(404, false, message, null, null,
                    null);
        }

        Country countryReturned = countryRetrieved.get();
        CountryDto countryDto = modelMapper.map(countryReturned, CountryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildCountryResponse(200, true, message, countryDto, null,
                null);
    }

    @Override
    public CountryResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<Country> countryList = countryRepository.findAll().stream()
                .filter(country -> country.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        if(countryList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildCountryResponse(404, false, message, null,
                    null, null);
        }

        List<CountryDto> countryDtoList = modelMapper.map(countryList, new TypeToken<List<CountryDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildCountryResponse(200, true, message, null, countryDtoList,
                null);
    }

    @Override
    public CountryResponse update(EditCountryRequest editCountryRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = countryServiceValidator.isRequestValidForEditing(editCountryRequest, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_COUNTRY_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildCountryResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Country> countryRetrieved = countryRepository.findByIdAndEntityStatusNot(editCountryRequest.getId(),
                EntityStatus.DELETED);

        if (countryRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildCountryResponse(404, false, message, null, null,
                    null);
        }

        Country countryToBeEdited = countryRetrieved.get();

        // Update country properties from request
        countryToBeEdited.setName(Validators.capitalizeWords(editCountryRequest.getName()));
        countryToBeEdited.setIsoAlpha2Code(editCountryRequest.getIsoAlpha2Code());
        countryToBeEdited.setIsoAlpha3Code(editCountryRequest.getIsoAlpha3Code());
        countryToBeEdited.setDialCode(editCountryRequest.getDialCode());
        countryToBeEdited.setTimezone(editCountryRequest.getTimezone());
        countryToBeEdited.setCurrencyCode(editCountryRequest.getCurrencyCode());

        if ((editCountryRequest.getLatitude() == null) != (editCountryRequest.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildCountryResponse(400, false, message, null, null, null);
        }
        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                editCountryRequest.getGeoCoordinatesId(),
                editCountryRequest.getLatitude(),
                editCountryRequest.getLongitude());
        if (editCountryRequest.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildCountryResponse(400, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
            countryToBeEdited.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        Country countryEdited = countryServiceAuditable.update(countryToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        CountryDto countryDtoReturned = modelMapper.map(countryEdited, CountryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildCountryResponse(200, true, message, countryDtoReturned, null,
                null);
    }

    @Override
    public CountryResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = countryServiceValidator.isIdValid(id, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildCountryResponseWithErrors(400, false, message, null, null,
                    null, validatorDto != null ? validatorDto.getErrorMessages() : new ArrayList<>());
        }

        Optional<Country> countryRetrieved = countryRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (countryRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildCountryResponse(404, false, message, null, null,
                    null);
        }

        Country countryToBeDeleted = countryRetrieved.get();
        countryToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        Country countryDeleted = countryServiceAuditable.delete(countryToBeDeleted, locale, username);

        CountryDto countryDtoReturned = modelMapper.map(countryDeleted, CountryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildCountryResponse(200, true, message, countryDtoReturned, null,
                null);
    }

    @Override
    public CountryResponse findByMultipleFilters(CountryMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<Country> spec = null;
        spec = addToSpec(spec, CountrySpecification::deleted);

        ValidatorDto validatorDto = countryServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(
                request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_COUNTRY_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);


            return buildCountryResponseWithErrors(400, false, message, null, null, null, 
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (isNotEmpty(request.getName())) {
            spec = addToSpec(request.getName(), spec, CountrySpecification::nameLike);
        }

        if (isNotEmpty(request.getIsoAlpha2Code())) {
            spec = addToSpec(request.getIsoAlpha2Code(), spec, CountrySpecification::isoAlpha2CodeLike);
        }

        if (isNotEmpty(request.getIsoAlpha3Code())) {
            spec = addToSpec(request.getIsoAlpha3Code(), spec, CountrySpecification::isoAlpha3CodeLike);
        }

        if (isNotEmpty(request.getDialCode())) {
            spec = addToSpec(request.getDialCode(), spec, CountrySpecification::dialCodeLike);
        }

        if (isNotEmpty(request.getTimezone())) {
            spec = addToSpec(request.getTimezone(), spec, CountrySpecification::timezoneLike);
        }

        if (isNotEmpty(request.getCurrencyCode())) {
            spec = addToSpec(request.getCurrencyCode(), spec, CountrySpecification::currencyCodeLike);
        }

        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, CountrySpecification::any);
        }

        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, CountrySpecification::hasEntityStatus);
        }

        Page<Country> result = countryRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildCountryResponse(404, false, message, null, null, null);
        }

        Page<CountryDto> countryDtoPage = convertCountryEntityToCountryDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_COUNTRY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildCountryResponse(200, true, message, null, null, countryDtoPage);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
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

    @Override
    public byte[] exportToCsv(List<CountryDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (CountryDto country : items) {
            sb.append(country.getId()).append(",")
                    .append(safe(country.getName())).append(",")
                    .append(safe(country.getIsoAlpha2Code())).append(",")
                    .append(safe(country.getIsoAlpha3Code())).append(",")
                    .append(safe(country.getDialCode())).append(",")
                    .append(safe(country.getTimezone())).append(",")
                    .append(safe(country.getCurrencyCode())).append(",")
                    .append(country.getCreatedAt()).append(",")
                    .append(country.getUpdatedAt()).append(",")
                    .append(country.getEntityStatus()).append(",")
                    .append(country.getGeoCoordinatesId() != null ? country.getGeoCoordinatesId() : "").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<CountryDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Countries");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (CountryDto country : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(country.getId());
            row.createCell(1).setCellValue(safe(country.getName()));
            row.createCell(2).setCellValue(safe(country.getIsoAlpha2Code()));
            row.createCell(3).setCellValue(safe(country.getIsoAlpha3Code()));
            row.createCell(4).setCellValue(safe(country.getDialCode()));
            row.createCell(5).setCellValue(safe(country.getTimezone()));
            row.createCell(6).setCellValue(safe(country.getCurrencyCode()));
            row.createCell(7).setCellValue(country.getCreatedAt() != null ? country.getCreatedAt().toString() : "");
            row.createCell(8).setCellValue(country.getUpdatedAt() != null ? country.getUpdatedAt().toString() : "");
            row.createCell(9).setCellValue(country.getEntityStatus() != null ? country.getEntityStatus().toString() : "");
            row.createCell(10).setCellValue(country.getGeoCoordinatesId() != null ? country.getGeoCoordinatesId() : 0L);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<CountryDto> items) throws DocumentException {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("COUNTRY EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (CountryDto country : items) {
            table.addCell(String.valueOf(country.getId()));
            table.addCell(safe(country.getName()));
            table.addCell(safe(country.getIsoAlpha2Code()));
            table.addCell(safe(country.getIsoAlpha3Code()));
            table.addCell(safe(country.getDialCode()));
            table.addCell(safe(country.getTimezone()));
            table.addCell(safe(country.getCurrencyCode()));
            table.addCell(country.getCreatedAt() != null ? country.getCreatedAt().toString() : "");
            table.addCell(country.getUpdatedAt() != null ? country.getUpdatedAt().toString() : "");
            table.addCell(country.getEntityStatus() != null ? country.getEntityStatus().toString() : "");
            table.addCell(country.getGeoCoordinatesId() != null ? country.getGeoCoordinatesId().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importCountryFromCsv(InputStream csvInputStream) throws IOException {

        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            // Configure CSV to Bean mapping
            HeaderColumnNameMappingStrategy<CountryCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(CountryCsvDto.class);

            CsvToBean<CountryCsvDto> csvToBean = new CsvToBeanBuilder<CountryCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            // Parse CSV to list of DTOs
            List<CountryCsvDto> countriesList = csvToBean.parse();
            total = countriesList.size();

            int rowNum = 1; // Start from 1 to account for header
            for (CountryCsvDto countryDto : countriesList) {
                rowNum++;
                try {
                    if (countryDto.getName() == null || countryDto.getName().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing country name");
                        continue;
                    }

                    CreateCountryRequest request = new CreateCountryRequest();
                    request.setName(countryDto.getName());
                    request.setIsoAlpha2Code(countryDto.getIsoAlpha2Code());
                    request.setIsoAlpha3Code(countryDto.getIsoAlpha3Code());
                    request.setDialCode(countryDto.getDialCode());
                    request.setTimezone(countryDto.getTimezone());
                    request.setCurrencyCode(countryDto.getCurrencyCode());
                    request.setGeoCoordinatesId(countryDto.getGeoCoordinatesId());

                    CountryResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
                ? "Import completed successfully. " + success + " out of " + total + " countries imported."
                : "Import failed. No countries were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<Country> addToSpec(Specification<Country> spec,
                                           Function<EntityStatus, Specification<Country>> predicateMethod) {
        Specification<Country> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<Country> addToSpec(final String aString, Specification<Country> spec, Function<String,
            Specification<Country>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<Country> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<Country> addToSpec(final EntityStatus entityStatus, Specification<Country> spec, Function<EntityStatus,
            Specification<Country>> predicateMethod) {
        if (entityStatus != null) {
            Specification<Country> localSpec = Specification.where(predicateMethod.apply(entityStatus));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<CountryDto> convertCountryEntityToCountryDto(Page<Country> countryPage) {

        List<Country> countryList = countryPage.getContent();
        List<CountryDto> countryDtoList = new ArrayList<>();

        for (Country country : countryPage) {
            CountryDto countryDto = modelMapper.map(country, CountryDto.class);
            countryDtoList.add(countryDto);
        }

        int page = countryPage.getNumber();
        int size = countryPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableCountries = PageRequest.of(page, size);

        return new PageImpl<>(countryDtoList, pageableCountries, countryPage.getTotalElements());
    }

    private CountryResponse buildCountryResponse(int statusCode, boolean isSuccess, String message,
                                                 CountryDto countryDto, List<CountryDto> countryDtoList,
                                                     Page<CountryDto> countryDtoPage) {

        CountryResponse countryResponse = new CountryResponse();

        countryResponse.setStatusCode(statusCode);
        countryResponse.setSuccess(isSuccess);
        countryResponse.setMessage(message);
        countryResponse.setCountryDto(countryDto);
        countryResponse.setCountryDtoList(countryDtoList);
        countryResponse.setCountryDtoPage(countryDtoPage);

        return countryResponse;
    }

    private CountryResponse buildCountryResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                         CountryDto countryDto, List<CountryDto> countryDtoList,
                                                         Page<CountryDto> countryDtoPage,List<String> errorMessages) {

        CountryResponse countryResponse = new CountryResponse();

        countryResponse.setStatusCode(statusCode);
        countryResponse.setSuccess(isSuccess);
        countryResponse.setMessage(message);
        countryResponse.setCountryDto(countryDto);
        countryResponse.setCountryDtoList(countryDtoList);
        countryResponse.setCountryDtoPage(countryDtoPage);
        countryResponse.setErrorMessages(errorMessages);

        return countryResponse;
    }
}
