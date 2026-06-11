package projectlx.inventory.management.service.rest.frontend;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.service.processor.api.ProductCategoryServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.ProductCategoryDto;
import projectlx.inventory.management.utils.requests.CreateProductCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductCategoryRequest;
import projectlx.inventory.management.utils.requests.ProductCategoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.ProductCategoryResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/product-category")
@Tag(name = "Product Category Frontend Resource", description = "Operations related to managing product categories")
@RequiredArgsConstructor
public class ProductCategoryFrontendResource {

    private final ProductCategoryServiceProcessor productCategoryServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(ProductCategoryFrontendResource.class);

    @Auditable(action = "CREATE_PRODUCT_CATEGORY")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a new product category", description = "Creates a new product category and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ProductCategoryResponse> create(@Valid @RequestBody final CreateProductCategoryRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productCategoryServiceProcessor.create(request, locale, username));
    }

    @Auditable(action = "UPDATE_PRODUCT_CATEGORY")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update product category details", description = "Updates an existing product category's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product category updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product category not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ProductCategoryResponse> update(@Valid @RequestBody final EditProductCategoryRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productCategoryServiceProcessor.update(request, username, locale));
    }

    @Auditable(action = "FIND_PRODUCT_CATEGORY_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find product category by ID", description = "Retrieves a product category by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product category found successfully"),
            @ApiResponse(responseCode = "404", description = "Product category not found"),
            @ApiResponse(responseCode = "400", description = "Product category id supplied invalid")
    })
    public ResponseEntity<ProductCategoryResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productCategoryServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_PRODUCT_CATEGORY")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a product category by ID")
    public ResponseEntity<ProductCategoryResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productCategoryServiceProcessor.delete(id, locale, username));
    }

    @Auditable(action = "FIND_ALL_PRODUCT_CATEGORIES_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all product categories", description = "Retrieves a list of all product categories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product categories retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product category(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching product categories")
    })
    public ResponseEntity<ProductCategoryResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                             final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productCategoryServiceProcessor.findAllAsList(locale, username));
    }

    @Auditable(action = "FIND_ALL_PRODUCT_CATEGORIES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find product categories by multiple filters",
            description = "Retrieves a list of product categories that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product category(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Product category(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<ProductCategoryResponse> findByMultipleFilters(@Valid @RequestBody ProductCategoryMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                               final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productCategoryServiceProcessor.findByMultipleFilters(filters, username, locale));
    }

    @Auditable(action = "EXPORT_PRODUCT_CATEGORIES")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export product categories",
            description = "Exports product categories based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product categories exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody ProductCategoryMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export product categories in {} format with filters: {}", format, filters);
            ProductCategoryResponse response = productCategoryServiceProcessor.findByMultipleFilters(filters, SecurityContextHolder.getContext().getAuthentication().getName(), locale);
            List<ProductCategoryDto> list = InventoryExportSupport.itemsFromPage(response.getProductCategoryDtoPage());
            switch (format.toLowerCase()) {
                case "csv":
                    data = productCategoryServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "product_categories.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = productCategoryServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "product_categories.xlsx";
                    break;
                case "pdf":
                    data = productCategoryServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "product_categories.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export product categories: " + e.getMessage();
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

    @Auditable(action = "IMPORT_PRODUCT_CATEGORIES_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import product categories from CSV",
            description = "Imports product categories from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product categories imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import product categories from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import product categories: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = productCategoryServiceProcessor.importProductCategoryFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import product categories from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import product categories: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
