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
import projectlx.inventory.management.service.processor.api.ProductSubCategoryServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.ProductSubCategoryDto;
import projectlx.inventory.management.utils.requests.CreateProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.ProductSubCategoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.ProductSubCategoryResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/product-sub-category")
@Tag(name = "Product Sub Category System Resource", description = "Operations related to managing product sub categories (system)")
@RequiredArgsConstructor
public class ProductSubCategorySystemResource {

    private final ProductSubCategoryServiceProcessor productSubCategoryServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(ProductSubCategorySystemResource.class);

    @Auditable(action = "CREATE_PRODUCT_SUB_CATEGORY")
    @PostMapping("/create")
    @Operation(summary = "Create a new product sub category", description = "Creates a new product sub category and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product sub category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ProductSubCategoryResponse> create(@Valid @RequestBody final CreateProductSubCategoryRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(productSubCategoryServiceProcessor.create(request, locale, "SYSTEM"));
    }

    @Auditable(action = "UPDATE_PRODUCT_SUB_CATEGORY")
    @PutMapping("/update")
    @Operation(summary = "Update product sub category details", description = "Updates an existing product sub category's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product sub category updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product sub category not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ProductSubCategoryResponse> update(@Valid @RequestBody final EditProductSubCategoryRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return ResponseEntity.ok(productSubCategoryServiceProcessor.update(request, "SYSTEM", locale));
    }

    @Auditable(action = "FIND_PRODUCT_SUB_CATEGORY_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find product sub category by ID", description = "Retrieves a product sub category by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product sub category found successfully"),
            @ApiResponse(responseCode = "404", description = "Product sub category not found"),
            @ApiResponse(responseCode = "400", description = "Product sub category id supplied invalid")
    })
    public ResponseEntity<ProductSubCategoryResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                   final Locale locale) {
        return ResponseEntity.ok(productSubCategoryServiceProcessor.findById(id, locale, "SYSTEM"));
    }

    @Auditable(action = "DELETE_PRODUCT_SUB_CATEGORY")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a product sub category by ID")
    public ResponseEntity<ProductSubCategoryResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return ResponseEntity.ok(productSubCategoryServiceProcessor.delete(id, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_PRODUCT_SUB_CATEGORIES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all product sub categories", description = "Retrieves a list of all product sub categories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product sub categories retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product sub category(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching product sub categories")
    })
    public ResponseEntity<ProductSubCategoryResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                             final Locale locale) {
        return ResponseEntity.ok(productSubCategoryServiceProcessor.findAllAsList(locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_PRODUCT_SUB_CATEGORIES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find product sub categories by multiple filters",
            description = "Retrieves a list of product sub categories that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product sub category(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Product sub category(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<ProductSubCategoryResponse> findByMultipleFilters(@Valid @RequestBody ProductSubCategoryMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                               final Locale locale) {
        return ResponseEntity.ok(productSubCategoryServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale));
    }

    @Auditable(action = "EXPORT_PRODUCT_SUB_CATEGORIES")
    @PostMapping("/export")
    @Operation(summary = "Export product sub categories",
            description = "Exports product sub categories based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product sub categories exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody ProductSubCategoryMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export product sub categories in {} format with filters: {}", format, filters);
            ProductSubCategoryResponse response = productSubCategoryServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<ProductSubCategoryDto> list = InventoryExportSupport.itemsFromPage(response.getProductSubCategoryDtoPage());
            switch (format.toLowerCase()) {
                case "csv":
                    data = productSubCategoryServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "product_sub_categories.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = productSubCategoryServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "product_sub_categories.xlsx";
                    break;
                case "pdf":
                    data = productSubCategoryServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "product_sub_categories.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export product sub categories: " + e.getMessage();
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

    @Auditable(action = "IMPORT_PRODUCT_SUB_CATEGORIES_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import product sub categories from CSV",
            description = "Imports product sub categories from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product sub categories imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import product sub categories from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import product sub categories: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = productSubCategoryServiceProcessor.importProductSubCategoryFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import product sub categories from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import product sub categories: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
