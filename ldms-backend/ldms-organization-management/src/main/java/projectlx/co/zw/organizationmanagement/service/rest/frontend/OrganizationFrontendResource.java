package projectlx.co.zw.organizationmanagement.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import projectlx.co.zw.organizationmanagement.service.processor.api.OrganizationServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateFleetVehicleRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.EditFleetVehicleRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.Locale;

@RestController
@RequestMapping("/ldms-organization-management/v1/frontend/organization")
@Tag(name = "Organization (frontend)", description = "Organization self-service and relationships")
@RequiredArgsConstructor
public class OrganizationFrontendResource {

    private final OrganizationServiceProcessor organizationServiceProcessor;

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

    @Auditable(action = "ORG_LIST_CUSTOMERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).LIST_CUSTOMERS.toString())")
    @GetMapping("/customers")
    public OrganizationResponse listCustomers(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.listCustomers(locale, username);
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
    public OrganizationResponse linkTransporter(
            @RequestBody LinkTransporterRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.linkTransporter(request, locale, username);
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
}
