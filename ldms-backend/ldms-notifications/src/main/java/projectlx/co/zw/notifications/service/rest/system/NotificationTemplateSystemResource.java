package projectlx.co.zw.notifications.service.rest.system;

import com.lowagie.text.DocumentException;
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
import projectlx.co.zw.notifications.service.processor.api.NotificationTemplateProcessor;
import projectlx.co.zw.notifications.utils.dtos.ImportSummary;
import projectlx.co.zw.notifications.utils.requests.CreateTemplateRequest;
import projectlx.co.zw.notifications.utils.requests.TemplateMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.requests.UpdateTemplateRequest;
import projectlx.co.zw.notifications.utils.responses.TemplateResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/system/notification-template")
@Tag(name = "Notification Template System Resource", description = "System operations related to managing notification templates")
@RequiredArgsConstructor
public class NotificationTemplateSystemResource {

    private final NotificationTemplateProcessor notificationTemplateProcessor;
    private static final Logger logger = LoggerFactory.getLogger(NotificationTemplateSystemResource.class);

    @Auditable(action = "GET_ADD_TEMPLATE_METADATA")
    @GetMapping("/add-template-metadata")
    @Operation(summary = "Get Add Template form metadata", description = "Returns sections and channel options for rendering the Add Template form in a stepped, intuitive way (like Add Organization).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadata returned successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public TemplateResponse getAddTemplateMetadata(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return notificationTemplateProcessor.getAddTemplateMetadata(locale, "SYSTEM");
    }

    @Auditable(action = "CREATE_TEMPLATE")
    @PostMapping("/create")
    @Operation(summary = "Create a new notification template", description = "Creates a new notification template and returns the created template details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification template created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public TemplateResponse create(@Valid @RequestBody final CreateTemplateRequest createTemplateRequest,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return notificationTemplateProcessor.create(createTemplateRequest, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_TEMPLATE")
    @PutMapping("/update")
    @Operation(summary = "Update notification template details", description = "Updates an existing notification template's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification template updated successfully"),
            @ApiResponse(responseCode = "404", description = "Notification template not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public TemplateResponse update(@Valid @RequestBody final UpdateTemplateRequest updateTemplateRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return notificationTemplateProcessor.update(updateTemplateRequest, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_TEMPLATE_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find notification template by ID", description = "Retrieves a notification template by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification template found successfully"),
            @ApiResponse(responseCode = "404", description = "Notification template not found"),
            @ApiResponse(responseCode = "400", description = "Notification template id supplied invalid")
    })
    public TemplateResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return notificationTemplateProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_TEMPLATE")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a notification template by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification template deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Notification template not found"),
            @ApiResponse(responseCode = "400", description = "Notification template id supplied invalid")
    })
    public TemplateResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return notificationTemplateProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_TEMPLATES_AS_A_LIST")
    @GetMapping(value = "/find-all-as-a-list")
    @Operation(summary = "Find all notification templates as a list", description = "Retrieves all notification templates as a list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification templates found successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public TemplateResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return notificationTemplateProcessor.findAllAsList(locale, "SYSTEM");
    }

    @Auditable(action = "FIND_TEMPLATES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find notification templates by multiple filters", description = "Retrieves notification templates based on multiple filter criteria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification templates found successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public TemplateResponse findByMultipleFilters(@Valid @RequestBody final TemplateMultipleFiltersRequest templateMultipleFiltersRequest,
                                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return notificationTemplateProcessor.findByMultipleFilters(templateMultipleFiltersRequest, "SYSTEM", locale);
    }

    @Auditable(action = "EXPORT_TEMPLATES")
    @PostMapping(value = "/export")
    @Operation(summary = "Export notification templates", description = "Exports notification templates based on filter criteria in the specified format.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification templates exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid format specified"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<byte[]> exportTemplates(@Valid @RequestBody final TemplateMultipleFiltersRequest filters,
                                               @RequestParam("format") final String format,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = "notification_templates";

        try {
            byte[] data;
            switch (format.toLowerCase()) {
                case "csv":
                    data = notificationTemplateProcessor.exportToCsv(filters, "SYSTEM", locale);
                    headers.setContentDispositionFormData("attachment", filename + ".csv");
                    break;
                case "excel":
                    data = notificationTemplateProcessor.exportToExcel(filters, "SYSTEM", locale);
                    headers.setContentDispositionFormData("attachment", filename + ".xlsx");
                    break;
                case "pdf":
                    data = notificationTemplateProcessor.exportToPdf(filters, "SYSTEM", locale);
                    headers.setContentDispositionFormData("attachment", filename + ".pdf");
                    break;
                default:
                    return ResponseEntity.badRequest().body(("Invalid format: " + format).getBytes(StandardCharsets.UTF_8));
            }
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IOException | DocumentException e) {
            logger.error("Error exporting notification templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error exporting notification templates: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "IMPORT_TEMPLATES")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import notification templates from CSV", description = "Imports notification templates from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification templates imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file format or data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ImportSummary> importTemplatesFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            InputStream csvInputStream = file.getInputStream();
            ImportSummary importSummary = notificationTemplateProcessor.importTemplatesFromCsv(csvInputStream);
            return ResponseEntity.ok(importSummary);
        } catch (IOException e) {
            logger.error("Error importing notification templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}