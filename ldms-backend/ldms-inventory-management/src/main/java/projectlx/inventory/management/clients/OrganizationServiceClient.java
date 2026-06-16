package projectlx.inventory.management.clients;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.enums.VerificationMethod;
import projectlx.co.zw.shared_library.utils.enums.VerificationSource;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.List;
import java.util.Locale;

public interface OrganizationServiceClient {

    @PostMapping("/ldms-organization-management/v1/system/organization/register")
    OrganizationResponse create(@Valid @ModelAttribute final Object createOrganizationRequest,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PutMapping("/ldms-organization-management/v1/system/organization/{id}")
    OrganizationResponse update(@Valid @RequestBody final Object editOrganizationRequest,
                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/{id}")
    OrganizationResponse findById(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                         defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @DeleteMapping("/delete-by-id/{id}")
    OrganizationResponse delete(@PathVariable("id") final Long id,
                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/find-all-as-a-list")
    OrganizationResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                               defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/find-by-multiple-filters")
    OrganizationResponse findByMultipleFilters(@Valid @RequestBody final Object organizationMultipleFiltersRequest,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/find-by-supplier-id-and-customer-id-and-organization-classification")
    OrganizationResponse findBySupplierIdAndCustomerIdAndOrganizationClassification(@Valid @RequestBody final Object findCustomerByIdRequest,
                                                                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/import-csv")
    ResponseEntity<Object> importOrganizationsFromCsv(@RequestPart("file") MultipartFile file);

    @PostMapping("/ldms-organization-management/v1/system/organization/approve-organization")
    Object approveOrganization(@RequestParam("organizationId") final Long organizationId,
                                            @RequestParam("approvingUserId") final Long approvingUserId,
                                            @RequestParam("method") final VerificationMethod method,
                                            @RequestParam("source") final VerificationSource source,
                                            @RequestParam("notes") final String notes,
                                            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/approve-organization-simple")
    Object approveOrganizationSimple(@RequestParam("organizationId") final Long organizationId,
                                                  @RequestParam("approvingUserId") final Long approvingUserId,
                                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                          defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/verify-organization")
    Object verifyOrganization(@RequestParam("token") final String token,
                                           @RequestParam("organizationId") final Long organizationId,
                                           @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                           @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                   defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/resend-verification-link/{organizationId}")
    Object resendVerificationLink(@PathVariable("organizationId") final Long organizationId,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/is-organization-approved/{organizationId}")
    Object isOrganizationApproved(@PathVariable("organizationId") final Long organizationId,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/is-organization-fully-verified/{organizationId}")
    Object isOrganizationFullyVerified(@PathVariable("organizationId") final Long organizationId,
                                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/get-organization-verification-status/{organizationId}")
    Object getOrganizationVerificationStatus(@PathVariable("organizationId") final Long organizationId,
                                                          @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                          @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                  defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/find-organizations-by-classification/{classification}")
    Object findOrganizationsByClassification(@PathVariable("classification") final String classification,
                                                          @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                          @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                  defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/find-all-suppliers")
    Object findAllSuppliers(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                 defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/find-all-customers")
    Object findAllCustomers(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                 defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/find-all-transport-companies")
    Object findAllTransportCompanies(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                          defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/find-approved-organizations-by-classification/{classification}")
    Object findApprovedOrganizationsByClassification(@PathVariable("classification") final String classification,
                                                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                          defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/find-approved-suppliers")
    Object findApprovedSuppliers(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/find-customers-by-supplier/{supplierId}")
    Object findCustomersBySupplier(@PathVariable("supplierId") final Long supplierId,
                                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                        defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/find-suppliers-by-customer/{customerId}")
    Object findSuppliersByCustomer(@PathVariable("customerId") final Long customerId,
                                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                        defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/assign-customer-to-supplier")
    Object assignCustomerToSupplier(@RequestParam("supplierId") final Long supplierId,
                                                 @RequestParam("customerId") final Long customerId,
                                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                         defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @DeleteMapping("/ldms-organization-management/v1/system/organization/remove-customer-from-supplier")
    Object removeCustomerFromSupplier(@RequestParam("supplierId") final Long supplierId,
                                                   @RequestParam("customerId") final Long customerId,
                                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/assign-multiple-customers-to-supplier")
    Object assignMultipleCustomersToSupplier(@RequestParam("supplierId") final Long supplierId,
                                                          @RequestParam("customerIds") final List<Long> customerIds,
                                                          @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                          @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                  defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/count-organizations-by-classification/{classification}")
    Object countOrganizationsByClassification(@PathVariable("classification") final String classification,
                                                           @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                           @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                   defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/branches/{branchId}")
    OrganizationResponse getBranchById(
            @PathVariable("branchId") Long branchId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @GetMapping("/ldms-organization-management/v1/system/organization/{organizationId}/head-office-branch")
    OrganizationResponse getHeadOfficeBranch(
            @PathVariable("organizationId") Long organizationId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
