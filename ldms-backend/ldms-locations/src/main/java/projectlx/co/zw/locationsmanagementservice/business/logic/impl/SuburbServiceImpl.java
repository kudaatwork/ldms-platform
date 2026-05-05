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
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.SuburbServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.SuburbService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.SuburbServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.model.LocalizedName;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.repository.AdministrativeLevelRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.SuburbSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.GeoCoordinateCsvImportHelper;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.SuburbCsvDto;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SuburbServiceImpl implements SuburbService {

    private final SuburbServiceValidator suburbServiceValidator;
    private final SuburbRepository suburbRepository;
    private final DistrictRepository districtRepository;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final AdministrativeLevelRepository administrativeLevelRepository;
    private final SuburbServiceAuditable suburbServiceAuditable;
    private final LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "NAME", "CODE", "DISTRICT", "PROVINCE", "COUNTRY", "ADMIN LEVEL", "DISTRICT ID", "ADMINISTRATIVE LEVEL ID",
            "POSTAL CODE", "GEO COORDINATES ID", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
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

        // Validate district exists (uniqueness of suburb name is scoped to district)
        Optional<District> districtRetrieved =
                districtRepository.findById(request.getDistrictId());

        if (districtRetrieved.isEmpty() || districtRetrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildSuburbResponse(400, false, message, null,
                    null, null);
        }

        District district = districtRetrieved.get();
        String normalizedName = Validators.capitalizeWords(request.getName());

        Optional<Suburb> suburbActiveInDistrict = suburbRepository
                .findByNameAndDistrictAndEntityStatusNot(normalizedName, district, EntityStatus.DELETED);

        if (suburbActiveInDistrict.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_SUBURB_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildSuburbResponse(400, false, message, null,
                    null, null);
        }

        Optional<Suburb> deletedSuburb = suburbRepository.findByNameAndDistrict(normalizedName, district)
                .filter(suburb -> suburb.getEntityStatus() == EntityStatus.DELETED);

        request.setName(normalizedName);
        Suburb suburbToBeSaved = deletedSuburb.orElseGet(Suburb::new);
        applyCreateRequestToSuburb(suburbToBeSaved, request);

        // Set the district
        suburbToBeSaved.setDistrict(district);
        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildSuburbResponse(400, false, message, null, null, null);
        }

        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildSuburbResponse(400, false, message, null, null, null);
        }
        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                request.getGeoCoordinatesId(), request.getLatitude(), request.getLongitude());
        if (request.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildSuburbResponse(400, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
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

        // Ensure ACTIVE for new inserts and reactivations (ModelMapper / lifecycle edge cases).
        suburbToBeSaved.setEntityStatus(EntityStatus.ACTIVE);

        Suburb suburbSaved;
        try {
            suburbSaved = deletedSuburb.isPresent()
                    ? suburbServiceAuditable.update(suburbToBeSaved, locale, username)
                    : suburbServiceAuditable.create(suburbToBeSaved, locale, username);
        } catch (OptimisticLockingFailureException ex) {
            // Defensive fallback for stale soft-deleted references during bulk imports:
            // insert as a fresh row instead of failing the whole line with stale update errors.
            Suburb fallbackInsert = new Suburb();
            applyCreateRequestToSuburb(fallbackInsert, request);
            fallbackInsert.setDistrict(district);
            fallbackInsert.setGeoCoordinates(suburbToBeSaved.getGeoCoordinates());
            fallbackInsert.setAdministrativeLevel(suburbToBeSaved.getAdministrativeLevel());
            fallbackInsert.setEntityStatus(EntityStatus.ACTIVE);
            suburbSaved = suburbServiceAuditable.create(fallbackInsert, locale, username);
        }

        SuburbDto suburbDtoReturned = toSuburbDto(suburbSaved);

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
        SuburbDto suburbDto = toSuburbDto(suburbReturned);

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

        List<SuburbDto> suburbDtoList = suburbList.stream().map(this::toSuburbDto).collect(Collectors.toList());

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

        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                request.getGeoCoordinatesId(), request.getLatitude(), request.getLongitude());
        if (request.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildSuburbResponse(400, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
            suburbToBeEdited.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        // Handle administrative level if provided
        if (request.getAdministrativeLevelId() != null) {
            Optional<AdministrativeLevel> administrativeLevelOptional = administrativeLevelRepository.findById(request.getAdministrativeLevelId());
            if (administrativeLevelOptional.isPresent()) {
                suburbToBeEdited.setAdministrativeLevel(administrativeLevelOptional.get());
            }
        }

        Suburb suburbEdited = suburbServiceAuditable.update(suburbToBeEdited, locale, username);

        SuburbDto suburbDtoReturned = toSuburbDto(suburbEdited);

        message = messageService.getMessage(I18Code.MESSAGE_SUBURB_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildSuburbResponse(200, true, message, suburbDtoReturned, null,
                null);
    }

    @Override
    @Transactional
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

        locationHierarchyCascadeSoftDeleteService.cascadeBeforeDeletingSuburb(suburbToBeDeleted.getId(), locale, username);

        suburbToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        Suburb suburbDeleted = suburbServiceAuditable.delete(suburbToBeDeleted, locale, username);

        SuburbDto suburbDtoReturned = toSuburbDto(suburbDeleted);

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

        ValidatorDto validatorDto = suburbServiceValidator.isRequestValidToRetrieveSuburbsByMultipleFilters(
                request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildSuburbResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "id"));

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

        if (request.getDistrictId() != null) {
            Specification<Suburb> byDistrict = SuburbSpecification.byDistrict(request.getDistrictId());
            spec = (spec == null) ? byDistrict : spec.and(byDistrict);
        }

        if (request.getAdministrativeLevelId() != null) {
            Specification<Suburb> byLevel = SuburbSpecification.byAdministrativeLevel(request.getAdministrativeLevelId());
            spec = (spec == null) ? byLevel : spec.and(byLevel);
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
                    .append(safe(suburb.getDistrictName())).append(",")
                    .append(safe(suburb.getProvinceName())).append(",")
                    .append(safe(suburb.getCountryName())).append(",")
                    .append(safe(suburb.getAdministrativeLevelName())).append(",")
                    .append(suburb.getDistrictId() != null ? suburb.getDistrictId() : "").append(",")
                    .append(suburb.getAdministrativeLevelId() != null ? suburb.getAdministrativeLevelId() : "").append(",")
                    .append(safe(suburb.getPostalCode())).append(",")
                    .append(suburb.getGeoCoordinatesId() != null ? suburb.getGeoCoordinatesId() : "").append(",")
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
            row.createCell(3).setCellValue(safe(suburb.getDistrictName()));
            row.createCell(4).setCellValue(safe(suburb.getProvinceName()));
            row.createCell(5).setCellValue(safe(suburb.getCountryName()));
            row.createCell(6).setCellValue(safe(suburb.getAdministrativeLevelName()));
            row.createCell(7).setCellValue(suburb.getDistrictId() != null ? suburb.getDistrictId() : 0);
            row.createCell(8).setCellValue(suburb.getAdministrativeLevelId() != null ? suburb.getAdministrativeLevelId() : 0);
            row.createCell(9).setCellValue(safe(suburb.getPostalCode()));
            row.createCell(10).setCellValue(suburb.getGeoCoordinatesId() != null ? suburb.getGeoCoordinatesId() : 0);
            row.createCell(11).setCellValue(suburb.getCreatedAt() != null ? suburb.getCreatedAt().toString() : "");
            row.createCell(12).setCellValue(suburb.getUpdatedAt() != null ? suburb.getUpdatedAt().toString() : "");
            row.createCell(13).setCellValue(suburb.getEntityStatus() != null ? suburb.getEntityStatus().toString() : "");
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
            table.addCell(safe(suburb.getDistrictName()));
            table.addCell(safe(suburb.getProvinceName()));
            table.addCell(safe(suburb.getCountryName()));
            table.addCell(safe(suburb.getAdministrativeLevelName()));
            table.addCell(suburb.getDistrictId() != null ? suburb.getDistrictId().toString() : "");
            table.addCell(suburb.getAdministrativeLevelId() != null ? suburb.getAdministrativeLevelId().toString() : "");
            table.addCell(safe(suburb.getPostalCode()));
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

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<SuburbCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(SuburbCsvDto.class);

            CsvToBean<SuburbCsvDto> csvToBean = new CsvToBeanBuilder<SuburbCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<SuburbCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (SuburbCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getName() == null || row.getName().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing suburb name");
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

                    CreateSuburbRequest request = new CreateSuburbRequest();
                    request.setName(row.getName());
                    request.setCode(row.getCode());
                    request.setPostalCode(row.getPostalCode());
                    request.setDistrictId(row.getDistrictId());
                    request.setAdministrativeLevelId(row.getAdministrativeLevelId());
                    GeoCoordinateCsvImportHelper.ResolvedGeo geo = GeoCoordinateCsvImportHelper.resolve(
                            row.getGeoCoordinatesId(),
                            row.getLatitude(),
                            row.getLongitude(),
                            districtOpt.get().getGeoCoordinates());
                    request.setGeoCoordinatesId(geo.geoCoordinatesId());
                    request.setLatitude(geo.latitude());
                    request.setLongitude(geo.longitude());

                    SuburbResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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

    /**
     * Use explicit field mapping so request id-like fields (districtId, administrativeLevelId) can never
     * leak into the entity primary key through loose ModelMapper conventions.
     */
    private void applyCreateRequestToSuburb(Suburb suburb, CreateSuburbRequest request) {
        suburb.setName(request.getName());
        suburb.setCode(request.getCode());
        suburb.setPostalCode(request.getPostalCode());
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

    private SuburbDto toSuburbDto(Suburb entity) {
        SuburbDto dto = new SuburbDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setPostalCode(entity.getPostalCode());
        dto.setGeoCoordinatesId(entity.getGeoCoordinates() != null ? entity.getGeoCoordinates().getId() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setEntityStatus(entity.getEntityStatus() != null ? entity.getEntityStatus() : EntityStatus.ACTIVE);
        if (entity.getDistrict() != null) {
            dto.setDistrictId(entity.getDistrict().getId());
            dto.setDistrictName(entity.getDistrict().getName());
            Province p = entity.getDistrict().getProvince();
            if (p != null) {
                dto.setProvinceId(p.getId());
                dto.setProvinceName(p.getName());
                if (p.getCountry() != null) {
                    dto.setCountryId(p.getCountry().getId());
                    dto.setCountryName(p.getCountry().getName());
                }
            }
        }
        if (entity.getAdministrativeLevel() != null) {
            dto.setAdministrativeLevelId(entity.getAdministrativeLevel().getId());
            dto.setAdministrativeLevelName(entity.getAdministrativeLevel().getName());
        }
        if (entity.getLocalizedNames() != null && Hibernate.isInitialized(entity.getLocalizedNames())) {
            dto.setLocalizedNameIds(entity.getLocalizedNames().stream().map(LocalizedName::getId).collect(Collectors.toList()));
        }
        return dto;
    }

    private Page<SuburbDto> convertSuburbEntityToSuburbDto(Page<Suburb> suburbPage) {

        List<Suburb> suburbList = suburbPage.getContent();
        List<SuburbDto> suburbDtoList = new ArrayList<>();

        for (Suburb suburb : suburbPage) {
            suburbDtoList.add(toSuburbDto(suburb));
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
