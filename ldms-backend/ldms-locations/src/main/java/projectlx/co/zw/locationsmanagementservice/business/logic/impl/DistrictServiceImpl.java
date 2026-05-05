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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.DistrictServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.DistrictService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.DistrictServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.model.LocalizedName;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.locationsmanagementservice.repository.AdministrativeLevelRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.ProvinceRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.DistrictSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.GeoCoordinateCsvImportHelper;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.DistrictCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.DistrictDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.DistrictMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.DistrictResponse;
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
public class DistrictServiceImpl implements DistrictService {

    private final DistrictServiceValidator districtServiceValidator;
    private final DistrictRepository districtRepository;
    private final ProvinceRepository provinceRepository;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final AdministrativeLevelRepository administrativeLevelRepository;
    private final DistrictServiceAuditable districtServiceAuditable;
    private final LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "NAME", "CODE", "PROVINCE", "COUNTRY", "ADMIN LEVEL", "PROVINCE ID", "ADMINISTRATIVE LEVEL ID",
            "GEO COORDINATES ID", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    private static final String[] CSV_HEADERS = {
            "NAME", "CODE", "PROVINCE ID", "ADMINISTRATIVE LEVEL ID", "GEO COORDINATES ID"
    };

    @Override
    public DistrictResponse create(CreateDistrictRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = districtServiceValidator.isCreateDistrictRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_DISTRICT_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildDistrictResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Province> provinceRetrieved =
                provinceRepository.findByIdAndEntityStatusNot(request.getProvinceId(), EntityStatus.DELETED);

        if (provinceRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PROVINCE_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildDistrictResponse(400, false, message, null,
                    null, null);
        }

        Province province = provinceRetrieved.get();
        String normalizedName = Validators.capitalizeWords(request.getName());
        Optional<District> districtActiveInProvince =
                districtRepository.findByNameAndProvinceAndEntityStatusNot(normalizedName, province, EntityStatus.DELETED);

        if (districtActiveInProvince.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildDistrictResponse(400, false, message, null,
                    null, null);
        }

        Optional<District> deletedDistrict = districtRepository.findByNameAndProvince(normalizedName, province)
                .filter(district -> district.getEntityStatus() == EntityStatus.DELETED);

        request.setName(normalizedName);
        District districtToBeSaved = deletedDistrict.orElseGet(District::new);
        if (deletedDistrict.isPresent()) {
            modelMapper.map(request, districtToBeSaved);
        } else {
            districtToBeSaved = modelMapper.map(request, District.class);
        }

        // Set the province
        districtToBeSaved.setProvince(province);
        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildDistrictResponse(400, false, message, null, null, null);
        }

        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildDistrictResponse(400, false, message, null, null, null);
        }
        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                request.getGeoCoordinatesId(), request.getLatitude(), request.getLongitude());
        if (request.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildDistrictResponse(400, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
            districtToBeSaved.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        if (request.getAdministrativeLevelId() != null) {

            Optional<AdministrativeLevel> administrativeLevelOptional = administrativeLevelRepository
                    .findByIdAndEntityStatusNot(request.getAdministrativeLevelId(), EntityStatus.DELETED);

            if (administrativeLevelOptional.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(), new String[]{},
                        locale);

                return buildDistrictResponse(400, false, message, null,
                        null, null);
            }

            districtToBeSaved.setAdministrativeLevel(administrativeLevelOptional.get());
        }

        if (deletedDistrict.isPresent()) {
            districtToBeSaved.setEntityStatus(EntityStatus.ACTIVE);
        }
        District districtSaved = deletedDistrict.isPresent()
                ? districtServiceAuditable.update(districtToBeSaved, locale, username)
                : districtServiceAuditable.create(districtToBeSaved, locale, username);

        DistrictDto districtDtoReturned = toDistrictDto(districtSaved);

        message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildDistrictResponse(201, true, message, districtDtoReturned, null,
                null);
    }

    @Override
    public DistrictResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = districtServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildDistrictResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<District> districtRetrieved = districtRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (districtRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildDistrictResponse(404, false, message, null, null,
                    null);
        }

        District districtReturned = districtRetrieved.get();
        DistrictDto districtDto = toDistrictDto(districtReturned);

        message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildDistrictResponse(200, true, message, districtDto, null,
                null);
    }

    @Override
    public DistrictResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<District> districtList = districtRepository.findAllByEntityStatusNot(EntityStatus.DELETED);

        if(districtList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildDistrictResponse(404, false, message, null,
                    null, null);
        }

        List<DistrictDto> districtDtoList = districtList.stream().map(this::toDistrictDto).collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildDistrictResponse(200, true, message, null, districtDtoList,
                null);
    }

    @Override
    public DistrictResponse update(EditDistrictRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = districtServiceValidator.isRequestValidForEditing(request, locale);

        if(!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_DISTRICT_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildDistrictResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<District> districtRetrieved = districtRepository.findByIdAndEntityStatusNot(request.getId(),
                EntityStatus.DELETED);

        if (districtRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildDistrictResponse(404, false, message, null, null,
                    null);
        }

        District districtToBeEdited = districtRetrieved.get();

        // Update district properties from request
        districtToBeEdited.setName(Validators.capitalizeWords(request.getName()));
        districtToBeEdited.setCode(request.getCode());

        // Handle province if provided
        if (request.getProvinceId() != null) {
            Optional<Province> provinceOptional = provinceRepository.findByIdAndEntityStatusNot(request.getProvinceId(),
                    EntityStatus.DELETED);
            if (provinceOptional.isPresent()) {
                districtToBeEdited.setProvince(provinceOptional.get());
            }
        }

        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                request.getGeoCoordinatesId(), request.getLatitude(), request.getLongitude());
        if (request.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildDistrictResponse(400, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
            districtToBeEdited.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        if (request.getAdministrativeLevelId() != null) {

            Optional<AdministrativeLevel> administrativeLevelOptional = administrativeLevelRepository
                    .findByIdAndEntityStatusNot(request.getAdministrativeLevelId(), EntityStatus.DELETED);

            if (administrativeLevelOptional.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_ADMINISTRATIVE_LEVEL_NOT_FOUND.getCode(), new String[]{},
                        locale);

                return buildDistrictResponse(400, false, message, null,
                        null, null);
            }

            districtToBeEdited.setAdministrativeLevel(administrativeLevelOptional.get());
        }

        District districtEdited = districtServiceAuditable.update(districtToBeEdited, locale, username);

        DistrictDto districtDtoReturned = toDistrictDto(districtEdited);

        message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildDistrictResponse(200, true, message, districtDtoReturned, null,
                null);
    }

    @Override
    @Transactional
    public DistrictResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = districtServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildDistrictResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<District> districtRetrieved = districtRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (districtRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildDistrictResponse(404, false, message, null, null,
                    null);
        }

        District districtToBeDeleted = districtRetrieved.get();

        locationHierarchyCascadeSoftDeleteService.cascadeBeforeDeletingDistrict(districtToBeDeleted.getId(), locale, username);

        districtToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        District districtDeleted = districtServiceAuditable.delete(districtToBeDeleted, locale);

        DistrictDto districtDtoReturned = toDistrictDto(districtDeleted);

        message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildDistrictResponse(200, true, message, districtDtoReturned, null,
                null);
    }

    @Override
    public DistrictResponse findByMultipleFilters(DistrictMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<District> spec = null;
        spec = addToSpec(spec, DistrictSpecification::deleted);

        ValidatorDto validatorDto = districtServiceValidator.isRequestValidToRetrieveDistrictsByMultipleFilters(
                request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_DISTRICT_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildDistrictResponseWithErrors(400, false, message, null, null, null, 
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "id"));

        if (isNotEmpty(request.getName())) {
            spec = addToSpec(request.getName(), spec, DistrictSpecification::nameLike);
        }

        if (isNotEmpty(request.getCode())) {
            spec = addToSpec(request.getCode(), spec, DistrictSpecification::codeLike);
        }

        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, DistrictSpecification::any);
        }

        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, DistrictSpecification::hasEntityStatus);
        }

        if (request.getProvinceId() != null) {
            Specification<District> byProvince = DistrictSpecification.byProvince(request.getProvinceId());
            spec = (spec == null) ? byProvince : spec.and(byProvince);
        }

        if (request.getAdministrativeLevelId() != null) {
            Specification<District> byLevel = DistrictSpecification.byAdministrativeLevel(request.getAdministrativeLevelId());
            spec = (spec == null) ? byLevel : spec.and(byLevel);
        }

        Page<District> result = districtRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildDistrictResponse(404, false, message, null, null, null);
        }

        Page<DistrictDto> districtDtoPage = convertDistrictEntityToDistrictDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_DISTRICT_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildDistrictResponse(200, true, message, null, null, districtDtoPage);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<District> addToSpec(Specification<District> spec,
                                           Function<EntityStatus, Specification<District>> predicateMethod) {
        Specification<District> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<District> addToSpec(final String aString, Specification<District> spec, Function<String,
            Specification<District>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<District> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<District> addToSpec(final EntityStatus entityStatus, Specification<District> spec, Function<EntityStatus,
            Specification<District>> predicateMethod) {
        if (entityStatus != null) {
            Specification<District> localSpec = Specification.where(predicateMethod.apply(entityStatus));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
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

    private DistrictDto toDistrictDto(District entity) {
        DistrictDto dto = new DistrictDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        if (entity.getProvince() != null) {
            dto.setProvinceId(entity.getProvince().getId());
            dto.setProvinceName(entity.getProvince().getName());
            if (entity.getProvince().getCountry() != null) {
                dto.setCountryId(entity.getProvince().getCountry().getId());
                dto.setCountryName(entity.getProvince().getCountry().getName());
            }
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

    private Page<DistrictDto> convertDistrictEntityToDistrictDto(Page<District> districtPage) {
        List<District> districtList = districtPage.getContent();
        List<DistrictDto> districtDtoList = new ArrayList<>();

        for (District district : districtPage) {
            districtDtoList.add(toDistrictDto(district));
        }

        int page = districtPage.getNumber();
        int size = districtPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableDistricts = PageRequest.of(page, size);

        return new PageImpl<>(districtDtoList, pageableDistricts, districtPage.getTotalElements());
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<DistrictDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (DistrictDto district : items) {
            sb.append(district.getId()).append(",")
                    .append(safe(district.getName())).append(",")
                    .append(safe(district.getCode())).append(",")
                    .append(safe(district.getProvinceName())).append(",")
                    .append(safe(district.getCountryName())).append(",")
                    .append(safe(district.getAdministrativeLevelName())).append(",")
                    .append(district.getProvinceId() != null ? district.getProvinceId() : "").append(",")
                    .append(district.getAdministrativeLevelId() != null ? district.getAdministrativeLevelId() : "").append(",")
                    .append(district.getGeoCoordinatesId() != null ? district.getGeoCoordinatesId() : "").append(",")
                    .append(district.getCreatedAt()).append(",")
                    .append(district.getUpdatedAt()).append(",")
                    .append(district.getEntityStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<DistrictDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Districts");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (DistrictDto district : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(district.getId());
            row.createCell(1).setCellValue(safe(district.getName()));
            row.createCell(2).setCellValue(safe(district.getCode()));
            row.createCell(3).setCellValue(safe(district.getProvinceName()));
            row.createCell(4).setCellValue(safe(district.getCountryName()));
            row.createCell(5).setCellValue(safe(district.getAdministrativeLevelName()));
            row.createCell(6).setCellValue(district.getProvinceId() != null ? district.getProvinceId() : 0);
            row.createCell(7).setCellValue(district.getAdministrativeLevelId() != null ? district.getAdministrativeLevelId() : 0);
            row.createCell(8).setCellValue(district.getGeoCoordinatesId() != null ? district.getGeoCoordinatesId() : 0);
            row.createCell(9).setCellValue(district.getCreatedAt() != null ? district.getCreatedAt().toString() : "");
            row.createCell(10).setCellValue(district.getUpdatedAt() != null ? district.getUpdatedAt().toString() : "");
            row.createCell(11).setCellValue(district.getEntityStatus() != null ? district.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<DistrictDto> items) throws DocumentException {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("DISTRICT EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);

        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (DistrictDto district : items) {
            table.addCell(String.valueOf(district.getId()));
            table.addCell(safe(district.getName()));
            table.addCell(safe(district.getCode()));
            table.addCell(safe(district.getProvinceName()));
            table.addCell(safe(district.getCountryName()));
            table.addCell(safe(district.getAdministrativeLevelName()));
            table.addCell(district.getProvinceId() != null ? district.getProvinceId().toString() : "");
            table.addCell(district.getAdministrativeLevelId() != null ? district.getAdministrativeLevelId().toString() : "");
            table.addCell(district.getGeoCoordinatesId() != null ? district.getGeoCoordinatesId().toString() : "");
            table.addCell(district.getCreatedAt() != null ? district.getCreatedAt().toString() : "");
            table.addCell(district.getUpdatedAt() != null ? district.getUpdatedAt().toString() : "");
            table.addCell(district.getEntityStatus() != null ? district.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importDistrictFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<DistrictCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(DistrictCsvDto.class);

            CsvToBean<DistrictCsvDto> csvToBean = new CsvToBeanBuilder<DistrictCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<DistrictCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (DistrictCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getName() == null || row.getName().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing district name");
                        continue;
                    }
                    if (row.getProvinceId() == null) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing province ID");
                        continue;
                    }

                    Optional<Province> provinceOpt =
                            provinceRepository.findByIdFetchingGeoCoordinates(row.getProvinceId(), EntityStatus.DELETED);
                    if (provinceOpt.isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Province not found for ID " + row.getProvinceId());
                        continue;
                    }

                    CreateDistrictRequest request = new CreateDistrictRequest();
                    request.setName(row.getName());
                    request.setCode(row.getCode());
                    request.setProvinceId(row.getProvinceId());
                    request.setAdministrativeLevelId(row.getAdministrativeLevelId());
                    GeoCoordinateCsvImportHelper.ResolvedGeo geo = GeoCoordinateCsvImportHelper.resolve(
                            row.getGeoCoordinatesId(),
                            row.getLatitude(),
                            row.getLongitude(),
                            provinceOpt.get().getGeoCoordinates());
                    request.setGeoCoordinatesId(geo.geoCoordinatesId());
                    request.setLatitude(geo.latitude());
                    request.setLongitude(geo.longitude());

                    DistrictResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
                ? "Import completed successfully. " + success + " out of " + total + " districts imported."
                : "Import failed. No districts were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private DistrictResponse buildDistrictResponse(int statusCode, boolean isSuccess, String message,
                                                 DistrictDto districtDto, List<DistrictDto> districtDtoList,
                                                 Page<DistrictDto> districtDtoPage) {

        DistrictResponse districtResponse = new DistrictResponse();

        districtResponse.setStatusCode(statusCode);
        districtResponse.setSuccess(isSuccess);
        districtResponse.setMessage(message);
        districtResponse.setDistrictDto(districtDto);
        districtResponse.setDistrictDtoList(districtDtoList);
        districtResponse.setDistrictDtoPage(districtDtoPage);

        return districtResponse;
    }

    private DistrictResponse buildDistrictResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                         DistrictDto districtDto, List<DistrictDto> districtDtoList,
                                                         Page<DistrictDto> districtDtoPage, List<String> errorMessages) {

        DistrictResponse districtResponse = new DistrictResponse();

        districtResponse.setStatusCode(statusCode);
        districtResponse.setSuccess(isSuccess);
        districtResponse.setMessage(message);
        districtResponse.setDistrictDto(districtDto);
        districtResponse.setDistrictDtoList(districtDtoList);
        districtResponse.setDistrictDtoPage(districtDtoPage);
        districtResponse.setErrorMessages(errorMessages);

        return districtResponse;
    }
}
