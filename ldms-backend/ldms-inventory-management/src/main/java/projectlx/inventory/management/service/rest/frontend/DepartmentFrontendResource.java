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
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.service.processor.api.DepartmentServiceProcessor;
import projectlx.inventory.management.utils.dtos.DepartmentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateDepartmentRequest;
import projectlx.inventory.management.utils.requests.DepartmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.EditDepartmentRequest;
import projectlx.inventory.management.utils.responses.DepartmentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/department")
@Tag(name = "Department Frontend Resource", description = "Organisation departments for purchase requisitions")
@RequiredArgsConstructor
public class DepartmentFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentFrontendResource.class);
    private final DepartmentServiceProcessor departmentServiceProcessor;

    @Auditable(action = "CREATE_DEPARTMENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create department")
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody final CreateDepartmentRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(departmentServiceProcessor.create(request, locale, username));
    }

    @Auditable(action = "UPDATE_DEPARTMENT")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update department")
    public ResponseEntity<DepartmentResponse> update(
            @Valid @RequestBody final EditDepartmentRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(departmentServiceProcessor.update(request, username, locale));
    }

    @Auditable(action = "FIND_DEPARTMENT_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find department by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department found"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<DepartmentResponse> findById(
            @PathVariable("id") final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(departmentServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_DEPARTMENT")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete-by-id/{id}")
    @Operation(summary = "Delete department")
    public ResponseEntity<DepartmentResponse> delete(
            @PathVariable("id") final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(departmentServiceProcessor.delete(id, locale, username));
    }

    @Auditable(action = "FIND_ALL_DEPARTMENTS_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-list")
    @Operation(summary = "List departments for signed-in organisation")
    public ResponseEntity<DepartmentResponse> findAllAsList(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(departmentServiceProcessor.findAllAsList(locale, username));
    }

    @Auditable(action = "FIND_ALL_DEPARTMENTS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Find departments by multiple filters")
    public ResponseEntity<DepartmentResponse> findByMultipleFilters(
            @Valid @RequestBody DepartmentMultipleFiltersRequest filters,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(departmentServiceProcessor.findByMultipleFilters(filters, username, locale));
    }

    @Auditable(action = "EXPORT_DEPARTMENTS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export departments", description = "Exports departments in csv, excel, or pdf format.")
    public ResponseEntity<byte[]> export(
            @RequestBody DepartmentMultipleFiltersRequest filters,
            @RequestParam String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Exporting departments in {} format with filters: {}", format, filters);
            DepartmentResponse response = departmentServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<DepartmentDto> list = InventoryExportSupport.itemsFromPage(response.getDepartmentDtoPage());
            if (list.isEmpty() && response.getDepartmentDtoList() != null) {
                list = response.getDepartmentDtoList();
            }
            switch (format.toLowerCase()) {
                case "csv":
                    data = departmentServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "departments.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = departmentServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "departments.xlsx";
                    break;
                case "pdf":
                    data = departmentServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "departments.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export departments: " + e.getMessage();
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

    @Auditable(action = "IMPORT_DEPARTMENTS_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import departments from CSV")
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Importing departments from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import departments: Empty file", 0, 0, 0,
                                List.of("Empty file provided"))
                );
            }
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = departmentServiceProcessor.importDepartmentFromCsv(
                        inputStream, username, Locale.ENGLISH);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import departments from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import departments: " + e.getMessage(), 0, 0, 0,
                            List.of(e.getMessage()))
            );
        }
    }
}
