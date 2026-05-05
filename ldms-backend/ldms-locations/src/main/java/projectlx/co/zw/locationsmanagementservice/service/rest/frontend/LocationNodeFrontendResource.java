package projectlx.co.zw.locationsmanagementservice.service.rest.frontend;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocationNodeServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocationNodeDto;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;
import projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/ldms-locations/v1/frontend/location-node")
@RequiredArgsConstructor
public class LocationNodeFrontendResource {
    private final LocationNodeServiceProcessor locationNodeServiceProcessor;

    @Auditable(action = "CREATE_LOCATION_NODE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "CREATE_LOCATION_NODE.toString())")
    @PostMapping("/create")
    public LocationNodeResponse create(@Valid @RequestBody CreateLocationNodeRequest request,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.create(request, locale, username);
    }

    @Auditable(action = "UPDATE_LOCATION_NODE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "UPDATE_LOCATION_NODE.toString())")
    @PutMapping("/update")
    public LocationNodeResponse update(@Valid @RequestBody EditLocationNodeRequest request,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.update(request, locale, username);
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "VIEW_LOCATION_NODE_BY_ID.toString())")
    @GetMapping("/find-by-id/{id}")
    public LocationNodeResponse findById(@PathVariable("id") Long id,
                                         @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_LOCATION_NODES_BY_LIST")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "VIEW_LOCATION_NODE_BY_FILTERS.toString())")
    @GetMapping("/find-by-list")
    public LocationNodeResponse findAllAsAList(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.findAllAsList(locale, username);
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_PARENT")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "VIEW_LOCATION_NODE_BY_PARENT.toString())")
    @GetMapping("/find-by-parent-id/{parentId}")
    public LocationNodeResponse findByParentId(@PathVariable("parentId") Long parentId,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.findByParentId(parentId, locale, username);
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "VIEW_LOCATION_NODE_BY_FILTERS.toString())")
    @PostMapping("/find-by-multiple-filters")
    public LocationNodeResponse findByMultipleFilters(@RequestBody LocationNodeMultipleFiltersRequest request,
                                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.findByMultipleFilters(request, locale, username);
    }

    @Auditable(action = "DELETE_LOCATION_NODE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "DELETE_LOCATION_NODE.toString())")
    @DeleteMapping("/delete-by-id/{id}")
    public LocationNodeResponse delete(@PathVariable("id") Long id,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "EXPORT_LOCATION_NODE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "EXPORT_LOCATION_NODE.toString())")
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportLocationNodes(@RequestBody LocationNodeMultipleFiltersRequest filters,
                                                      @RequestParam String format,
                                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            filters.setPage(0);
            filters.setSize(Integer.MAX_VALUE);
            LocationNodeResponse response = locationNodeServiceProcessor.findByMultipleFilters(filters, locale, username);
            List<LocationNodeDto> nodes = response.getLocationNodeDtoPage() != null
                    ? response.getLocationNodeDtoPage().getContent()
                    : new ArrayList<>();
            switch (format.toLowerCase()) {
                case "csv":
                    data = locationNodeServiceProcessor.exportToCsv(nodes);
                    contentType = "text/csv";
                    filename = "location-nodes.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = locationNodeServiceProcessor.exportToExcel(nodes);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "location-nodes.xlsx";
                    break;
                case "pdf":
                    data = locationNodeServiceProcessor.exportToPdf(nodes);
                    contentType = "application/pdf";
                    filename = "location-nodes.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export location nodes: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    @Auditable(action = "IMPORT_LOCATION_NODE_FROM_CSV")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "IMPORT_LOCATION_NODE.toString())")
    @PostMapping("/import-csv")
    public ResponseEntity<ImportSummary> importLocationNodesFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import location nodes: Empty file",
                                0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = locationNodeServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import location nodes: " + e.getMessage(),
                            0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
