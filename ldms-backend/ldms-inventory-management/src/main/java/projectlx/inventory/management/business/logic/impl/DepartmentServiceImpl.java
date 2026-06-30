package projectlx.inventory.management.business.logic.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.inventory.management.business.auditable.api.DepartmentServiceAuditable;
import projectlx.inventory.management.business.logic.api.DepartmentService;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.logic.support.InventoryOrganizationScopeSupport;
import projectlx.inventory.management.business.validator.api.DepartmentServiceValidator;
import projectlx.inventory.management.model.Department;
import projectlx.inventory.management.repository.DepartmentRepository;
import projectlx.inventory.management.repository.PurchaseRequisitionRepository;
import projectlx.inventory.management.repository.specification.DepartmentSpecification;
import projectlx.inventory.management.utils.dtos.DepartmentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateDepartmentRequest;
import projectlx.inventory.management.utils.requests.DepartmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.EditDepartmentRequest;
import projectlx.inventory.management.utils.responses.DepartmentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Transactional
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private static final String[] HEADERS = {"ID", "NAME", "DEPARTMENT_CODE", "DESCRIPTION"};

    private final DepartmentServiceValidator validator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final DepartmentRepository departmentRepository;
    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final DepartmentServiceAuditable auditable;
    private final InventoryOrganizationScopeSupport organizationScopeSupport;

    @Override
    public DepartmentResponse create(CreateDepartmentRequest request, Locale locale, String username) {
        ValidatorDto validatorDto = validator.isCreateDepartmentRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_DEPARTMENT_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Long orgId = resolveOwningOrganizationId(request.getSupplierId(), username, locale);
        if (orgId == null) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_DEPARTMENT_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        String normalizedName = request.getName().trim().toUpperCase();
        if (departmentRepository.findByNameAndSupplierIdAndEntityStatusNot(normalizedName, orgId, EntityStatus.DELETED)
                .isPresent()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_ALREADY_EXISTS.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        String normalizedCode = normalizeCode(request.getDepartmentCode());
        if (normalizedCode != null
                && departmentRepository.findByDepartmentCodeAndSupplierIdAndEntityStatusNot(
                        normalizedCode, orgId, EntityStatus.DELETED).isPresent()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_CODE_ALREADY_EXISTS.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        Department department = new Department();
        department.setName(normalizedName);
        department.setDepartmentCode(normalizedCode);
        department.setDescription(trimToNull(request.getDescription()));
        department.setSupplierId(orgId);

        Department saved = auditable.create(department, locale, username);
        DepartmentDto dto = mapDto(saved);
        dto.setInUse(Boolean.FALSE);
        String message = messageService.getMessage(
                I18Code.MESSAGE_DEPARTMENT_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public DepartmentResponse findById(Long id, Locale locale, String username) {
        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<Department> existing = departmentRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (existing.isEmpty() || !isVisibleToUser(existing.get(), username, locale)) {
            String message = messageService.getMessage(I18Code.MESSAGE_DEPARTMENT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        String message = messageService.getMessage(
                I18Code.MESSAGE_DEPARTMENT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        DepartmentDto dto = mapDto(existing.get());
        dto.setInUse(isDepartmentInUse(id));
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public DepartmentResponse findAllAsList(Locale locale, String username) {
        List<Department> list;
        if (organizationScopeSupport.isSystemUser(username)) {
            list = departmentRepository.findByEntityStatusNot(EntityStatus.DELETED);
        } else {
            Long orgId = organizationScopeSupport.resolveOrganizationId(username, locale);
            if (orgId == null) {
                String message = messageService.getMessage(
                        I18Code.MESSAGE_DEPARTMENT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
                return buildResponse(200, true, message, null, List.of(), null);
            }
            list = departmentRepository.findBySupplierIdAndEntityStatusNot(orgId, EntityStatus.DELETED);
        }

        List<DepartmentDto> dtoList = enrichInUseFlags(list.stream().map(this::mapDto).toList());
        String message = messageService.getMessage(
                I18Code.MESSAGE_DEPARTMENT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    public DepartmentResponse update(EditDepartmentRequest request, String username, Locale locale) {
        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_UPDATE_DEPARTMENT_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<Department> existingOpt = departmentRepository.findByIdAndEntityStatusNot(
                request.getDepartmentId(), EntityStatus.DELETED);
        if (existingOpt.isEmpty() || !isVisibleToUser(existingOpt.get(), username, locale)) {
            String message = messageService.getMessage(I18Code.MESSAGE_DEPARTMENT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Department toEdit = existingOpt.get();
        String normalizedName = request.getName().trim().toUpperCase();
        Optional<Department> duplicateName = departmentRepository.findByNameAndSupplierIdAndEntityStatusNot(
                normalizedName, toEdit.getSupplierId(), EntityStatus.DELETED);
        if (duplicateName.isPresent() && !duplicateName.get().getId().equals(toEdit.getId())) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_ALREADY_EXISTS.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        String normalizedCode = normalizeCode(request.getDepartmentCode());
        if (normalizedCode != null) {
            Optional<Department> duplicateCode = departmentRepository.findByDepartmentCodeAndSupplierIdAndEntityStatusNot(
                    normalizedCode, toEdit.getSupplierId(), EntityStatus.DELETED);
            if (duplicateCode.isPresent() && !duplicateCode.get().getId().equals(toEdit.getId())) {
                String message = messageService.getMessage(
                        I18Code.MESSAGE_DEPARTMENT_CODE_ALREADY_EXISTS.getCode(), new String[]{}, locale);
                return buildResponse(400, false, message, null, null, null);
            }
        }

        toEdit.setName(normalizedName);
        toEdit.setDepartmentCode(normalizedCode);
        if (request.getDescription() != null) {
            toEdit.setDescription(trimToNull(request.getDescription()));
        }

        Department saved = auditable.update(toEdit, locale, username);
        String message = messageService.getMessage(
                I18Code.MESSAGE_DEPARTMENT_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        DepartmentDto dto = mapDto(saved);
        dto.setInUse(isDepartmentInUse(saved.getId()));
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public DepartmentResponse delete(Long id, Locale locale, String username) {
        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<Department> existingOpt = departmentRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (existingOpt.isEmpty() || !isVisibleToUser(existingOpt.get(), username, locale)) {
            String message = messageService.getMessage(I18Code.MESSAGE_DEPARTMENT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Department toDelete = existingOpt.get();
        if (isDepartmentInUse(id)) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_CANNOT_DELETE_IN_USE.getCode(), new String[]{}, locale);
            DepartmentDto dto = mapDto(toDelete);
            dto.setInUse(Boolean.TRUE);
            return buildResponse(409, false, message, dto, null, null);
        }

        toDelete.setEntityStatus(EntityStatus.DELETED);
        Department saved = auditable.delete(toDelete, locale);
        String message = messageService.getMessage(
                I18Code.MESSAGE_DEPARTMENT_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        DepartmentDto dto = mapDto(saved);
        dto.setInUse(Boolean.FALSE);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public DepartmentResponse findByMultipleFilters(
            DepartmentMultipleFiltersRequest request, String username, Locale locale) {
        Specification<Department> spec = null;
        spec = addToSpec(spec, DepartmentSpecification::deleted);

        ValidatorDto validatorDto = validator.isRequestValidToRetrieveDepartmentByMultipleFilters(request, locale);
        if (validatorDto == null || !validatorDto.getSuccess()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        ValidatorDto nameValidatorDto = validator.isStringValid(request.getName(), locale);
        if (nameValidatorDto.getSuccess()) {
            spec = addToSpec(request.getName(), spec, DepartmentSpecification::nameLike);
        }

        ValidatorDto codeValidatorDto = validator.isStringValid(request.getDepartmentCode(), locale);
        if (codeValidatorDto.getSuccess()) {
            spec = addToSpec(request.getDepartmentCode(), spec, DepartmentSpecification::departmentCodeLike);
        }

        ValidatorDto descriptionValidatorDto = validator.isStringValid(request.getDescription(), locale);
        if (descriptionValidatorDto.getSuccess()) {
            spec = addToSpec(request.getDescription(), spec, DepartmentSpecification::descriptionLike);
        }

        ValidatorDto searchValueValidatorDto = validator.isStringValid(request.getSearchValue(), locale);
        if (searchValueValidatorDto.getSuccess()) {
            spec = addToSpec(request.getSearchValue(), spec, DepartmentSpecification::any);
        }

        if (!organizationScopeSupport.isSystemUser(username)) {
            Long orgId = organizationScopeSupport.resolveOrganizationId(username, locale);
            if (orgId == null) {
                String message = messageService.getMessage(
                        I18Code.MESSAGE_DEPARTMENT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
                DepartmentResponse empty = buildResponse(200, true, message, null, null, null);
                empty.setDepartmentDtoPage(Page.empty(pageable));
                return empty;
            }
            spec = addToSpec(orgId, spec, DepartmentSpecification::supplierIdEquals);
        }

        long totalCount = departmentRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());
        if (request.getPage() >= maxPage && totalCount > 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_PAGE_OUT_OF_BOUNDS.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Page<Department> result = departmentRepository.findAll(spec, pageable);
        if (result.getContent().isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_NOT_FOUND.getCode(), new String[]{}, locale);
            DepartmentResponse empty = buildResponse(404, false, message, null, null, null);
            empty.setDepartmentDtoPage(Page.empty(pageable));
            return empty;
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<DepartmentDto> mapped = result.map(this::mapDto);
        List<DepartmentDto> enriched = enrichInUseFlags(mapped.getContent());
        Page<DepartmentDto> dtoPage = new org.springframework.data.domain.PageImpl<>(
                enriched, pageable, mapped.getTotalElements());
        String message = messageService.getMessage(
                I18Code.MESSAGE_DEPARTMENT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        DepartmentResponse response = buildResponse(200, true, message, null, null, null);
        response.setDepartmentDtoPage(dtoPage);
        return response;
    }

    @Override
    public byte[] exportToCsv(List<DepartmentDto> items) {
        items = InventoryExportSupport.nullSafe(items);
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (DepartmentDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(safe(item.getName())).append(",")
                    .append(safe(item.getDepartmentCode())).append(",")
                    .append(safe(item.getDescription())).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<DepartmentDto> items) throws IOException {
        items = InventoryExportSupport.nullSafe(items);
        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Departments");

        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;
        for (DepartmentDto item : items) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(safe(item.getName()));
            row.createCell(2).setCellValue(safe(item.getDepartmentCode()));
            row.createCell(3).setCellValue(safe(item.getDescription()));
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<DepartmentDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (DepartmentDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId()),
                    safe(item.getName()),
                    safe(item.getDepartmentCode()),
                    safe(item.getDescription())
            });
        }
        return InventoryExportSupport.writeTabularPdf(
                "Departments", "INV-DEPT", "Department export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importDepartmentFromCsv(InputStream csvInputStream, String username, Locale locale)
            throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int total = 0;

        try (java.io.Reader reader = new java.io.InputStreamReader(csvInputStream, java.nio.charset.StandardCharsets.UTF_8);
             org.apache.commons.csv.CSVParser csvParser = org.apache.commons.csv.CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            List<org.apache.commons.csv.CSVRecord> records = csvParser.getRecords();
            total = records.size();

            for (org.apache.commons.csv.CSVRecord record : records) {
                try {
                    CreateDepartmentRequest request = new CreateDepartmentRequest();
                    request.setName(record.get("NAME"));
                    if (record.isMapped("DEPARTMENT_CODE")) {
                        request.setDepartmentCode(record.get("DEPARTMENT_CODE"));
                    }
                    if (record.isMapped("DESCRIPTION")) {
                        request.setDescription(record.get("DESCRIPTION"));
                    }

                    DepartmentResponse response = create(request, locale, username);
                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + record.getRecordNumber() + ": " + response.getMessage());
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + record.getRecordNumber() + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = isSuccess
                ? "Import completed successfully. " + success + " out of " + total + " departments imported."
                : "Import failed. No departments were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private Long resolveOwningOrganizationId(Long requestedSupplierId, String username, Locale locale) {
        if (organizationScopeSupport.isSystemUser(username)) {
            return requestedSupplierId != null && requestedSupplierId > 0 ? requestedSupplierId : null;
        }
        Long callerOrgId = organizationScopeSupport.resolveOrganizationId(username, locale);
        if (callerOrgId == null) {
            return null;
        }
        if (requestedSupplierId == null || requestedSupplierId <= 0 || requestedSupplierId.equals(callerOrgId)) {
            return callerOrgId;
        }
        return null;
    }

    private boolean isVisibleToUser(Department department, String username, Locale locale) {
        if (department == null) {
            return false;
        }
        if (organizationScopeSupport.isSystemUser(username)) {
            return true;
        }
        Long orgId = organizationScopeSupport.resolveOrganizationId(username, locale);
        return orgId != null && orgId.equals(department.getSupplierId());
    }

    private DepartmentDto mapDto(Department department) {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper.map(department, DepartmentDto.class);
    }

    private boolean isDepartmentInUse(Long departmentId) {
        if (departmentId == null || departmentId <= 0) {
            return false;
        }
        return purchaseRequisitionRepository.existsByDepartmentIdAndEntityStatusNot(
                departmentId, EntityStatus.DELETED);
    }

    private List<DepartmentDto> enrichInUseFlags(List<DepartmentDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return dtos == null ? List.of() : dtos;
        }
        List<Long> ids = dtos.stream()
                .map(DepartmentDto::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (ids.isEmpty()) {
            return dtos;
        }
        Set<Long> inUseIds = new HashSet<>(
                purchaseRequisitionRepository.findDepartmentIdsReferencedByRequisitions(ids, EntityStatus.DELETED));
        for (DepartmentDto dto : dtos) {
            dto.setInUse(inUseIds.contains(dto.getId()));
        }
        return dtos;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private Specification<Department> addToSpec(
            Specification<Department> spec,
            java.util.function.Function<EntityStatus, Specification<Department>> predicateMethod) {
        return spec == null ? predicateMethod.apply(EntityStatus.DELETED) : spec.and(predicateMethod.apply(EntityStatus.DELETED));
    }

    private Specification<Department> addToSpec(
            String value,
            Specification<Department> spec,
            java.util.function.Function<String, Specification<Department>> predicateMethod) {
        if (value == null || value.trim().isEmpty()) {
            return spec;
        }
        String normalized = value.toUpperCase();
        return spec == null ? predicateMethod.apply(normalized) : spec.and(predicateMethod.apply(normalized));
    }

    private Specification<Department> addToSpec(
            Long supplierId,
            Specification<Department> spec,
            java.util.function.Function<Long, Specification<Department>> predicateMethod) {
        if (supplierId == null || supplierId <= 0) {
            return spec;
        }
        return spec == null ? predicateMethod.apply(supplierId) : spec.and(predicateMethod.apply(supplierId));
    }

    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DepartmentResponse buildResponse(
            int statusCode,
            boolean success,
            String message,
            DepartmentDto dto,
            List<DepartmentDto> dtoList,
            List<String> errorMessages) {
        DepartmentResponse response = new DepartmentResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(success);
        response.setMessage(message);
        response.setDepartmentDto(dto);
        response.setDepartmentDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private DepartmentResponse buildResponseWithErrors(
            int statusCode,
            boolean success,
            String message,
            DepartmentDto dto,
            List<DepartmentDto> dtoList,
            List<String> errorMessages) {
        return buildResponse(statusCode, success, message, dto, dtoList, errorMessages);
    }
}
