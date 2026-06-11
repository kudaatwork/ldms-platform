package projectlx.inventory.management.business.logic.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.business.auditable.api.ProductDocumentServiceAuditable;
import projectlx.inventory.management.business.logic.api.ProductDocumentService;
import projectlx.inventory.management.business.validator.api.ProductDocumentServiceValidator;
import projectlx.inventory.management.clients.FileUploadServiceClient;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.ProductDocument;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.ProductDocumentRepository;
import projectlx.inventory.management.repository.specification.ProductDocumentSpecification;
import projectlx.inventory.management.utils.dtos.ProductDocumentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ProductDocumentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateProductDocumentRequest;
import projectlx.inventory.management.utils.requests.EditProductDocumentRequest;
import projectlx.inventory.management.utils.responses.ProductDocumentResponse;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Transactional
public class ProductDocumentServiceImpl implements ProductDocumentService {

    private final ProductDocumentServiceValidator validator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final ProductRepository productRepository;
    private final ProductDocumentRepository productDocumentRepository;
    private final ProductDocumentServiceAuditable auditable;
    private final FileUploadServiceClient fileUploadServiceClient;
    private final ObjectMapper objectMapper;

    private static final String[] HEADERS = {"ID", "PRODUCT_ID", "DOCUMENT_ID", "EXPIRES_AT", "CREATED_AT", "UPDATED_AT", "ENTITY_STATUS"};

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public ProductDocumentResponse create(CreateProductDocumentRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isCreateProductDocumentRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_DOCUMENT_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        // Retrieve product
        Optional<Product> productOpt = productRepository.findByIdAndEntityStatusNot(request.getProductId(), EntityStatus.DELETED);

        if (productOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        Product product = productOpt.get();

        // Perform upload first to get document ID (required for persistence due to NOT NULL constraint)
        MultipartFile file = request.getDocumentUpload();

        if (file == null || file.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_DOCUMENT_UPLOAD_MISSING.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        String documentIdStr = null;
        try {
            // Build minimal request map following ProductServiceImpl style
            Map<String, Object> uploadMeta = new HashMap<>();
            uploadMeta.put("fileType", FileType.PRODUCT.name());
            uploadMeta.put("expiresAt", request.getExpiresAt());

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("filesMetadata", List.of(uploadMeta));
            requestMap.put("ownerType", OwnerType.ORGANIZATION.getOwnerType());
            requestMap.put("ownerId", product.getId());

            String fileUploadRequestJson = objectMapper.writeValueAsString(requestMap);
            FileUploadResponse fileUploadResponse = fileUploadServiceClient.upload(List.of(file), fileUploadRequestJson);

            if (fileUploadResponse != null && fileUploadResponse.isSuccess()) {

                Long docId = null;

                if (fileUploadResponse.getFileUploadDto() != null) {
                    docId = fileUploadResponse.getFileUploadDto().getId();
                } else if (fileUploadResponse.getFileUploadDtoList() != null && !fileUploadResponse.getFileUploadDtoList().isEmpty()) {
                    FileUploadDto first = fileUploadResponse.getFileUploadDtoList().get(0);
                    docId = first.getId();
                }

                if (docId != null) {
                    documentIdStr = String.valueOf(docId);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize file upload request", e);
        } catch (Exception ex) {
            log.error("Failed to upload file", ex);
        }

        if (documentIdStr == null) {
            // Upload did not yield a document id; do not persist to avoid NOT NULL violation
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_DOCUMENT_UPLOAD_MISSING.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        // Build and save entity
        ProductDocument toSave = new ProductDocument();
        toSave.setProduct(product);
        toSave.setName(request.getName() != null ? request.getName().toUpperCase() : null);
        toSave.setDescription(request.getDescription());
        toSave.setExpiresAt(request.getExpiresAt());

        toSave.setDocumentId(documentIdStr);

        ProductDocument saved = auditable.create(toSave, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductDocumentDto dto = modelMapper.map(saved, ProductDocumentDto.class);
        if (saved.getProduct() != null) dto.setProductId(saved.getProduct().getId());

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public ProductDocumentResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        
        if (!validatorDto.getSuccess()) {
            
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            
            return buildResponseWithErrors(400, false, message, null, null, 
                    validatorDto.getErrorMessages());
        }

        Optional<ProductDocument> docOpt = productDocumentRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        
        if (docOpt.isEmpty()) {
            
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_NOT_FOUND.getCode(), new String[]{}, 
                    locale);
           
            return buildResponse(404, false, message, null, null, null);
        }

        ProductDocument doc = docOpt.get();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductDocumentDto dto = modelMapper.map(doc, ProductDocumentDto.class);
        if (doc.getProduct() != null) dto.setProductId(doc.getProduct().getId());

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_RETRIEVED_SUCCESSFULLY.getCode(), 
                new String[]{}, locale);
        
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public ProductDocumentResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<ProductDocument> list = productDocumentRepository.findByEntityStatusNot(EntityStatus.DELETED);
       
        if (list.isEmpty()) {
            
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_NOT_FOUND.getCode(), new String[]{}, 
                    locale);
           
            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<ProductDocumentDto> dtoList = list.stream().map(d -> {
            ProductDocumentDto dto = modelMapper.map(d, ProductDocumentDto.class);
            if (d.getProduct() != null) dto.setProductId(d.getProduct().getId());
            return dto;
        }).toList();

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, 
                locale);
        
        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    public ProductDocumentResponse update(EditProductDocumentRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);
        
        if (!validatorDto.getSuccess()) {
            
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_DOCUMENT_INVALID_REQUEST.getCode(), 
                    new String[]{}, locale);
            
            return buildResponseWithErrors(400, false, message, null, null, 
                    validatorDto.getErrorMessages());
        }

        Optional<ProductDocument> existingOpt = productDocumentRepository.findByIdAndEntityStatusNot(request.getProductDocumentId(), EntityStatus.DELETED);
        
        if (existingOpt.isEmpty()) {
            
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_NOT_FOUND.getCode(), new String[]{}, 
                    locale);
            
            return buildResponse(400, false, message, null, null, null);
        }

        ProductDocument toEdit = existingOpt.get();

        // Optionally change associated product
        if (request.getProductId() != null) {
            
            Optional<Product> productOpt = productRepository.findByIdAndEntityStatusNot(request.getProductId(), 
                    EntityStatus.DELETED);
            
            if (productOpt.isEmpty()) {
                
                message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);
                
                return buildResponse(400, false, message, null, null, null);
            }
            
            toEdit.setProduct(productOpt.get());
        }

        // Optional file upload to replace documentId
        if (request.getDocumentUpload() != null && !request.getDocumentUpload().isEmpty()) {
            
            try {
               
                Map<String, Object> uploadMeta = new HashMap<>();
                uploadMeta.put("fileType", FileType.PRODUCT.name());
                uploadMeta.put("expiresAt", request.getExpiresAt());

                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("filesMetadata", List.of(uploadMeta));
                requestMap.put("ownerType", OwnerType.ORGANIZATION.getOwnerType());
                requestMap.put("ownerId", toEdit.getId());

                String fileUploadRequestJson = objectMapper.writeValueAsString(requestMap);
                FileUploadResponse fileUploadResponse = fileUploadServiceClient.upload(List.of(request.getDocumentUpload()), fileUploadRequestJson);

                if (fileUploadResponse != null && fileUploadResponse.isSuccess()) {
                    
                    Long docId = null;
                    if (fileUploadResponse.getFileUploadDto() != null) {
                        docId = fileUploadResponse.getFileUploadDto().getId();
                    } else if (fileUploadResponse.getFileUploadDtoList() != null && !fileUploadResponse.getFileUploadDtoList().isEmpty()) {
                        FileUploadDto first = fileUploadResponse.getFileUploadDtoList().get(0);
                        docId = first.getId();
                    }
                   
                    if (docId != null) {
                        toEdit.setDocumentId(String.valueOf(docId));
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to upload file during product document update", ex);
            }
        } else if (request.getDocumentId() != null) {
            // allow direct documentId update
            toEdit.setDocumentId(request.getDocumentId());
        }

        if (request.getName() != null && !request.getName().isEmpty()) toEdit.setName(request.getName().toUpperCase());
        if (request.getDescription() != null) toEdit.setDescription(request.getDescription());
        if (request.getExpiresAt() != null) toEdit.setExpiresAt(request.getExpiresAt());
        if (request.getEntityStatus() != null) toEdit.setEntityStatus(request.getEntityStatus());

        ProductDocument saved = auditable.update(toEdit, locale, username);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductDocumentDto dto = modelMapper.map(saved, ProductDocumentDto.class);
        if (saved.getProduct() != null) dto.setProductId(saved.getProduct().getId());

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public ProductDocumentResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        
        if (!validatorDto.getSuccess()) {
          
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
         
            return buildResponseWithErrors(400, false, message, null, null, 
                    validatorDto.getErrorMessages());
        }

        Optional<ProductDocument> existingOpt = productDocumentRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        
        if (existingOpt.isEmpty()) {
           
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_NOT_FOUND.getCode(), new String[]{}, 
                    locale);
          
            return buildResponse(404, false, message, null, null, null);
        }

        ProductDocument toDelete = existingOpt.get();
        toDelete.setEntityStatus(EntityStatus.DELETED);
        ProductDocument deleted = auditable.delete(toDelete, locale);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        ProductDocumentDto dto = modelMapper.map(deleted, ProductDocumentDto.class);
        if (deleted.getProduct() != null) dto.setProductId(deleted.getProduct().getId());

        message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_DELETED_SUCCESSFULLY.getCode(), 
                new String[]{}, locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public ProductDocumentResponse findByMultipleFilters(ProductDocumentMultipleFiltersRequest request, String username, Locale locale) {
        return null;
    }

    @Override
    public byte[] exportToCsv(List<ProductDocumentDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (ProductDocumentDto item : items) {
            sb.append(item.getId() != null ? item.getId() : 0).append(",")
              .append(item.getProductId() != null ? item.getProductId() : 0).append(",")
              .append(safe(item.getDocumentId())).append(",")
              .append(item.getExpiresAt() != null ? item.getExpiresAt() : "").append(",")
              .append(item.getCreatedAt() != null ? item.getCreatedAt() : "").append(",")
              .append(item.getUpdatedAt() != null ? item.getUpdatedAt() : "").append(",")
              .append(item.getEntityStatus() != null ? item.getEntityStatus().name() : "")
              .append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<ProductDocumentDto> items) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Product Documents");

        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;
        for (ProductDocumentDto item : items) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(1).setCellValue(item.getProductId() != null ? item.getProductId() : 0);
            row.createCell(2).setCellValue(safe(item.getDocumentId()));
            row.createCell(3).setCellValue(item.getExpiresAt() != null ? item.getExpiresAt().toString() : "");
            row.createCell(4).setCellValue(item.getCreatedAt() != null ? item.getCreatedAt().toString() : "");
            row.createCell(5).setCellValue(item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : "");
            row.createCell(6).setCellValue(item.getEntityStatus() != null ? item.getEntityStatus().name() : "");
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<ProductDocumentDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (ProductDocumentDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    String.valueOf(item.getProductId() != null ? item.getProductId() : 0),
                    safe(item.getDocumentId()),
                    item.getExpiresAt() != null ? item.getExpiresAt().toString() : "",
                    item.getCreatedAt() != null ? item.getCreatedAt().toString() : "",
                    item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : "",
                    item.getEntityStatus() != null ? item.getEntityStatus().name() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf("Product Documents", "INV-PDC",
                "Product document export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importProductDocumentFromCsv(InputStream csvInputStream) throws IOException {

        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);

             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            java.util.List<CSVRecord> records = csvParser.getRecords();
            total = records.size();

            for (org.apache.commons.csv.CSVRecord record : records) {
                try {
                    CreateProductDocumentRequest request = new CreateProductDocumentRequest();
                    // CSV columns expected: PRODUCT_ID, NAME, DESCRIPTION, EXPIRES_AT
                    String productIdStr = record.isMapped("PRODUCT_ID") ? record.get("PRODUCT_ID") : null;
                    if (productIdStr != null && !productIdStr.trim().isEmpty()) {
                        request.setProductId(Long.valueOf(productIdStr.trim()));
                    }
                    request.setName(record.isMapped("NAME") ? record.get("NAME") : null);
                    request.setDescription(record.isMapped("DESCRIPTION") ? record.get("DESCRIPTION") : null);
                    String expiresAt = record.isMapped("EXPIRES_AT") ? record.get("EXPIRES_AT") : null;
                    if (expiresAt != null && !expiresAt.trim().isEmpty()) {
                        try {
                            request.setExpiresAt(java.time.LocalDate.parse(expiresAt.trim()));
                        } catch (Exception e) {
                            // ignore parse issue; validator will handle missing/invalid as needed
                        }
                    }

                    ProductDocumentResponse response = create(request, java.util.Locale.ENGLISH, "IMPORT_SCRIPT");
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
                ? "Import completed successfully. " + success + " out of " + total + " product documents imported."
                : "Import failed. No product documents were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private ProductDocumentResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                                  ProductDocumentDto dto, List<ProductDocumentDto> dtoList,
                                                  List<String> errorMessages) {
        ProductDocumentResponse response = new ProductDocumentResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setProductDocumentDto(dto);
        response.setProductDocumentDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private ProductDocumentResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                            ProductDocumentDto dto, List<ProductDocumentDto> dtoList,
                                                            List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }
}
