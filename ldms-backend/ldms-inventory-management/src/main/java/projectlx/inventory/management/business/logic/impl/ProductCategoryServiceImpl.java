package projectlx.inventory.management.business.logic.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.ProductCategoryServiceAuditable;
import projectlx.inventory.management.business.logic.api.ProductCategoryService;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.validator.api.ProductCategoryServiceValidator;
import projectlx.inventory.management.model.ProductCategory;
import projectlx.inventory.management.repository.ProductCategoryRepository;
import projectlx.inventory.management.repository.specification.ProductCategorySpecification;
import projectlx.inventory.management.utils.dtos.ProductCategoryDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ProductCategoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateProductCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductCategoryRequest;
import projectlx.inventory.management.utils.responses.ProductCategoryResponse;
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
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryServiceValidator productCategoryServiceValidator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryServiceAuditable productCategoryServiceAuditable;

    private static final String[] HEADERS = {"ID", "NAME", "DESCRIPTION"};
    private static final String[] CSV_HEADERS = {"NAME", "DESCRIPTION"};

    @Override
    public ProductCategoryResponse create(CreateProductCategoryRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = productCategoryServiceValidator.isCreateProductCategoryRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_CATEGORY_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildProductCategoryResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<ProductCategory> productCategoryRetrieved =
                productCategoryRepository.findByNameAndEntityStatusNot(request.getName(), EntityStatus.DELETED);

        if (productCategoryRetrieved.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_ALREADY_EXISTS.getCode(),
                    new String[]{}, locale);

            return buildProductCategoryResponse(400, false, message, null,
                    null, null);
        }

        request.setName(request.getName().toUpperCase());
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductCategory productCategoryToBeSaved = modelMapper.map(request, ProductCategory.class);

        ProductCategory productCategorySaved = productCategoryServiceAuditable.create(productCategoryToBeSaved, locale,
                username);
        ProductCategoryDto productCategoryDtoReturned = modelMapper.map(productCategorySaved, ProductCategoryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_CREATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildProductCategoryResponse(201, true, message, productCategoryDtoReturned,
                null, null);
    }

    @Override
    public ProductCategoryResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = productCategoryServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildProductCategoryResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<ProductCategory> productCategoryRetrieved = productCategoryRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (productCategoryRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildProductCategoryResponse(404, false, message, null,
                    null, null);
        }

        ProductCategoryDto dto = modelMapper.map(productCategoryRetrieved.get(), ProductCategoryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildProductCategoryResponse(200, true, message, dto, null,
                null);
    }

    @Override
    public ProductCategoryResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<ProductCategory> list = productCategoryRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if (list.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildProductCategoryResponse(404, false, message, null,
                    null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<ProductCategoryDto> dtoList = list.stream()
                .map(pc -> modelMapper.map(pc, ProductCategoryDto.class))
                .toList();

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildProductCategoryResponse(200, true, message, null, dtoList,
                null);
    }

    @Override
    public ProductCategoryResponse update(EditProductCategoryRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = productCategoryServiceValidator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_CATEGORY_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildProductCategoryResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<ProductCategory> existingOpt = productCategoryRepository.findByIdAndEntityStatusNot(request.getProductCategoryId(),
                EntityStatus.DELETED);

        if (existingOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildProductCategoryResponse(400, false, message, null,
                    null, null);
        }

        // Check for name uniqueness against other records
        if (request.getName() != null && !request.getName().isEmpty()) {

            Optional<ProductCategory> byName = productCategoryRepository.findByNameAndEntityStatusNot(request.getName(),
                    EntityStatus.DELETED);

            if (byName.isPresent() && !byName.get().getId().equals(request.getProductCategoryId())) {

                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_ALREADY_EXISTS.getCode(),
                        new String[]{}, locale);

                return buildProductCategoryResponse(400, false, message, null,
                        null, null);
            }
        }

        ProductCategory toEdit = existingOpt.get();

        if (request.getName() != null && !request.getName().isEmpty()) {
            toEdit.setName(request.getName().toUpperCase());
        }

        if (request.getDescription() != null) {
            toEdit.setDescription(request.getDescription());
        }

        ProductCategory saved = productCategoryServiceAuditable.update(toEdit, locale, username);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductCategoryDto dto = modelMapper.map(saved, ProductCategoryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildProductCategoryResponse(201, true, message, dto, null,
                null);
    }

    @Override
    public ProductCategoryResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = productCategoryServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildProductCategoryResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<ProductCategory> existingOpt = productCategoryRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (existingOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildProductCategoryResponse(404, false, message, null,
                    null, null);
        }

        ProductCategory toDelete = existingOpt.get();
        toDelete.setEntityStatus(EntityStatus.DELETED);
        ProductCategory deleted = productCategoryServiceAuditable.delete(toDelete, locale);
        ProductCategoryDto dto = modelMapper.map(deleted, ProductCategoryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildProductCategoryResponse(200, true, message, dto, null,
                null);
    }

    @Override
    public ProductCategoryResponse findByMultipleFilters(ProductCategoryMultipleFiltersRequest request, String username,
                                                         Locale locale) {
        String message = "";

        Specification<ProductCategory> spec = null;
        spec = addToSpec(spec, ProductCategorySpecification::deleted);

        // Validate the incoming request (must not be null and within bounds)
        ValidatorDto validatorDto = productCategoryServiceValidator
                .isRequestValidToRetrieveProductCategoryByMultipleFilters(request, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildProductCategoryResponseWithErrors(400, false, message,
                    null, null, validatorDto != null ? validatorDto.getErrorMessages()
                            : null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // Apply 'name' filter when valid
        ValidatorDto nameValidatorDto = productCategoryServiceValidator.isStringValid(request.getName(), locale);

        if (nameValidatorDto.getSuccess()) {
            spec = addToSpec(request.getName(), spec, ProductCategorySpecification::nameLike);
        }

        // Apply 'searchValue' filter when valid
        ValidatorDto searchValueValidatorDto = productCategoryServiceValidator.isStringValid(request.getSearchValue(),
                locale);

        if (searchValueValidatorDto.getSuccess()) {
            spec = addToSpec(request.getSearchValue(), spec, ProductCategorySpecification::any);
        }

        // Page bounds check similar to UserServiceImpl
        long totalCount = productCategoryRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_PAGE_OUT_OF_BOUNDS.getCode(),
                    new String[]{}, locale);

            return buildProductCategoryResponse(404, false, message, null,
                    null, null);
        }

        Page<ProductCategory> result = productCategoryRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildProductCategoryResponse(404, false, message, null,
                    null, null);
        }

        // Map to DTO page
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        org.springframework.data.domain.Page<ProductCategoryDto> dtoPage = result.map(pc ->
                modelMapper.map(pc, ProductCategoryDto.class));

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        ProductCategoryResponse response = buildProductCategoryResponse(200, true, message,
                null, null, null);
        response.setProductCategoryDtoPage(dtoPage);

        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<ProductCategoryDto> items) {
        items = InventoryExportSupport.nullSafe(items);
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (ProductCategoryDto item : items) {
            sb.append(item.getId()).append(",")
              .append(safe(item.getName())).append(",")
              .append(safe(item.getDescription())).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<ProductCategoryDto> items) throws IOException {
        items = InventoryExportSupport.nullSafe(items);
        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Product Categories");

        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;
        for (ProductCategoryDto item : items) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(safe(item.getName()));
            row.createCell(2).setCellValue(safe(item.getDescription()));
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<ProductCategoryDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (ProductCategoryDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId()),
                    safe(item.getName()),
                    safe(item.getDescription())
            });
        }
        return InventoryExportSupport.writeTabularPdf(
                "Product Categories", "INV-PCAT", "Product category export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importProductCategoryFromCsv(InputStream csvInputStream) throws IOException {

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
                    CreateProductCategoryRequest request = new CreateProductCategoryRequest();
                    request.setName(record.get("NAME"));
                    request.setDescription(record.get("DESCRIPTION"));

                    ProductCategoryResponse response = create(request, java.util.Locale.ENGLISH, "IMPORT_SCRIPT");
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
                ? "Import completed successfully. " + success + " out of " + total + " product categories imported."
                : "Import failed. No product categories were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private org.springframework.data.jpa.domain.Specification<ProductCategory> addToSpec(
            org.springframework.data.jpa.domain.Specification<ProductCategory> spec,
            java.util.function.Function<EntityStatus, org.springframework.data.jpa.domain.Specification<ProductCategory>> predicateMethod) {
        return spec == null ? predicateMethod.apply(EntityStatus.DELETED) : spec.and(predicateMethod.apply(EntityStatus.DELETED));
    }

    private org.springframework.data.jpa.domain.Specification<ProductCategory> addToSpec(
            String aString,
            org.springframework.data.jpa.domain.Specification<ProductCategory> spec,
            java.util.function.Function<String, org.springframework.data.jpa.domain.Specification<ProductCategory>> predicateMethod) {
        if (aString == null || aString.trim().isEmpty()) return spec;
        String value = aString.toUpperCase();
        return spec == null ? predicateMethod.apply(value) : spec.and(predicateMethod.apply(value));
    }

    private ProductCategoryResponse buildProductCategoryResponse(int statusCode, boolean isSuccess, String message,
                                                                 ProductCategoryDto productCategoryDto,
                                                                 List<ProductCategoryDto> productCategoryDtoList,
                                                                 List<String> errorMessages) {
        ProductCategoryResponse response = new ProductCategoryResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setProductCategoryDto(productCategoryDto);
        response.setProductCategoryDtoList(productCategoryDtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private ProductCategoryResponse buildProductCategoryResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                                           ProductCategoryDto productCategoryDto,
                                                                           List<ProductCategoryDto> productCategoryDtoList,
                                                                           List<String> errorMessages) {
        return buildProductCategoryResponse(statusCode, isSuccess, message, productCategoryDto, productCategoryDtoList,
                errorMessages);
    }
}
