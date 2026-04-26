package projectlx.co.zw.organizationmanagement.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.organizationmanagement.service.processor.api.OrganizationServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.Locale;

@CrossOrigin
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
            @Valid @RequestBody RegisterOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String createdBy = request.getEmail() != null ? request.getEmail().trim() : "anonymous";
        return organizationServiceProcessor.register(request, locale, createdBy);
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
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).VIEW_MY_ORG.toString())")
    @GetMapping("/my")
    public OrganizationResponse getMy(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.getMy(locale, username);
    }

    @Auditable(action = "ORG_UPDATE_MY")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).UPDATE_MY_ORG.toString())")
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

    @Auditable(action = "ORG_REGISTER_CUSTOMER")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).REGISTER_CUSTOMER.toString())")
    @PostMapping("/customers/register")
    public OrganizationResponse registerCustomer(
            @RequestBody RegisterCustomerOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return organizationServiceProcessor.registerCustomer(request, locale, username);
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
}
