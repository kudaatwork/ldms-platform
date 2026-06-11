package projectlx.inventory.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.service.processor.api.ProductDocumentServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.ProductDocumentDto;
import projectlx.inventory.management.utils.requests.CreateProductDocumentRequest;
import projectlx.inventory.management.utils.requests.EditProductDocumentRequest;
import projectlx.inventory.management.utils.requests.ProductDocumentMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.ProductDocumentResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/product-document")
@Tag(name = "Product Document System Resource", description = "Operations related to managing product documents (system)")
@RequiredArgsConstructor
public class ProductDocumentSystemResource {

    private final ProductDocumentServiceProcessor productDocumentServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(ProductDocumentSystemResource.class);

    @Auditable(action = "CREATE_PRODUCT_DOCUMENT")
    @PostMapping("/create")
    @Operation(summary = "Create a new product document", description = "Creates a new product document and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product document created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ProductDocumentResponse create(@Valid @ModelAttribute final CreateProductDocumentRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return productDocumentServiceProcessor.create(request, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_PRODUCT_DOCUMENT")
    @PutMapping("/update")
    @Operation(summary = "Update product document details", description = "Updates an existing product document's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product document updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product document not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ProductDocumentResponse update(@Valid @ModelAttribute final EditProductDocumentRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return productDocumentServiceProcessor.update(request, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_PRODUCT_DOCUMENT_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find product document by ID", description = "Retrieves a product document by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product document found successfully"),
            @ApiResponse(responseCode = "404", description = "Product document not found"),
            @ApiResponse(responseCode = "400", description = "Product document id supplied invalid")
    })
    public ProductDocumentResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                   final Locale locale) {
        return productDocumentServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_PRODUCT_DOCUMENT")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a product document by ID")
    public ProductDocumentResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return productDocumentServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_PRODUCT_DOCUMENTS_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all product documents", description = "Retrieves a list of all product documents")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product documents retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product document(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching product documents")
    })
    public ProductDocumentResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                             final Locale locale) {
        return productDocumentServiceProcessor.findAllAsList(locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_PRODUCT_DOCUMENTS_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find product documents by multiple filters",
            description = "Retrieves a list of product documents that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product document(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Product document(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ProductDocumentResponse findByMultipleFilters(@Valid @RequestBody ProductDocumentMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                               final Locale locale) {
        return productDocumentServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
    }

    @Auditable(action = "EXPORT_PRODUCT_DOCUMENTS")
    @PostMapping("/export")
    @Operation(summary = "Export product documents",
            description = "Exports product documents based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product documents exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody ProductDocumentMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export product documents in {} format with filters: {}", format, filters);
            ProductDocumentResponse response = productDocumentServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<ProductDocumentDto> list = InventoryExportSupport.nullSafe(response.getProductDocumentDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = productDocumentServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "product_documents.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = productDocumentServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "product_documents.xlsx";
                    break;
                case "pdf":
                    data = productDocumentServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "product_documents.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export product documents: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    @Auditable(action = "IMPORT_PRODUCT_DOCUMENTS_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import product documents from CSV",
            description = "Imports product documents from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product documents imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import product documents from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import product documents: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = productDocumentServiceProcessor.importProductDocumentFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import product documents from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import product documents: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
