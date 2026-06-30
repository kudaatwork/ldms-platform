package projectlx.co.zw.organizationmanagement.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.organizationmanagement.service.export.OrganizationExportHelper;
import projectlx.co.zw.organizationmanagement.service.processor.api.OrganizationServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.BranchMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateFleetVehicleRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.EditFleetVehicleRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationOperationalSettingsRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateBranchRequest;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.ImportSummary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/ldms-organization-management/v1/frontend/organization")
@Tag(name = "Organization (frontend)", description = "Organization self-service and relationships")
@RequiredArgsConstructor
public class OrganizationFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationFrontendResource.class);

    private final OrganizationServiceProcessor organizationServiceProcessor;
    private final OrganizationExportHelper exportHelper;

    @Auditable(action = "ORG_REGISTER")
    @PostMapping("/register")
    @Operation(summary = "Public signup — create organization (KYC DRAFT)")
    public OrganizationResponse register(
            @Valid @ModelAttribute RegisterOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String createdBy = request.getEmail() != null ? request.getEmail().trim() : "anonymous";
        return organizationServiceProcessor.register(request, locale, createdBy);
    }

    @Auditable(action = "ORG_ONBOARDING_STATUS")
    @GetMapping("/onboarding-status/{organizationId}")
    @Operation(summary = "Public onboarding tracker — live KYC status for signup applicants")
    public OrganizationResponse getOnboardingStatus(
            @PathVariable Long organizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.getOnboardingStatus(organizationId, locale);
    }

    @Auditable(action = "ORG_SUBMIT_KYC")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).SUBMIT_KYC.toString())")
    @PostMapping("/submit-kyc")
    public OrganizationResponse submitKyc(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.submitKyc(locale, username);
    }

    @Auditable(action = "ORG_GET_MY")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public OrganizationResponse getMy(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.getMy(locale, username);
    }

    @Auditable(action = "ORG_FIND_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Find organisations by filters (paginated)", description = "Organisation portal users only see their own organisation.")
    public OrganizationResponse findByMultipleFilters(
            @Valid @RequestBody OrganizationMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.findByMultipleFilters(request, username, locale);
    }

    @Auditable(action = "ORG_UPDATE_MY")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).UPDATE_MY_ORGAN.toString())")
    @PutMapping("/my/update")
    public OrganizationResponse updateMy(
            @RequestBody UpdateMyOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.updateMy(request, locale, username);
    }

    @Auditable(action = "ORG_ADD_BRANCH")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @PostMapping("/branch/add")
    public OrganizationResponse addBranch(
            @RequestBody AddBranchRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.addBranch(request, locale, username);
    }

    @Auditable(action = "ORG_LIST_BRANCHES")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @GetMapping("/branches")
    public OrganizationResponse listBranches(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listBranches(locale, username);
    }

    @Auditable(action = "ORG_GET_BRANCH")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @GetMapping("/branches/{branchId}")
    @Operation(summary = "Get a branch belonging to the signed-in organisation")
    public OrganizationResponse getBranch(
            @PathVariable Long branchId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.getBranchByIdForUser(branchId, locale, username);
    }

    @Auditable(action = "ORG_UPDATE_BRANCH")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @PutMapping("/branches/{branchId}")
    @Operation(summary = "Update a branch belonging to the signed-in organisation")
    public OrganizationResponse updateBranch(
            @PathVariable Long branchId,
            @RequestBody UpdateBranchRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.updateBranchForUser(branchId, request, locale, username);
    }

    @Auditable(action = "ORG_DELETE_BRANCH")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @DeleteMapping("/branches/{branchId}")
    @Operation(summary = "Delete a branch belonging to the signed-in organisation")
    public OrganizationResponse deleteBranch(
            @PathVariable Long branchId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.deleteBranchForUser(branchId, locale, username);
    }

    @Auditable(action = "ORG_FIND_BRANCHES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @PostMapping("/branches/find-by-multiple-filters")
    @Operation(summary = "Find branches for the signed-in organisation (paginated)")
    public OrganizationResponse findBranchesByMultipleFilters(
            @Valid @RequestBody BranchMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.findBranchesByMultipleFiltersForUser(request, locale, username);
    }

    @Auditable(action = "ORG_EXPORT_BRANCHES")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @PostMapping("/branches/export")
    @Operation(summary = "Export branches for the signed-in organisation (csv / xlsx / pdf)")
    public ResponseEntity<byte[]> exportBranches(
            @RequestBody BranchMultipleFiltersRequest filters,
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            List<BranchDto> items = organizationServiceProcessor.listBranchesForExportForUser(filters, locale, username);
            byte[] data = exportHelper.branchesToBytes(items, format);
            return exportResponse(data, format, "branches");
        } catch (Exception e) {
            logger.error("Export branches failed for user {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Export failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "ORG_IMPORT_BRANCHES_CSV")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @PostMapping("/branches/import-csv")
    @Operation(summary = "Import branches from CSV for the signed-in organisation")
    public ResponseEntity<ImportSummary> importBranchesFromCsv(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Empty file", 0, 0, 0, List.of("Empty file provided")));
        }
        try (InputStream is = file.getInputStream()) {
            ImportSummary summary = organizationServiceProcessor.importBranchesFromCsvForUser(is, locale, username);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            logger.error("CSV import failed for branches user={}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Import failed: " + e.getMessage(), 0, 0, 0,
                            List.of(e.getMessage())));
        }
    }

    @Auditable(action = "ORG_LIST_AGENTS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @GetMapping("/agents")
    @Operation(summary = "List agents for the signed-in organisation")
    public OrganizationResponse listAgents(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listAgents(locale, username);
    }

    @Auditable(action = "ORG_CREATE_AGENT")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @PostMapping("/agents")
    @Operation(summary = "Create an agent for the signed-in organisation")
    public OrganizationResponse createAgent(
            @RequestBody CreateAgentRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.createAgentForUser(request, locale, username);
    }

    @Auditable(action = "ORG_UPDATE_AGENT")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @PutMapping("/agents/{agentId}")
    @Operation(summary = "Update an agent belonging to the signed-in organisation")
    public OrganizationResponse updateAgent(
            @PathVariable Long agentId,
            @RequestBody UpdateAgentRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.updateAgentForUser(agentId, request, locale, username);
    }

    @Auditable(action = "ORG_DELETE_AGENT")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @DeleteMapping("/agents/{agentId}")
    @Operation(summary = "Delete an agent belonging to the signed-in organisation")
    public OrganizationResponse deleteAgent(
            @PathVariable Long agentId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.deleteAgentForUser(agentId, locale, username);
    }

    @Auditable(action = "ORG_IMPORT_AGENTS_CSV")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).MANAGE_BRANCHES.toString())")
    @PostMapping("/agents/import-csv")
    @Operation(summary = "Import agents from CSV for the signed-in organisation")
    public ResponseEntity<ImportSummary> importAgentsFromCsv(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Empty file", 0, 0, 0, List.of("Empty file provided")));
        }
        try (InputStream is = file.getInputStream()) {
            ImportSummary summary = organizationServiceProcessor.importAgentsFromCsvForUser(is, locale, username);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            logger.error("CSV import failed for agents user={}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Import failed: " + e.getMessage(), 0, 0, 0,
                            List.of(e.getMessage())));
        }
    }

    @Auditable(action = "ORG_LIST_CUSTOMERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).LIST_CUSTOMERS.toString())")
    @GetMapping("/customers")
    public OrganizationResponse listCustomers(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listCustomers(locale, username);
    }

    @Auditable(action = "ORG_LIST_SUPPLIERS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/suppliers")
    @Operation(summary = "List linked supplier organisations for the signed-in customer")
    public OrganizationResponse listSuppliers(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listSuppliers(locale, username);
    }

    @Auditable(action = "ORG_LIST_TRANSPORTERS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transporters")
    @Operation(summary = "List contracted transport companies for the signed-in organisation")
    public OrganizationResponse listTransporters(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listTransporters(locale, username);
    }

    @Auditable(action = "ORG_GET_TRANSPORTER")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transporters/{transporterId:\\d+}")
    @Operation(summary = "Get a linked transport company by id")
    public OrganizationResponse getTransporter(
            @PathVariable Long transporterId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.getTransporter(transporterId, locale, username);
    }

    @Auditable(action = "ORG_SEARCH_TRANSPORT_CANDIDATES")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).LINK_TRANSPORTER.toString())")
    @GetMapping("/transporters/candidates")
    @Operation(summary = "Search transport companies available to contract")
    public OrganizationResponse searchTransportCompanyCandidates(
            @RequestParam(required = false) String search,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.searchTransportCompanyCandidates(search, locale, username);
    }

    @Auditable(action = "ORG_LIST_PLATFORM_INDUSTRIES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/industries")
    @Operation(summary = "List platform industries (admin-configured, shared by all organisations)")
    public OrganizationResponse listPlatformIndustries(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.listActiveIndustriesForPlatform(locale);
    }

    @Auditable(action = "ORG_CHECK_CUSTOMER_REGISTRATION_EMAIL")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).REGISTER_CUSTOMER.toString())")
    @GetMapping("/customers/check-registration-email")
    @Operation(summary = "Check whether a customer registration email is available or can be linked (duplex offer)")
    public OrganizationResponse checkCustomerRegistrationEmail(
            @RequestParam String email,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.checkCustomerRegistrationEmail(email, locale, username);
    }

    @Auditable(action = "ORG_LINK_EXISTING_CUSTOMER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).REGISTER_CUSTOMER.toString())")
    @PostMapping("/customers/link-existing")
    @Operation(summary = "Link an existing organisation as a customer (enables duplex when target is a supplier)")
    public OrganizationResponse linkExistingOrganizationAsCustomer(
            @Valid @RequestBody projectlx.co.zw.organizationmanagement.utils.requests.LinkExistingOrganizationAsCustomerRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.linkExistingOrganizationAsCustomer(request, locale, username);
    }

    @Auditable(action = "ORG_REGISTER_CUSTOMER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).REGISTER_CUSTOMER.toString())")
    @PostMapping(value = "/customers/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register a customer organisation and link to the signed-in supplier")
    public OrganizationResponse registerCustomer(
            @Valid @ModelAttribute RegisterOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.registerCustomer(request, locale, username);
    }

    @Auditable(action = "ORG_REGISTER_TRANSPORTER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).LINK_TRANSPORTER.toString())")
    @PostMapping(value = "/transporters/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register a transport company and link to the signed-in supplier")
    public OrganizationResponse registerTransporter(
            @Valid @ModelAttribute RegisterOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.registerTransporter(request, locale, username);
    }

    @Auditable(action = "ORG_GET_CUSTOMER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).REGISTER_CUSTOMER.toString())")
    @GetMapping("/customers/{customerId:\\d+}")
    @Operation(summary = "Get a linked customer organisation for edit")
    public OrganizationResponse getCustomer(
            @PathVariable Long customerId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.getCustomer(customerId, locale, username);
    }

    @Auditable(action = "ORG_UPDATE_CUSTOMER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).REGISTER_CUSTOMER.toString())")
    @PutMapping(value = "/customers/{customerId:\\d+}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a linked customer organisation")
    public OrganizationResponse updateCustomer(
            @PathVariable Long customerId,
            @Valid @ModelAttribute RegisterOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.updateCustomer(customerId, request, locale, username);
    }

    @Auditable(action = "ORG_DELETE_CUSTOMER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).REGISTER_CUSTOMER.toString())")
    @DeleteMapping("/customers/{customerId:\\d+}")
    @Operation(summary = "Remove customer from supplier network (soft-delete when unlinked)")
    public OrganizationResponse deleteCustomer(
            @PathVariable Long customerId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.deleteCustomer(customerId, locale, username);
    }

    @Auditable(action = "ORG_RETRY_CUSTOMER_ONBOARDING")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).REGISTER_CUSTOMER.toString())")
    @PostMapping("/customers/{customerId:\\d+}/retry-onboarding")
    @Operation(summary = "Resend supplier-registered onboarding emails for a linked customer")
    public OrganizationResponse retryCustomerOnboarding(
            @PathVariable Long customerId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.retryCustomerOnboarding(customerId, locale, username);
    }

    @Auditable(action = "ORG_LINK_TRANSPORTER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).LINK_TRANSPORTER.toString())")
    @PostMapping("/transporters/link")
    @Operation(summary = "Send a contract offer to an existing transporter (awaits the transporter's acceptance)")
    public OrganizationResponse linkTransporter(
            @RequestBody LinkTransporterRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.linkTransporter(request, locale, username);
    }

    @Auditable(action = "ORG_LIST_TRANSPORTER_OFFERS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transporters/offers/incoming")
    @Operation(summary = "List pending contract offers awaiting this transporter's response")
    public OrganizationResponse listIncomingTransporterOffers(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listIncomingTransporterOffers(locale, username);
    }

    @Auditable(action = "ORG_ACCEPT_TRANSPORTER_OFFER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/transporters/offers/{supplierOrganizationId:\\d+}/accept")
    @Operation(summary = "Accept a supplier's transporter contract offer")
    public OrganizationResponse acceptTransporterOffer(
            @PathVariable Long supplierOrganizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.respondToTransporterOffer(supplierOrganizationId, true, locale, username);
    }

    @Auditable(action = "ORG_DECLINE_TRANSPORTER_OFFER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/transporters/offers/{supplierOrganizationId:\\d+}/decline")
    @Operation(summary = "Decline a supplier's transporter contract offer")
    public OrganizationResponse declineTransporterOffer(
            @PathVariable Long supplierOrganizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.respondToTransporterOffer(supplierOrganizationId, false, locale, username);
    }

    @Auditable(action = "ORG_CANCEL_TRANSPORTER_OFFER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).LINK_TRANSPORTER.toString())")
    @PostMapping("/transporters/offers/{transporterOrganizationId:\\d+}/cancel")
    @Operation(summary = "Cancel a pending transporter contract offer the supplier sent")
    public OrganizationResponse cancelTransporterOffer(
            @PathVariable Long transporterOrganizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.cancelTransporterOffer(transporterOrganizationId, locale, username);
    }

    @Auditable(action = "ORG_GET_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get organisation by id", description = "Includes branches. Caller may only access their linked organisation.")
    public OrganizationResponse getById(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.getByIdForFrontend(id, locale, username);
    }

    // =========================================================
    // Transporter edit / delete
    // =========================================================

    @Auditable(action = "ORG_UPDATE_TRANSPORTER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).LINK_TRANSPORTER.toString())")
    @PutMapping(value = "/transporters/{transporterId:\\d+}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a contracted transporter organisation")
    public OrganizationResponse updateTransporter(
            @PathVariable Long transporterId,
            @Valid @ModelAttribute RegisterOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.updateTransporter(transporterId, request, locale, username);
    }

    @Auditable(action = "ORG_LIST_TRANSPORTER_FLEET_VEHICLES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transporters/{transporterId:\\d+}/fleet-vehicles")
    @Operation(summary = "List fleet vehicles for a linked transport partner")
    public OrganizationResponse listTransporterFleetVehicles(
            @PathVariable Long transporterId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listTransporterFleetVehicles(transporterId, locale, username);
    }

    @Auditable(action = "ORG_DELETE_TRANSPORTER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).LINK_TRANSPORTER.toString())")
    @DeleteMapping("/transporters/{transporterId:\\d+}")
    @Operation(summary = "Remove transporter contract link (soft-delete when unlinked)")
    public OrganizationResponse deleteTransporter(
            @PathVariable Long transporterId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.deleteTransporter(transporterId, locale, username);
    }

    // =========================================================
    // Fleet vehicle management
    // =========================================================

    @Auditable(action = "ORG_LIST_FLEET_VEHICLES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/fleet-vehicles")
    @Operation(summary = "List own fleet vehicles for the signed-in organisation")
    public OrganizationResponse listFleetVehicles(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listFleetVehicles(locale, username);
    }

    @Auditable(action = "ORG_CREATE_FLEET_VEHICLE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/fleet-vehicles")
    @Operation(summary = "Add a fleet vehicle to the signed-in organisation")
    public OrganizationResponse createFleetVehicle(
            @RequestBody CreateFleetVehicleRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.createFleetVehicle(request, locale, username);
    }

    @Auditable(action = "ORG_UPDATE_FLEET_VEHICLE")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/fleet-vehicles/{id:\\d+}")
    @Operation(summary = "Update a fleet vehicle belonging to the signed-in organisation")
    public OrganizationResponse updateFleetVehicle(
            @PathVariable Long id,
            @RequestBody EditFleetVehicleRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.updateFleetVehicle(id, request, locale, username);
    }

    @Auditable(action = "ORG_DELETE_FLEET_VEHICLE")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/fleet-vehicles/{id:\\d+}")
    @Operation(summary = "Soft-delete a fleet vehicle from the signed-in organisation")
    public OrganizationResponse deleteFleetVehicle(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.deleteFleetVehicle(id, locale, username);
    }

    // =========================================================
    // Operational mode settings
    // =========================================================

    @Auditable(action = "ORG_UPDATE_OPERATIONAL_SETTINGS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).UPDATE_MY_ORGAN.toString())")
    @PutMapping("/operational-settings")
    @Operation(summary = "Update operational mode settings for the signed-in organisation",
            description = "Controls standalone mode, inventory management, cross-docking, and inventory data source. " +
                    "Transition rules are enforced server-side.")
    public OrganizationResponse updateOperationalSettings(
            @RequestBody UpdateOrganizationOperationalSettingsRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.updateOperationalSettings(request, locale, username);
    }

    private ResponseEntity<byte[]> exportResponse(byte[] data, String format, String entityName) {
        String fmt = format.trim().toLowerCase();
        String contentType;
        String extension;
        if ("pdf".equals(fmt)) {
            contentType = "application/pdf";
            extension = "pdf";
        } else if ("xlsx".equals(fmt) || "excel".equals(fmt)) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = "xlsx";
        } else {
            contentType = "text/csv;charset=UTF-8";
            extension = "csv";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + entityName + "." + extension)
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
