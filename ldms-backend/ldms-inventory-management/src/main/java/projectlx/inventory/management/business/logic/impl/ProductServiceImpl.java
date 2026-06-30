package projectlx.inventory.management.business.logic.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.business.auditable.api.ProductServiceAuditable;
import projectlx.inventory.management.business.logic.api.ProductService;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.logic.support.InventoryOrganizationScopeSupport;
import projectlx.inventory.management.business.validator.api.ProductServiceValidator;
import projectlx.inventory.management.clients.FileUploadServiceClient;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.ProductCategory;
import projectlx.inventory.management.model.ProductSubCategory;
import projectlx.inventory.management.repository.ProductCategoryRepository;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.ProductSubCategoryRepository;
import projectlx.inventory.management.repository.specification.ProductSpecification;
import projectlx.inventory.management.utils.dtos.ProductDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ProductMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateProductRequest;
import projectlx.inventory.management.utils.requests.EditProductRequest;
import projectlx.inventory.management.utils.responses.ProductResponse;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.requests.FileUploadRequest;
import projectlx.co.zw.shared_library.utils.requests.SingleFileUploadRequest;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import projectlx.inventory.management.model.UnitOfMeasure;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.function.Function;

@Slf4j
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductServiceValidator validator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSubCategoryRepository productSubCategoryRepository;
    private final ProductServiceAuditable auditable;
    private final FileUploadServiceClient fileUploadServiceClient;
    private final ObjectMapper objectMapper;
    private final InventoryOrganizationScopeSupport organizationScopeSupport;

    private static final String[] HEADERS = {"ID", "NAME", "PRODUCT_CODE", "BARCODE", "PRICE", "UNIT_OF_MEASURE",
            "CATEGORY_ID", "SUBCATEGORY_ID", "SUPPLIER_ID", "MANUFACTURER", "EXPIRES_AT"};
    private static final String[] CSV_HEADERS = {"NAME", "DESCRIPTION", "PRODUCT_CODE", "BARCODE", "PRICE",
            "UNIT_OF_MEASURE", "PRODUCT_CATEGORY_ID", "PRODUCT_SUB_CATEGORY_ID", "SUPPLIER_ID", "MANUFACTURER",
            "EXPIRES_AT"};

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public ProductResponse create(CreateProductRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isCreateProductRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        // Check uniqueness by product code
        Optional<Product> existing = productRepository.findByProductCodeAndEntityStatusNot(request.getProductCode(),
                EntityStatus.DELETED);

        if (existing.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_ALREADY_EXISTS.getCode(), new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        String normalizedBarcode = normalizeBarcode(request.getBarcode());
        if (normalizedBarcode != null) {
            Optional<Product> existingBarcode = productRepository
                    .lookupByBarcodeTrimmedAndEntityStatusNot(normalizedBarcode, EntityStatus.DELETED);
            if (existingBarcode.isPresent()) {
                message = "A product with this barcode already exists";
                return buildResponse(400, false, message, null, null, null);
            }
        }

        // Retrieve category
        Optional<ProductCategory> categoryOpt = productCategoryRepository.findByIdAndEntityStatusNot(request.getProductCategoryId(),
                EntityStatus.DELETED);

        if (categoryOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(400, false, message, null, null, null);
        }

        // Retrieve subcategory if provided
        ProductSubCategory subCategory = null;

        if (request.getProductSubCategoryId() != null) {

            Optional<ProductSubCategory> subOpt = productSubCategoryRepository.findByIdAndEntityStatusNot(
                    request.getProductSubCategoryId(), EntityStatus.DELETED);

            if (subOpt.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_NOT_FOUND.getCode(),
                        new String[]{}, locale);

                return buildResponse(400, false, message, null, null, null);
            }
            subCategory = subOpt.get();
        }

        // Build product entity
        Product productToBeSaved = new Product();
        productToBeSaved.setName(request.getName().toUpperCase());
        productToBeSaved.setDescription(request.getDescription());
        productToBeSaved.setProductCode(request.getProductCode());
        productToBeSaved.setBarcode(normalizedBarcode);
        productToBeSaved.setPrice(request.getPrice());
        productToBeSaved.setUnitOfMeasure(request.getUnitOfMeasure());
        productToBeSaved.setSupplierId(request.getSupplierId());
        productToBeSaved.setManufacturer(request.getManufacturer());
        productToBeSaved.setExpiresAt(request.getExpiresAt());
        productToBeSaved.setCategory(categoryOpt.get());
        productToBeSaved.setSubcategory(subCategory);

        // Save first to get ID (if we decide to use productId as owner for uploads)
        Product saved = auditable.create(productToBeSaved, locale, username);

        // Handle optional image upload
        if (request.getImageUpload() != null && !request.getImageUpload().isEmpty()) {

            try {
                FileUploadRequest fileUploadRequest = new FileUploadRequest();
                List<SingleFileUploadRequest> filesMetadata = new ArrayList<>();

                SingleFileUploadRequest uploadRequest = new SingleFileUploadRequest();
                uploadRequest.setFile(request.getImageUpload());
                uploadRequest.setFileType(FileType.PRODUCT.name());
                filesMetadata.add(uploadRequest);

                fileUploadRequest.setFilesMetadata(filesMetadata);
                // Following OrganizationServiceImpl pattern; we associate the file to the Product record just created
                fileUploadRequest.setOwnerType(OwnerType.ORGANIZATION.getOwnerType());
                fileUploadRequest.setOwnerId(saved.getId());

                List<MultipartFile> files = List.of(request.getImageUpload());

                // Serialize minimal metadata map like OrganizationServiceImpl
                List<Map<String, Object>> metadataList = new ArrayList<>();
                Map<String, Object> map = new HashMap<>();
                map.put("fileType", uploadRequest.getFileType());
                map.put("expiresAt", uploadRequest.getExpiresAt());
                metadataList.add(map);

                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("filesMetadata", metadataList);
                requestMap.put("ownerType", fileUploadRequest.getOwnerType());
                requestMap.put("ownerId", fileUploadRequest.getOwnerId());

                String fileUploadRequestJson = objectMapper.writeValueAsString(requestMap);
                FileUploadResponse fileUploadResponse = fileUploadServiceClient.upload(files, fileUploadRequestJson);

                if (fileUploadResponse != null && fileUploadResponse.isSuccess()) {

                    Long imageId = null;

                    if (fileUploadResponse.getFileUploadDto() != null) {
                        imageId = fileUploadResponse.getFileUploadDto().getId();
                    } else if (fileUploadResponse.getFileUploadDtoList() != null &&
                            !fileUploadResponse.getFileUploadDtoList().isEmpty()) {
                        FileUploadDto first = fileUploadResponse.getFileUploadDtoList().get(0);
                        imageId = first.getId();
                    }

                    if (imageId != null) {
                        saved.setImageId(imageId);
                        saved = auditable.update(saved, locale, username);
                    }
                }
            } catch (JsonProcessingException e) {
                // If serialization fails, proceed without image
                log.error("Failed to serialize file upload request", e);
            } catch (Exception ex) {
                // If upload fails, proceed without image
                log.error("Failed to upload file", ex);
            }
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductDto dto = modelMapper.map(saved, ProductDto.class);
        // Manually set categoryId and subcategoryId if not mapped
        enrichProductDto(saved, dto);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public ProductResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<Product> productRetrieved = productRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (productRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Product product = productRetrieved.get();
        if (!organizationScopeSupport.isSystemUser(username)) {
            Long orgId = organizationScopeSupport.resolveOrganizationId(username, locale);
            if (orgId == null || !orgId.equals(product.getSupplierId())) {
                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);
                return buildResponse(404, false, message, null, null, null);
            }
        }
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductDto dto = modelMapper.map(product, ProductDto.class);

        enrichProductDto(product, dto);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public ProductResponse findAllAsList(Locale locale, String username) {

        List<Product> list;
        if (organizationScopeSupport.isSystemUser(username)) {
            list = productRepository.findByEntityStatusNot(EntityStatus.DELETED);
        } else {
            Long orgId = organizationScopeSupport.resolveOrganizationId(username, locale);
            if (orgId == null) {
                String message = messageService.getMessage(
                        I18Code.MESSAGE_PRODUCT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
                ProductResponse empty = buildResponse(200, true, message, null, List.of(), null);
                empty.setProductDtoList(List.of());
                return empty;
            }
            if (organizationScopeSupport.isCustomerOrganization(orgId, locale)) {
                // Own catalogue (supplierId = org) plus supplier-fed SKUs already stocked at visible warehouses.
                Set<Long> productIds = new HashSet<>();
                productRepository.findBySupplierIdAndEntityStatusNot(orgId, EntityStatus.DELETED)
                        .forEach(product -> productIds.add(product.getId()));
                Set<Long> warehouseIds = organizationScopeSupport.visibleWarehouseIds(orgId);
                productIds.addAll(organizationScopeSupport.distinctProductIdsAtWarehouses(warehouseIds));
                list = productIds.isEmpty()
                        ? List.of()
                        : productRepository.findByIdInAndEntityStatusNot(productIds, EntityStatus.DELETED);
            } else {
                list = productRepository.findBySupplierIdAndEntityStatusNot(orgId, EntityStatus.DELETED);
            }
        }

        if (list.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_PRODUCT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
            ProductResponse empty = buildResponse(200, true, message, null, List.of(), null);
            empty.setProductDtoList(List.of());
            return empty;
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        List<ProductDto> dtoList = list.stream().map(p -> {
            ProductDto d = modelMapper.map(p, ProductDto.class);
            enrichProductDto(p, d);
            return d;
        }).toList();

        String message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);
        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    public ProductResponse update(EditProductRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_INVALID_REQUEST.getCode(), new String[]{},
                    locale);
            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<Product> existingOpt = productRepository.findByIdAndEntityStatusNot(request.getProductId(),
                EntityStatus.DELETED);

        if (existingOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        Product toEdit = existingOpt.get();
        if (!organizationScopeSupport.isOwnedByCaller(toEdit.getSupplierId(), username, locale)) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        // Uniqueness check for product code if provided
        if (request.getProductCode() != null && !request.getProductCode().isEmpty()) {

            Optional<Product> byCode = productRepository.findByProductCodeAndEntityStatusNot(request.getProductCode(),
                    EntityStatus.DELETED);

            if (byCode.isPresent() && !byCode.get().getId().equals(request.getProductId())) {

                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_ALREADY_EXISTS.getCode(), new String[]{},
                        locale);

                return buildResponse(400, false, message, null, null, null);
            }

            toEdit.setProductCode(request.getProductCode());
        }

        if (request.getBarcode() != null) {
            String normalizedBarcode = normalizeBarcode(request.getBarcode());
            if (normalizedBarcode != null) {
                Optional<Product> byBarcode = productRepository
                        .lookupByBarcodeTrimmedAndEntityStatusNot(normalizedBarcode, EntityStatus.DELETED);
                if (byBarcode.isPresent() && !byBarcode.get().getId().equals(request.getProductId())) {
                    message = "A product with this barcode already exists";
                    return buildResponse(400, false, message, null, null, null);
                }
            }
            toEdit.setBarcode(normalizedBarcode);
        }

        // Update category if provided
        if (request.getCategoryId() != null) {

            Optional<ProductCategory> categoryOpt = productCategoryRepository.findByIdAndEntityStatusNot(request.getCategoryId(),
                    EntityStatus.DELETED);

            if (categoryOpt.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                        locale);

                return buildResponse(400, false, message, null, null, null);
            }

            toEdit.setCategory(categoryOpt.get());
        }

        // Update subcategory if provided
        if (request.getSubcategoryId() != null) {

            Optional<ProductSubCategory> subOpt = productSubCategoryRepository.findByIdAndEntityStatusNot(request.getSubcategoryId(),
                    EntityStatus.DELETED);

            if (subOpt.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_NOT_FOUND.getCode(), new String[]{},
                        locale);

                return buildResponse(400, false, message, null, null, null);
            }

            toEdit.setSubcategory(subOpt.get());
        }

        if (request.getName() != null && !request.getName().isEmpty()) toEdit.setName(request.getName().toUpperCase());
        if (request.getDescription() != null) toEdit.setDescription(request.getDescription());
        if (request.getPrice() != null) toEdit.setPrice(request.getPrice());
        if (request.getUnitOfMeasure() != null) toEdit.setUnitOfMeasure(request.getUnitOfMeasure());
        if (request.getSupplierId() != null) toEdit.setSupplierId(request.getSupplierId());
        if (request.getManufacturer() != null) toEdit.setManufacturer(request.getManufacturer());
        if (request.getExpiresAt() != null) toEdit.setExpiresAt(request.getExpiresAt());
        if (request.getEntityStatus() != null) toEdit.setEntityStatus(request.getEntityStatus());

        // Handle optional new image upload replacing the old one
        if (request.getImageUpload() != null && !request.getImageUpload().isEmpty()) {

            try {

                FileUploadRequest fileUploadRequest = new FileUploadRequest();
                List<SingleFileUploadRequest> filesMetadata = new ArrayList<>();
                SingleFileUploadRequest uploadRequest = new SingleFileUploadRequest();
                uploadRequest.setFile(request.getImageUpload());
                uploadRequest.setFileType(FileType.PRODUCT.name());
                filesMetadata.add(uploadRequest);
                fileUploadRequest.setFilesMetadata(filesMetadata);
                fileUploadRequest.setOwnerType(OwnerType.ORGANIZATION.getOwnerType());
                fileUploadRequest.setOwnerId(toEdit.getId());

                List<MultipartFile> files = List.of(request.getImageUpload());

                List<Map<String, Object>> metadataList = new ArrayList<>();
                Map<String, Object> map = new HashMap<>();
                map.put("fileType", uploadRequest.getFileType());
                map.put("expiresAt", uploadRequest.getExpiresAt());
                metadataList.add(map);

                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("filesMetadata", metadataList);
                requestMap.put("ownerType", fileUploadRequest.getOwnerType());
                requestMap.put("ownerId", fileUploadRequest.getOwnerId());

                String fileUploadRequestJson = objectMapper.writeValueAsString(requestMap);
                FileUploadResponse fileUploadResponse = fileUploadServiceClient.upload(files, fileUploadRequestJson);

                if (fileUploadResponse != null && fileUploadResponse.isSuccess()) {

                    Long imageId = null;

                    if (fileUploadResponse.getFileUploadDto() != null) {
                        imageId = fileUploadResponse.getFileUploadDto().getId();
                    } else if (fileUploadResponse.getFileUploadDtoList() != null && !fileUploadResponse.getFileUploadDtoList().isEmpty()) {
                        FileUploadDto first = fileUploadResponse.getFileUploadDtoList().get(0);
                        imageId = first.getId();
                    }

                    if (imageId != null) {
                        toEdit.setImageId(imageId);
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to upload file during product update", ex);
            }
        } else if (request.getImageId() != null) {
            // Allow direct imageId update if provided
            toEdit.setImageId(request.getImageId());
        }

        Product saved = auditable.update(toEdit, locale, username);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductDto dto = modelMapper.map(saved, ProductDto.class);
        enrichProductDto(saved, dto);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public ProductResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<Product> existingOpt = productRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (existingOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Product toDelete = existingOpt.get();
        if (!organizationScopeSupport.isOwnedByCaller(toDelete.getSupplierId(), username, locale)) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }
        toDelete.setEntityStatus(EntityStatus.DELETED);
        Product deleted = auditable.delete(toDelete, locale);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductDto dto = modelMapper.map(deleted, ProductDto.class);
        enrichProductDto(deleted, dto);

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public ProductResponse findByMultipleFilters(ProductMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<Product> spec = null;
        // Start with deleted spec
        spec = addToSpec(spec, ProductSpecification::deleted);

        // Validate request
        ValidatorDto validatorDto = validator.isRequestValidToRetrieveProductByMultipleFilters(request, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (!organizationScopeSupport.isSystemUser(username)) {
            Long callerOrgId = organizationScopeSupport.resolveOrganizationId(username, locale);
            if (callerOrgId == null) {
                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_RETRIEVED_SUCCESSFULLY.getCode(),
                        new String[]{}, locale);
                ProductResponse empty = buildResponse(200, true, message, null, null, null);
                empty.setProductDtoPage(new PageImpl<>(List.of(), pageable, 0));
                return empty;
            }
            Long requestedSupplierId = request.getSupplierId();
            if (requestedSupplierId == null || requestedSupplierId <= 0) {
                request.setSupplierId(callerOrgId);
            } else if (!requestedSupplierId.equals(callerOrgId)
                    && !organizationScopeSupport.isCustomerOrganization(callerOrgId, locale)) {
                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);
                return buildResponse(404, false, message, null, null, null);
            }
        }

        // Apply 'name' filter when valid
        ValidatorDto nameValidatorDto = validator.isStringValid(request.getName(), locale);

        if (nameValidatorDto.getSuccess()) {
            spec = addToSpec(request.getName(), spec, ProductSpecification::nameLike);
        }

        // Apply 'productCode' filter when valid
        ValidatorDto codeValidatorDto = validator.isStringValid(request.getProductCode(), locale);
        if (codeValidatorDto.getSuccess()) {
            spec = addToSpec(request.getProductCode(), spec, ProductSpecification::productCodeEquals);
        }

        // Apply 'entityStatus' filter if provided
        if (request.getEntityStatus() != null) {
            spec = (spec == null)
                    ? ProductSpecification.entityStatusEquals(request.getEntityStatus())
                    : spec.and(ProductSpecification.entityStatusEquals(request.getEntityStatus()));
        }

        if (request.getSupplierId() != null && request.getSupplierId() > 0) {
            spec = (spec == null)
                    ? ProductSpecification.supplierIdEquals(request.getSupplierId())
                    : spec.and(ProductSpecification.supplierIdEquals(request.getSupplierId()));
        }

        // Apply 'searchValue' (any) when valid
        ValidatorDto searchValueValidatorDto = validator.isStringValid(request.getSearchValue(), locale);
        if (searchValueValidatorDto.getSuccess()) {
            spec = addToSpec(request.getSearchValue(), spec, ProductSpecification::any);
        }

        // Page bounds check
        long totalCount = productRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());
        if (request.getPage() >= maxPage) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_PAGE_OUT_OF_BOUNDS.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Page<Product> result = productRepository.findAll(spec, pageable);
        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
            ProductResponse response = buildResponse(200, true, message, null, null, null);
            response.setProductDtoPage(new PageImpl<>(List.of(), pageable, totalCount));
            return response;
        }

        // Map to DTO page
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<ProductDto> dtoPage = result.map(p -> modelMapper.map(p, ProductDto.class));

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        ProductResponse response = buildResponse(200, true, message, null, null, null);
        response.setProductDtoPage(dtoPage);
        return response;
    }

    private Specification<Product> addToSpec(
            Specification<Product> spec,
            Function<EntityStatus, Specification<Product>> predicateMethod) {
        return spec == null ? predicateMethod.apply(EntityStatus.DELETED) : spec.and(predicateMethod.apply(EntityStatus.DELETED));
    }

    private Specification<Product> addToSpec(
            String aString,
            Specification<Product> spec,
            Function<String, Specification<Product>> predicateMethod) {
        if (aString == null || aString.trim().isEmpty()) return spec;
        String value = aString.toUpperCase();
        return spec == null ? predicateMethod.apply(value) : spec.and(predicateMethod.apply(value));
    }

    @Override
    public byte[] exportToCsv(List<ProductDto> items) {
        items = InventoryExportSupport.nullSafe(items);
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (ProductDto item : items) {
            sb.append(item.getId()).append(",")
              .append(safe(item.getName())).append(",")
              .append(safe(item.getProductCode())).append(",")
              .append(safe(item.getBarcode())).append(",")
              .append(item.getPrice() != null ? item.getPrice() : "").append(",")
              .append(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : "").append(",")
              .append(item.getCategoryId() != null ? item.getCategoryId() : "").append(",")
              .append(item.getSubcategoryId() != null ? item.getSubcategoryId() : "").append(",")
              .append(item.getSupplierId() != null ? item.getSupplierId() : "").append(",")
              .append(safe(item.getManufacturer())).append(",")
              .append(item.getExpiresAt() != null ? item.getExpiresAt() : "")
              .append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<ProductDto> items) throws IOException {
        items = InventoryExportSupport.nullSafe(items);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;
        for (ProductDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(1).setCellValue(safe(item.getName()));
            row.createCell(2).setCellValue(safe(item.getProductCode()));
            row.createCell(3).setCellValue(safe(item.getBarcode()));
            row.createCell(4).setCellValue(item.getPrice() != null ? item.getPrice().doubleValue() : 0);
            row.createCell(5).setCellValue(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : "");
            row.createCell(6).setCellValue(item.getCategoryId() != null ? item.getCategoryId() : 0);
            row.createCell(7).setCellValue(item.getSubcategoryId() != null ? item.getSubcategoryId() : 0);
            row.createCell(8).setCellValue(item.getSupplierId() != null ? item.getSupplierId() : 0);
            row.createCell(9).setCellValue(safe(item.getManufacturer()));
            row.createCell(10).setCellValue(item.getExpiresAt() != null ? item.getExpiresAt().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<ProductDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (ProductDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    safe(item.getName()),
                    safe(item.getProductCode()),
                    safe(item.getBarcode()),
                    item.getPrice() != null ? item.getPrice().toString() : "",
                    item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : "",
                    String.valueOf(item.getCategoryId() != null ? item.getCategoryId() : 0),
                    String.valueOf(item.getSubcategoryId() != null ? item.getSubcategoryId() : 0),
                    String.valueOf(item.getSupplierId() != null ? item.getSupplierId() : 0),
                    safe(item.getManufacturer()),
                    item.getExpiresAt() != null ? item.getExpiresAt().toString() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf("Products", "INV-PRD",
                "Product catalogue export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importProductFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            List<CSVRecord> records = csvParser.getRecords();
            total = records.size();

            for (CSVRecord record : records) {
                try {
                    CreateProductRequest request = new CreateProductRequest();
                    request.setName(record.isMapped("NAME") ? record.get("NAME") : null);
                    request.setDescription(record.isMapped("DESCRIPTION") ? record.get("DESCRIPTION") : null);
                    request.setProductCode(record.isMapped("PRODUCT_CODE") ? record.get("PRODUCT_CODE") : null);
                    request.setBarcode(record.isMapped("BARCODE") ? record.get("BARCODE") : null);
                    String priceStr = record.isMapped("PRICE") ? record.get("PRICE") : null;
                    if (priceStr != null && !priceStr.trim().isEmpty()) {
                        request.setPrice(new BigDecimal(priceStr.trim()));
                    }
                    String uom = record.isMapped("UNIT_OF_MEASURE") ? record.get("UNIT_OF_MEASURE") : null;
                    request.setUnitOfMeasure(UnitOfMeasure.fromCsvValue(uom));
                    String catId = record.isMapped("PRODUCT_CATEGORY_ID") ? record.get("PRODUCT_CATEGORY_ID") : null;
                    if (catId != null && !catId.trim().isEmpty()) {
                        request.setProductCategoryId(Long.valueOf(catId.trim()));
                    }
                    String subId = record.isMapped("PRODUCT_SUB_CATEGORY_ID") ? record.get("PRODUCT_SUB_CATEGORY_ID") : null;
                    if (subId != null && !subId.trim().isEmpty()) {
                        request.setProductSubCategoryId(Long.valueOf(subId.trim()));
                    }
                    String supplierId = record.isMapped("SUPPLIER_ID") ? record.get("SUPPLIER_ID") : null;
                    if (supplierId != null && !supplierId.trim().isEmpty()) {
                        request.setSupplierId(Long.valueOf(supplierId.trim()));
                    }
                    request.setManufacturer(record.isMapped("MANUFACTURER") ? record.get("MANUFACTURER") : null);
                    String expiresAt = record.isMapped("EXPIRES_AT") ? record.get("EXPIRES_AT") : null;
                    if (expiresAt != null && !expiresAt.trim().isEmpty()) {
                        try {
                            request.setExpiresAt(LocalDate.parse(expiresAt.trim()));
                        } catch (Exception e) {
                            // ignore parsing issue; validator will not check this strictly
                        }
                    }

                    ProductResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
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
                ? "Import completed successfully. " + success + " out of " + total + " products imported."
                : "Import failed. No products were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private static String normalizeBarcode(String barcode) {
        if (barcode == null) {
            return null;
        }
        String trimmed = barcode.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void enrichProductDto(Product product, ProductDto dto) {
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        if (product.getSubcategory() != null) {
            dto.setSubcategoryId(product.getSubcategory().getId());
            dto.setSubcategoryName(product.getSubcategory().getName());
        }
    }

    private ProductResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                          ProductDto dto, List<ProductDto> dtoList, List<String> errorMessages) {
        ProductResponse response = new ProductResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setProductDto(dto);
        response.setProductDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private ProductResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                    ProductDto dto, List<ProductDto> dtoList, List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }
}
