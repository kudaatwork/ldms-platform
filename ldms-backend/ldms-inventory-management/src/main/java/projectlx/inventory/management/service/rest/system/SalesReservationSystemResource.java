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
import projectlx.inventory.management.service.processor.api.SalesReservationServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.SalesReservationDto;
import projectlx.inventory.management.utils.requests.CreateSalesReservationRequest;
import projectlx.inventory.management.utils.requests.EditSalesReservationRequest;
import projectlx.inventory.management.utils.requests.SalesReservationMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.SalesReservationResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/sales-reservation")
@Tag(name = "Sales Reservation System Resource", description = "Operations related to managing sales reservations (system)")
@RequiredArgsConstructor
public class SalesReservationSystemResource {

    private final SalesReservationServiceProcessor salesReservationServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(SalesReservationSystemResource.class);

    @Auditable(action = "CREATE_SALES_RESERVATION")
    @PostMapping("/create")
    @Operation(summary = "Create a new sales reservation", description = "Creates a new sales reservation and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sales reservation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public SalesReservationResponse create(@Valid @RequestBody final CreateSalesReservationRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return salesReservationServiceProcessor.create(request, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_SALES_RESERVATION")
    @PutMapping("/update")
    @Operation(summary = "Update sales reservation details", description = "Updates an existing sales reservation's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sales reservation updated successfully"),
            @ApiResponse(responseCode = "404", description = "Sales reservation not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public SalesReservationResponse update(@Valid @RequestBody final EditSalesReservationRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return salesReservationServiceProcessor.update(request, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_SALES_RESERVATION_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find sales reservation by ID", description = "Retrieves a sales reservation by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales reservation found successfully"),
            @ApiResponse(responseCode = "404", description = "Sales reservation not found"),
            @ApiResponse(responseCode = "400", description = "Sales reservation id supplied invalid")
    })
    public SalesReservationResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                   final Locale locale) {
        return salesReservationServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_SALES_RESERVATION")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a sales reservation by ID")
    public SalesReservationResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return salesReservationServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_SALES_RESERVATIONS_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all sales reservations", description = "Retrieves a list of all sales reservations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales reservations retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Sales reservation(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching sales reservations")
    })
    public SalesReservationResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                             final Locale locale) {
        return salesReservationServiceProcessor.findAllAsList(locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_SALES_RESERVATIONS_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find sales reservations by multiple filters",
            description = "Retrieves a list of sales reservations that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales reservation(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Sales reservation(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public SalesReservationResponse findByMultipleFilters(@Valid @RequestBody SalesReservationMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                               final Locale locale) {
        return salesReservationServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
    }

    @Auditable(action = "EXPORT_SALES_RESERVATIONS")
    @PostMapping("/export")
    @Operation(summary = "Export sales reservations",
            description = "Exports sales reservations based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales reservations exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody SalesReservationMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export sales reservations in {} format with filters: {}", format, filters);
            SalesReservationResponse response = salesReservationServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<SalesReservationDto> list = InventoryExportSupport.resolveExportItems(
                    response.getSalesReservationDtoPage(), response.getSalesReservationDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = salesReservationServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "sales_reservations.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = salesReservationServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "sales_reservations.xlsx";
                    break;
                case "pdf":
                    data = salesReservationServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "sales_reservations.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export sales reservations: " + e.getMessage();
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

    @Auditable(action = "IMPORT_SALES_RESERVATIONS_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import sales reservations from CSV",
            description = "Imports sales reservations from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales reservations imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import sales reservations from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import sales reservations: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = salesReservationServiceProcessor.importSalesReservationFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import sales reservations from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import sales reservations: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
