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
import projectlx.inventory.management.service.processor.api.ProductServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.ProductDto;
import projectlx.inventory.management.utils.requests.CreateProductRequest;
import projectlx.inventory.management.utils.requests.EditProductRequest;
import projectlx.inventory.management.utils.requests.ProductMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.ProductResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/product")
@Tag(name = "Product Frontend Resource", description = "Operations related to managing products")
@RequiredArgsConstructor
public class ProductFrontendResource {

    private final ProductServiceProcessor productServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(ProductFrontendResource.class);

    @Auditable(action = "CREATE_PRODUCT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a new product", description = "Creates a new product and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody final CreateProductRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productServiceProcessor.create(request, locale, username));
    }

    @Auditable(action = "UPDATE_PRODUCT")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update product details", description = "Updates an existing product's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ProductResponse> update(@Valid @RequestBody final EditProductRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productServiceProcessor.update(request, username, locale));
    }

    @Auditable(action = "FIND_PRODUCT_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find product by ID", description = "Retrieves a product by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "400", description = "Product id supplied invalid")
    })
    public ResponseEntity<ProductResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_PRODUCT")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a product by ID")
    public ResponseEntity<ProductResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productServiceProcessor.delete(id, locale, username));
    }

    @Auditable(action = "FIND_ALL_PRODUCTS_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all products", description = "Retrieves a list of all products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching products")
    })
    public ResponseEntity<ProductResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                             final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productServiceProcessor.findAllAsList(locale, username));
    }

    @Auditable(action = "FIND_ALL_PRODUCTS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find products by multiple filters",
            description = "Retrieves a list of products that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Product(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<ProductResponse> findByMultipleFilters(@Valid @RequestBody ProductMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                               final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productServiceProcessor.findByMultipleFilters(filters, username, locale));
    }

    @Auditable(action = "EXPORT_PRODUCTS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export products",
            description = "Exports products based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody ProductMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export products in {} format with filters: {}", format, filters);
            ProductResponse response = productServiceProcessor.findByMultipleFilters(filters, SecurityContextHolder.getContext().getAuthentication().getName(), locale);
            List<ProductDto> list = InventoryExportSupport.itemsFromPage(response.getProductDtoPage());
            switch (format.toLowerCase()) {
                case "csv":
                    data = productServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "products.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = productServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "products.xlsx";
                    break;
                case "pdf":
                    data = productServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "products.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export products: " + e.getMessage();
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

    @Auditable(action = "IMPORT_PRODUCTS_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import products from CSV",
            description = "Imports products from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import products from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import products: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = productServiceProcessor.importProductFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import products from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import products: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
