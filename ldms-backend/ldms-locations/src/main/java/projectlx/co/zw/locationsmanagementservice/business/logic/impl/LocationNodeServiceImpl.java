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
import lombok.extern.slf4j.Slf4j;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LocationNodeServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationNodeService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.LocationNodeServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.LocationAlias;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.repository.LocationNodeRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.LocationNodeSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocationNodeCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocationNodeDto;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class LocationNodeServiceImpl implements LocationNodeService {
    private static final String EXCHANGE = "ldms.locations.exchange";
    private static final String CREATED_ROUTING_KEY = "location.node.created";
    private static final String UPDATED_ROUTING_KEY = "location.node.updated";
    private static final String[] CSV_EXPORT_HEADERS = {
            "ID", "NAME", "CODE", "LOCATION TYPE", "PARENT ID", "PARENT NAME",
            "DISTRICT ID", "SUBURB ID", "LATITUDE", "LONGITUDE", "TIMEZONE", "POSTAL CODE",
            "ENTITY STATUS", "CREATED AT", "UPDATED AT"
    };

    private final LocationNodeServiceValidator validator;
    private final LocationNodeRepository locationNodeRepository;
    private final DistrictRepository districtRepository;
    private final SuburbRepository suburbRepository;
    private final LocationNodeServiceAuditable locationNodeServiceAuditable;
    private final LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public LocationNodeResponse create(CreateLocationNodeRequest request, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isCreateValid(request, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid location create request", null, null, null, validation.getErrorMessages());
        }

        LocationNode node = new LocationNode();
        node.setName(request.getName().trim());
        node.setCode(request.getCode());
        node.setLocationType(request.getLocationType());
        node.setLatitude(request.getLatitude());
        node.setLongitude(request.getLongitude());
        node.setTimezone(request.getTimezone());
        node.setPostalCode(request.getPostalCode());
        node.setCreatedAt(LocalDateTime.now());
        node.setCreatedBy(username);
        node.setEntityStatus(EntityStatus.ACTIVE);

        if (request.getLocationType() != LocationType.CITY && request.getParentId() != null) {
            Optional<LocationNode> parent = locationNodeRepository.findByIdAndEntityStatusNot(request.getParentId(), EntityStatus.DELETED);
            if (parent.isEmpty()) {
                return response(404, false, "Parent location not found", null, null, null, null);
            }
            node.setParent(parent.get());
        }
        String hierarchyError = applyHierarchyLinks(node, request.getDistrictId(), request.getSuburbId());
        if (hierarchyError != null) {
            return response(400, false, hierarchyError, null, null, null, null);
        }
        node.setAliases(toAliases(request.getAliases(), node, username));

        LocationNode saved = locationNodeServiceAuditable.create(node, locale, username);
        publishEvent(CREATED_ROUTING_KEY, saved.getId());
        return response(201, true, "Location node created successfully", toDto(saved), null, null, null);
    }

    @Override
    public LocationNodeResponse update(EditLocationNodeRequest request, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isEditValid(request, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid location update request", null, null, null, validation.getErrorMessages());
        }
        Optional<LocationNode> existing = locationNodeRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (existing.isEmpty()) {
            return response(404, false, "Location node not found", null, null, null, null);
        }

        LocationNode node = existing.get();
        node.setName(request.getName().trim());
        node.setCode(request.getCode());
        node.setLocationType(request.getLocationType());
        node.setLatitude(request.getLatitude());
        node.setLongitude(request.getLongitude());
        node.setTimezone(request.getTimezone());
        node.setPostalCode(request.getPostalCode());
        node.setModifiedAt(LocalDateTime.now());
        node.setModifiedBy(username);

        if (request.getLocationType() == LocationType.CITY) {
            node.setParent(null);
        } else if (request.getParentId() != null) {
            Optional<LocationNode> parent = locationNodeRepository.findByIdAndEntityStatusNot(request.getParentId(), EntityStatus.DELETED);
            if (parent.isEmpty()) {
                return response(404, false, "Parent location not found", null, null, null, null);
            }
            node.setParent(parent.get());
        } else {
            node.setParent(null);
        }
        String hierarchyError = applyHierarchyLinks(node, request.getDistrictId(), request.getSuburbId());
        if (hierarchyError != null) {
            return response(400, false, hierarchyError, null, null, null, null);
        }

        node.getAliases().clear();
        node.getAliases().addAll(toAliases(request.getAliases(), node, username));
        LocationNode saved = locationNodeServiceAuditable.update(node, locale, username);
        publishEvent(UPDATED_ROUTING_KEY, saved.getId());
        return response(200, true, "Location node updated successfully", toDto(saved), null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationNodeResponse findById(Long id, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid id", null, null, null, validation.getErrorMessages());
        }
        return locationNodeRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .map(node -> response(200, true, "Location node found", toDto(node), null, null, null))
                .orElseGet(() -> response(404, false, "Location node not found", null, null, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public LocationNodeResponse findByParentId(Long parentId, java.util.Locale locale, String username) {
        List<LocationNodeDto> dtoList = locationNodeRepository.findByParentIdAndEntityStatusNot(parentId, EntityStatus.DELETED)
                .stream().map(this::toDto).toList();
        return response(200, true, "Location nodes retrieved successfully", null, dtoList, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationNodeResponse findByMultipleFilters(LocationNodeMultipleFiltersRequest request, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isFilterValid(request, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid filter request", null, null, null, validation.getErrorMessages());
        }

        Specification<LocationNode> spec = Specification.where(LocationNodeSpecification.notDeleted());
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.hasLocationType(request.getLocationType()));
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.parentIdEquals(request.getParentId()));
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.districtIdEquals(request.getDistrictId()));
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.nameContains(request.getName()));
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.codeContains(request.getCode()));
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.timezoneContains(request.getTimezone()));
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.parentNameContains(request.getParentName()));
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.searchValueMatches(request.getSearchValue()));
        spec = combineLocationNodeSpec(spec, LocationNodeSpecification.hasEntityStatus(request.getEntityStatus()));

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "id"));
        Page<LocationNode> page = locationNodeRepository.findAll(spec, pageable);

        if (page.getContent().isEmpty()) {
            return response(404, false, "Location nodes not found", null, null, null, null);
        }

        Page<LocationNodeDto> dtoPage = page.map(this::toDto);
        return response(200, true, "Location nodes retrieved successfully", null, null, dtoPage, null);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationNodeResponse findAllAsList(java.util.Locale locale, String username) {
        List<LocationNode> nodes = locationNodeRepository.findAllByEntityStatusNot(EntityStatus.DELETED);
        List<LocationNodeDto> dtoList = nodes.stream().map(this::toDto).toList();
        return response(200, true, "Location nodes retrieved successfully", null, dtoList, null, null);
    }

    @Override
    public byte[] exportToCsv(List<LocationNodeDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", CSV_EXPORT_HEADERS)).append("\n");

        for (LocationNodeDto node : items) {
            sb.append(csv(node.getId())).append(",")
                    .append(csv(node.getName())).append(",")
                    .append(csv(node.getCode())).append(",")
                    .append(csv(node.getLocationType())).append(",")
                    .append(csv(node.getParentId())).append(",")
                    .append(csv(node.getParentName())).append(",")
                    .append(csv(node.getDistrictId())).append(",")
                    .append(csv(node.getSuburbId())).append(",")
                    .append(csv(node.getLatitude())).append(",")
                    .append(csv(node.getLongitude())).append(",")
                    .append(csv(node.getTimezone())).append(",")
                    .append(csv(node.getPostalCode())).append(",")
                    .append(csv(node.getEntityStatus())).append(",")
                    .append(csv(node.getCreatedAt())).append(",")
                    .append(csv(node.getModifiedAt()))
                    .append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<LocationNodeDto> items) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Location Nodes");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < CSV_EXPORT_HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(CSV_EXPORT_HEADERS[i]);
            }

            int rowIdx = 1;
            for (LocationNodeDto node : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(value(node.getId()));
                row.createCell(1).setCellValue(value(node.getName()));
                row.createCell(2).setCellValue(value(node.getCode()));
                row.createCell(3).setCellValue(value(node.getLocationType()));
                row.createCell(4).setCellValue(value(node.getParentId()));
                row.createCell(5).setCellValue(value(node.getParentName()));
                row.createCell(6).setCellValue(value(node.getDistrictId()));
                row.createCell(7).setCellValue(value(node.getSuburbId()));
                row.createCell(8).setCellValue(value(node.getLatitude()));
                row.createCell(9).setCellValue(value(node.getLongitude()));
                row.createCell(10).setCellValue(value(node.getTimezone()));
                row.createCell(11).setCellValue(value(node.getPostalCode()));
                row.createCell(12).setCellValue(value(node.getEntityStatus()));
                row.createCell(13).setCellValue(value(node.getCreatedAt()));
                row.createCell(14).setCellValue(value(node.getModifiedAt()));
            }

            for (int i = 0; i < CSV_EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<LocationNodeDto> items) throws DocumentException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            document.add(new Paragraph("Location Nodes", titleFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(CSV_EXPORT_HEADERS.length);
            table.setWidthPercentage(100);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            for (String header : CSV_EXPORT_HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(Color.DARK_GRAY);
                table.addCell(cell);
            }

            for (LocationNodeDto node : items) {
                table.addCell(value(node.getId()));
                table.addCell(value(node.getName()));
                table.addCell(value(node.getCode()));
                table.addCell(value(node.getLocationType()));
                table.addCell(value(node.getParentId()));
                table.addCell(value(node.getParentName()));
                table.addCell(value(node.getDistrictId()));
                table.addCell(value(node.getSuburbId()));
                table.addCell(value(node.getLatitude()));
                table.addCell(value(node.getLongitude()));
                table.addCell(value(node.getTimezone()));
                table.addCell(value(node.getPostalCode()));
                table.addCell(value(node.getEntityStatus()));
                table.addCell(value(node.getCreatedAt()));
                table.addCell(value(node.getModifiedAt()));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (IOException ioException) {
            throw new DocumentException(ioException);
        }
    }

    @Override
    public ImportSummary importLocationNodeFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<LocationNodeCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(LocationNodeCsvDto.class);

            CsvToBean<LocationNodeCsvDto> csvToBean = new CsvToBeanBuilder<LocationNodeCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<LocationNodeCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (LocationNodeCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getName() == null || row.getName().isBlank()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing node name");
                        continue;
                    }
                    if (row.getLocationType() == null || row.getLocationType().isBlank()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing location type");
                        continue;
                    }

                    String locationTypeRaw = row.getLocationType().trim().toUpperCase(Locale.ROOT);
                    if (!"CITY".equals(locationTypeRaw) && !"VILLAGE".equals(locationTypeRaw)) {
                        failed++;
                        errors.add("Row " + rowNum + ": Unsupported LOCATION TYPE '" + row.getLocationType()
                                + "' (allowed: CITY, VILLAGE)");
                        continue;
                    }

                    CreateLocationNodeRequest request = new CreateLocationNodeRequest();
                    request.setName(row.getName().trim());
                    request.setCode(row.getCode());
                    request.setLocationType(LocationType.valueOf(locationTypeRaw));
                    if (LocationType.CITY.equals(request.getLocationType())) {
                        request.setParentId(null);
                    } else {
                        request.setParentId(row.getParentId());
                    }
                    request.setDistrictId(row.getDistrictId());
                    request.setSuburbId(row.getSuburbId());
                    request.setLatitude(row.getLatitude());
                    request.setLongitude(row.getLongitude());
                    request.setTimezone(row.getTimezone());
                    request.setPostalCode(row.getPostalCode());
                    request.setAliases(parseAliases(row.getAliases()));

                    LocationNodeResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
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
                ? "Import completed successfully. " + success + " out of " + total + " location nodes imported."
                : "Import failed. No location nodes were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private Specification<LocationNode> combineLocationNodeSpec(Specification<LocationNode> base, Specification<LocationNode> addition) {
        if (addition == null) {
            return base;
        }
        return base.and(addition);
    }

    @Override
    public LocationNodeResponse delete(Long id, java.util.Locale locale, String username) {
        ValidatorDto validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return response(400, false, "Invalid id", null, null, null, validation.getErrorMessages());
        }
        Optional<LocationNode> existing = locationNodeRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (existing.isEmpty()) {
            return response(404, false, "Location node not found", null, null, null, null);
        }
        LocationNode node = existing.get();
        locationHierarchyCascadeSoftDeleteService.cascadeBeforeDeletingLocationNode(id, locale, username);
        node = locationNodeRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(node);
        node.setEntityStatus(EntityStatus.DELETED);
        node.setModifiedAt(LocalDateTime.now());
        node.setModifiedBy(username);
        locationNodeServiceAuditable.delete(node, locale, username);
        return response(200, true, "Location node deleted successfully", toDto(node), null, null, null);
    }

    private List<LocationAlias> toAliases(List<String> aliases, LocationNode node, String username) {
        List<LocationAlias> aliasEntities = new ArrayList<>();
        if (aliases == null) {
            return aliasEntities;
        }
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) {
                continue;
            }
            LocationAlias a = new LocationAlias();
            a.setAlias(alias.trim());
            a.setLocationNode(node);
            a.setEntityStatus(EntityStatus.ACTIVE);
            a.setCreatedAt(LocalDateTime.now());
            a.setCreatedBy(username);
            aliasEntities.add(a);
        }
        return aliasEntities;
    }

    private String applyHierarchyLinks(LocationNode node, Long districtId, Long suburbId) {
        if (districtId != null) {
            var district = districtRepository.findByIdAndEntityStatusNot(districtId, EntityStatus.DELETED);
            if (district.isEmpty()) {
                return "District not found";
            }
            node.setDistrict(district.get());
        } else {
            node.setDistrict(null);
        }

        if (suburbId != null) {
            var suburb = suburbRepository.findByIdAndEntityStatusNot(suburbId, EntityStatus.DELETED);
            if (suburb.isEmpty()) {
                return "Suburb not found";
            }
            if (node.getLocationType() != LocationType.VILLAGE) {
                return "SUBURB ID is only allowed for VILLAGE location type";
            }
            if (node.getDistrict() != null && suburb.get().getDistrict() != null
                    && !node.getDistrict().getId().equals(suburb.get().getDistrict().getId())) {
                return "Village suburb must belong to the same district";
            }
            node.setSuburb(suburb.get());
        } else {
            node.setSuburb(null);
        }

        if (node.getLocationType() == LocationType.CITY && node.getDistrict() == null) {
            return "CITY requires DISTRICT ID";
        }
        if (node.getLocationType() == LocationType.VILLAGE && node.getDistrict() == null) {
            return "VILLAGE requires DISTRICT ID";
        }
        return null;
    }

    private LocationNodeDto toDto(LocationNode node) {
        LocationNodeDto dto = new LocationNodeDto();
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setCode(node.getCode());
        dto.setLocationType(node.getLocationType());
        if (node.getParent() != null) {
            dto.setParentId(node.getParent().getId());
            dto.setParentName(node.getParent().getName());
        }
        if (node.getDistrict() != null) {
            dto.setDistrictId(node.getDistrict().getId());
            dto.setDistrictName(node.getDistrict().getName());
        }
        if (node.getSuburb() != null) {
            dto.setSuburbId(node.getSuburb().getId());
            dto.setSuburbName(node.getSuburb().getName());
        }
        dto.setLatitude(node.getLatitude());
        dto.setLongitude(node.getLongitude());
        dto.setTimezone(node.getTimezone());
        dto.setPostalCode(node.getPostalCode());
        dto.setAliases(node.getAliases().stream().map(LocationAlias::getAlias).toList());
        dto.setEntityStatus(node.getEntityStatus());
        dto.setCreatedAt(node.getCreatedAt());
        dto.setCreatedBy(node.getCreatedBy());
        dto.setModifiedAt(node.getModifiedAt());
        dto.setModifiedBy(node.getModifiedBy());
        return dto;
    }

    private List<String> parseAliases(String aliasesRaw) {
        if (aliasesRaw == null || aliasesRaw.isBlank()) {
            return List.of();
        }
        return List.of(aliasesRaw.split(";")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private LocationNodeResponse response(int status, boolean success, String message, LocationNodeDto dto, List<LocationNodeDto> dtoList,
                                          Page<LocationNodeDto> dtoPage, List<String> errors) {
        LocationNodeResponse response = new LocationNodeResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setLocationNodeDto(dto);
        response.setLocationNodeDtoList(dtoList);
        response.setLocationNodeDtoPage(dtoPage);
        response.setErrorMessages(errors);
        return response;
    }

    private void publishEvent(String routingKey, Long nodeId) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, "locationNodeId=" + nodeId);
        } catch (Exception ex) {
            log.warn("Failed to publish location event for node {}: {}", nodeId, ex.getMessage());
        }
    }
}
