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
import projectlx.inventory.management.business.auditable.api.ProductSubCategoryServiceAuditable;
import projectlx.inventory.management.business.logic.api.ProductSubCategoryService;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.validator.api.ProductSubCategoryServiceValidator;
import projectlx.inventory.management.model.ProductCategory;
import projectlx.inventory.management.model.ProductSubCategory;
import projectlx.inventory.management.repository.ProductCategoryRepository;
import projectlx.inventory.management.repository.ProductSubCategoryRepository;
import projectlx.inventory.management.repository.specification.ProductSubCategorySpecification;
import projectlx.inventory.management.utils.dtos.ProductSubCategoryDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ProductSubCategoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductSubCategoryRequest;
import projectlx.inventory.management.utils.responses.ProductSubCategoryResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
public class ProductSubCategoryServiceImpl implements ProductSubCategoryService {

    private final ProductSubCategoryServiceValidator validator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final ProductSubCategoryRepository repository;
    private final ProductSubCategoryServiceAuditable auditable;
    private final ProductCategoryRepository productCategoryRepository;

    private static final String[] HEADERS = {"ID", "CATEGORY_ID", "NAME", "DESCRIPTION"};
    private static final String[] CSV_HEADERS = {"CATEGORY_ID", "NAME", "DESCRIPTION"};

    @Override
    public ProductSubCategoryResponse create(CreateProductSubCategoryRequest request, Locale locale, String username) {
        String message = "";

        ValidatorDto validatorDto = validator.isCreateProductSubCategoryRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_SUB_CATEGORY_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        // Validate category exists
        Optional<ProductCategory> categoryOpt = productCategoryRepository.findByIdAndEntityStatusNot(request.getCategoryId(),
                EntityStatus.DELETED);

        if (categoryOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_SUB_CATEGORY_CATEGORY_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        // Uniqueness within category
        Optional<ProductSubCategory> existing = repository.findByCategory_IdAndNameAndEntityStatusNot(request.getCategoryId(),
                request.getName(), EntityStatus.DELETED);

        if (existing.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_ALREADY_EXISTS.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        // Map and save
        ProductSubCategory toSave = new ProductSubCategory();
        toSave.setCategory(categoryOpt.get());
        toSave.setName(request.getName() != null ? request.getName().toUpperCase() : null);
        toSave.setDescription(request.getDescription());

        ProductSubCategory saved = auditable.create(toSave, locale, username);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductSubCategoryDto dto = modelMapper.map(saved, ProductSubCategoryDto.class);
        dto.setCategoryId(saved.getCategory() != null ? saved.getCategory().getId() : null);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_CREATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public ProductSubCategoryResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<ProductSubCategory> opt = repository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (opt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductSubCategoryDto dto = modelMapper.map(opt.get(), ProductSubCategoryDto.class);
        dto.setCategoryId(opt.get().getCategory() != null ? opt.get().getCategory().getId() : null);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public ProductSubCategoryResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<ProductSubCategory> list = repository.findByEntityStatusNot(EntityStatus.DELETED);

        if (list.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<ProductSubCategoryDto> dtoList = list.stream().map(psc -> {
            ProductSubCategoryDto dto = modelMapper.map(psc, ProductSubCategoryDto.class);
            dto.setCategoryId(psc.getCategory() != null ? psc.getCategory().getId() : null);
            return dto;
        }).toList();

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    public ProductSubCategoryResponse update(EditProductSubCategoryRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_SUB_CATEGORY_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<ProductSubCategory> existingOpt = repository.findByIdAndEntityStatusNot(request.getProductSubCategoryId(),
                EntityStatus.DELETED);
        if (existingOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        // Validate parent category exists
        Optional<ProductCategory> categoryOpt = productCategoryRepository.findByIdAndEntityStatusNot(request.getCategoryId(),
                EntityStatus.DELETED);

        if (categoryOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_SUB_CATEGORY_CATEGORY_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        // Check for uniqueness within category
        Optional<ProductSubCategory> byName = repository.findByCategory_IdAndNameAndEntityStatusNot(request.getCategoryId(),
                request.getName(), EntityStatus.DELETED);

        if (byName.isPresent() && !byName.get().getId().equals(request.getProductSubCategoryId())) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_ALREADY_EXISTS.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        ProductSubCategory toEdit = existingOpt.get();
        toEdit.setCategory(categoryOpt.get());
        toEdit.setName(request.getName() != null ? request.getName().toUpperCase() : null);
        toEdit.setDescription(request.getDescription());

        ProductSubCategory saved = auditable.update(toEdit, locale, username);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductSubCategoryDto dto = modelMapper.map(saved, ProductSubCategoryDto.class);
        dto.setCategoryId(saved.getCategory() != null ? saved.getCategory().getId() : null);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public ProductSubCategoryResponse delete(Long id, Locale locale, String username) {
        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<ProductSubCategory> existingOpt = repository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (existingOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        ProductSubCategory toDelete = existingOpt.get();
        toDelete.setEntityStatus(EntityStatus.DELETED);
        ProductSubCategory deleted = auditable.delete(toDelete, locale);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductSubCategoryDto dto = modelMapper.map(deleted, ProductSubCategoryDto.class);
        dto.setCategoryId(deleted.getCategory() != null ? deleted.getCategory().getId() : null);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_DELETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public ProductSubCategoryResponse findByMultipleFilters(ProductSubCategoryMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<ProductSubCategory> spec = null;
        // Apply deleted filter first (same structure as ProductCategoryServiceImpl)
        spec = addToSpec(spec, ProductSubCategorySpecification::deleted);

        // Validate request
        ValidatorDto validatorDto = validator.isRequestValidToRetrieveProductSubCategoryByMultipleFilters(request, locale);
        if (validatorDto == null || !validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // Apply categoryId filter when valid (> 0)
        if (request.getCategoryId() != null && request.getCategoryId() > 0) {
            spec = addToSpec(request.getCategoryId(), spec, ProductSubCategorySpecification::categoryIdEquals);
        }

        // Apply 'name' filter when valid
        ValidatorDto nameValidatorDto = validator.isStringValid(request.getName(), locale);
        if (nameValidatorDto.getSuccess()) {
            spec = addToSpec(request.getName(), spec, ProductSubCategorySpecification::nameLike);
        }

        // Apply 'searchValue' filter when valid (use specification.any)
        ValidatorDto searchValueValidatorDto = validator.isStringValid(request.getSearchValue(), locale);
        if (searchValueValidatorDto.getSuccess()) {
            spec = addToSpec(request.getSearchValue(), spec, ProductSubCategorySpecification::any);
        }

        long totalCount = repository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());
        if (request.getPage() >= maxPage && totalCount > 0) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_PAGE_OUT_OF_BOUNDS.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Page<ProductSubCategory> result = repository.findAll(spec, pageable);
        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        org.springframework.data.domain.Page<ProductSubCategoryDto> dtoPage = result.map(entity -> {
            ProductSubCategoryDto dto = modelMapper.map(entity, ProductSubCategoryDto.class);
            dto.setCategoryId(entity.getCategory() != null ? entity.getCategory().getId() : null);
            return dto;
        });

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        ProductSubCategoryResponse response = buildResponse(200, true, message, null, null, null);
        response.setProductSubCategoryDtoPage(dtoPage);
        return response;
    }

    private org.springframework.data.jpa.domain.Specification<ProductSubCategory> addToSpec(
            org.springframework.data.jpa.domain.Specification<ProductSubCategory> spec,
            java.util.function.Function<projectlx.co.zw.shared_library.utils.enums.EntityStatus, org.springframework.data.jpa.domain.Specification<ProductSubCategory>> predicateMethod) {
        return spec == null ? predicateMethod.apply(EntityStatus.DELETED) : spec.and(predicateMethod.apply(EntityStatus.DELETED));
    }

    private org.springframework.data.jpa.domain.Specification<ProductSubCategory> addToSpec(
            String aString,
            org.springframework.data.jpa.domain.Specification<ProductSubCategory> spec,
            java.util.function.Function<String, org.springframework.data.jpa.domain.Specification<ProductSubCategory>> predicateMethod) {
        if (aString == null || aString.trim().isEmpty()) return spec;
        String value = aString.toUpperCase();
        return spec == null ? predicateMethod.apply(value) : spec.and(predicateMethod.apply(value));
    }

    private org.springframework.data.jpa.domain.Specification<ProductSubCategory> addToSpec(
            Long aLong,
            org.springframework.data.jpa.domain.Specification<ProductSubCategory> spec,
            java.util.function.Function<Long, org.springframework.data.jpa.domain.Specification<ProductSubCategory>> predicateMethod) {
        if (aLong == null || aLong <= 0) return spec;
        return spec == null ? predicateMethod.apply(aLong) : spec.and(predicateMethod.apply(aLong));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<ProductSubCategoryDto> items) {
        items = InventoryExportSupport.nullSafe(items);
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (ProductSubCategoryDto item : items) {
            sb.append(item.getId()).append(",")
              .append(item.getCategoryId()).append(",")
              .append(safe(item.getName())).append(",")
              .append(safe(item.getDescription())).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<ProductSubCategoryDto> items) throws IOException {
        items = InventoryExportSupport.nullSafe(items);
        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Product SubCategories");

        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;
        for (ProductSubCategoryDto item : items) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(item.getCategoryId() != null ? item.getCategoryId() : 0);
            row.createCell(2).setCellValue(safe(item.getName()));
            row.createCell(3).setCellValue(safe(item.getDescription()));
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<ProductSubCategoryDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (ProductSubCategoryDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId()),
                    String.valueOf(item.getCategoryId() != null ? item.getCategoryId() : 0),
                    safe(item.getName()),
                    safe(item.getDescription())
            });
        }
        return InventoryExportSupport.writeTabularPdf(
                "Product Sub-Categories", "INV-PSUB", "Product sub-category export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importProductSubCategoryFromCsv(InputStream csvInputStream) throws IOException {
        java.util.List<String> errors = new java.util.ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (java.io.Reader reader = new java.io.InputStreamReader(csvInputStream, java.nio.charset.StandardCharsets.UTF_8);
             org.apache.commons.csv.CSVParser csvParser = org.apache.commons.csv.CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            java.util.List<org.apache.commons.csv.CSVRecord> records = csvParser.getRecords();
            total = records.size();

            for (org.apache.commons.csv.CSVRecord record : records) {
                try {
                    CreateProductSubCategoryRequest request = new CreateProductSubCategoryRequest();
                    request.setCategoryId(Long.valueOf(record.get("CATEGORY_ID")));
                    request.setName(record.get("NAME"));
                    request.setDescription(record.get("DESCRIPTION"));

                    ProductSubCategoryResponse response = create(request, java.util.Locale.ENGLISH, "IMPORT_SCRIPT");
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
                ? "Import completed successfully. " + success + " out of " + total + " product sub-categories imported."
                : "Import failed. No product sub-categories were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private ProductSubCategoryResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                                     ProductSubCategoryDto dto,
                                                     List<ProductSubCategoryDto> dtoList,
                                                     List<String> errorMessages) {
        ProductSubCategoryResponse response = new ProductSubCategoryResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setProductSubCategoryDto(dto);
        response.setProductSubCategoryDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private ProductSubCategoryResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                               ProductSubCategoryDto dto,
                                                               List<ProductSubCategoryDto> dtoList,
                                                               List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }
}
