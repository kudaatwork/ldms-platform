package projectlx.co.zw.organizationmanagement.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.organizationmanagement.service.export.OrganizationExportHelper;
import projectlx.co.zw.organizationmanagement.service.processor.api.OrganizationServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.dtos.ImportSummary;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;
import projectlx.co.zw.organizationmanagement.utils.requests.AgentMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.BranchMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateIndustryRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.IndustryMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycActionRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycRejectRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateIndustryRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationRequest;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/ldms-organization-management/v1/system/organization")
@Tag(name = "Organization (system)", description = "Internal KYC queue and actions (no JWT)")
@RequiredArgsConstructor
public class OrganizationSystemResource {

    private static final String SYSTEM = "SYSTEM";
    private static final Logger logger = LoggerFactory.getLogger(OrganizationSystemResource.class);

    private final OrganizationServiceProcessor organizationServiceProcessor;
    private final OrganizationExportHelper exportHelper;

    // =========================================================
    // Organization
    // =========================================================

    @Auditable(action = "ORG_SYSTEM_VERIFY_EMAIL")
    @PostMapping("/verify-email")
    @Operation(summary = "Verify organisation email (supplier-registered customers/transporters)")
    public OrganizationResponse verifyOrganizationEmail(
            @RequestParam String token,
            @RequestParam String email,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.verifyOrganizationEmail(email, token, locale);
    }

    @Auditable(action = "ORG_SYSTEM_ONBOARDING_STATUS")
    @GetMapping("/onboarding-status/{organizationId}")
    @Operation(summary = "Public onboarding tracker — live KYC status for signup applicants")
    public OrganizationResponse getOnboardingStatus(
            @PathVariable Long organizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.getOnboardingStatus(organizationId, locale);
    }

    @Auditable(action = "ORG_SYSTEM_REGISTER")
    @PostMapping("/register")
    @Operation(summary = "Register organisation (admin / system)")
    public OrganizationResponse register(
            @Valid @ModelAttribute RegisterOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        if (request.getCreatedViaSignup() == null) {
            request.setCreatedViaSignup(false);
        }
        return organizationServiceProcessor.register(request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_FIND_BY_MULTIPLE_FILTERS")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Find organizations by multiple filters (paginated)")
    public OrganizationResponse findByMultipleFilters(
            @Valid @RequestBody OrganizationMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.findByMultipleFilters(request, SYSTEM, locale);
    }

    @Auditable(action = "ORG_SYSTEM_EXPORT_ORGANIZATIONS")
    @PostMapping("/export")
    @Operation(summary = "Export organizations (csv / xlsx / pdf)")
    public ResponseEntity<byte[]> exportOrganizations(
            @RequestBody OrganizationMultipleFiltersRequest filters,
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        try {
            logger.info("Export organizations format={}", format);
            List<OrganizationDto> items =
                    organizationServiceProcessor.listOrganizationsForExport(filters, SYSTEM, locale);
            byte[] data = exportHelper.organizationsToBytes(items, format);
            return exportResponse(data, format, "organizations");
        } catch (Exception e) {
            logger.error("Export organizations failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Export failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "ORG_SYSTEM_PROVISION_CONTACT_PERSON")
    @PostMapping("/{id}/provision-contact-person")
    @Operation(summary = "Provision organisation contact person user",
            description = "Creates (or refreshes) the contact person user linked to this organisation and sends verification email.")
    public OrganizationResponse provisionContactPerson(
            @PathVariable("id") Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.provisionContactPerson(id, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_UPDATE")
    @PutMapping("/{id}")
    @Operation(summary = "Update organisation profile and documents",
            description = "User updates: allowed only while KYC is DRAFT or RESUBMITTED. "
                    + "SYSTEM (admin gateway) bypasses KYC restriction for approved organisations.")
    public OrganizationResponse update(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateOrganizationRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.update(id, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_GET_BY_ID")
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get organisation by id", description = "Includes branches, agents, linked customers and contracted transporters.")
    public OrganizationResponse getById(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.getByIdForSystem(id, locale);
    }

    @Auditable(action = "ORG_SYSTEM_DELETE")
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete organisation")
    public OrganizationResponse delete(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.delete(id, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_QUEUE")
    @GetMapping("/kyc/queue")
    @Operation(summary = "Paginated KYC queue")
    public OrganizationResponse kycQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String classification,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        Pageable pageable = PageRequest.of(page, size);
        return organizationServiceProcessor.getKycQueue(status, classification, pageable, locale);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE1_APPROVE")
    @PostMapping("/{id}/kyc/stage1/approve")
    public OrganizationResponse stage1Approve(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage1Approve(id, request != null ? request : new KycActionRequest(), locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE1_REJECT")
    @PostMapping("/{id}/kyc/stage1/reject")
    public OrganizationResponse stage1Reject(
            @PathVariable Long id,
            @RequestBody KycRejectRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage1Reject(id, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE2_APPROVE")
    @PostMapping("/{id}/kyc/stage2/approve")
    public OrganizationResponse stage2Approve(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage2Approve(id, request != null ? request : new KycActionRequest(), locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE2_REJECT")
    @PostMapping("/{id}/kyc/stage2/reject")
    public OrganizationResponse stage2Reject(
            @PathVariable Long id,
            @RequestBody KycRejectRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage2Reject(id, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE3_APPROVE")
    @PostMapping("/{id}/kyc/stage3/approve")
    public OrganizationResponse stage3Approve(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage3Approve(id, request != null ? request : new KycActionRequest(), locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE3_REJECT")
    @PostMapping("/{id}/kyc/stage3/reject")
    public OrganizationResponse stage3Reject(
            @PathVariable Long id,
            @RequestBody KycRejectRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage3Reject(id, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE4_APPROVE")
    @PostMapping("/{id}/kyc/stage4/approve")
    public OrganizationResponse stage4Approve(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage4Approve(id, request != null ? request : new KycActionRequest(), locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE4_REJECT")
    @PostMapping("/{id}/kyc/stage4/reject")
    public OrganizationResponse stage4Reject(
            @PathVariable Long id,
            @RequestBody KycRejectRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage4Reject(id, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE5_APPROVE")
    @PostMapping("/{id}/kyc/stage5/approve")
    public OrganizationResponse stage5Approve(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage5Approve(id, request != null ? request : new KycActionRequest(), locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE5_REJECT")
    @PostMapping("/{id}/kyc/stage5/reject")
    public OrganizationResponse stage5Reject(
            @PathVariable Long id,
            @RequestBody KycRejectRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage5Reject(id, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_ALLOW_RESUBMISSION")
    @PostMapping("/{id}/allow-resubmission")
    public OrganizationResponse allowResubmission(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.allowResubmission(id, request != null ? request : new KycActionRequest(), locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_REVIEWS")
    @GetMapping("/{id}/kyc/reviews")
    public OrganizationResponse kycReviews(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.listKycReviews(id, locale);
    }

    // =========================================================
    // Industries
    // =========================================================

    @Auditable(action = "ORG_SYSTEM_FIND_INDUSTRIES_BY_MULTIPLE_FILTERS")
    @PostMapping("/industries/find-by-multiple-filters")
    @Operation(summary = "Find industries by multiple filters (paginated, with usage statistics)")
    public OrganizationResponse findIndustriesByMultipleFilters(
            @Valid @RequestBody IndustryMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.findIndustriesByMultipleFilters(request, locale);
    }

    @Auditable(action = "ORG_SYSTEM_LIST_INDUSTRIES")
    @GetMapping("/industries")
    @Operation(summary = "List industries with organisation usage statistics")
    public OrganizationResponse listIndustries(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.listIndustriesWithUsage(locale);
    }

    @Auditable(action = "ORG_SYSTEM_GET_INDUSTRY")
    @GetMapping("/industries/{industryId}")
    @Operation(summary = "Get industry by id")
    public OrganizationResponse getIndustry(
            @PathVariable Long industryId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.getIndustryById(industryId, locale);
    }

    @Auditable(action = "ORG_SYSTEM_CREATE_INDUSTRY")
    @PostMapping("/industries")
    @Operation(summary = "Create industry")
    public OrganizationResponse createIndustry(
            @Valid @RequestBody CreateIndustryRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.createIndustry(request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_UPDATE_INDUSTRY")
    @PutMapping("/industries/{industryId}")
    @Operation(summary = "Update industry")
    public OrganizationResponse updateIndustry(
            @PathVariable Long industryId,
            @Valid @RequestBody UpdateIndustryRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.updateIndustry(industryId, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_DELETE_INDUSTRY")
    @DeleteMapping("/industries/{industryId}")
    @Operation(summary = "Soft-delete industry")
    public OrganizationResponse deleteIndustry(
            @PathVariable Long industryId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.deleteIndustry(industryId, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_EXPORT_INDUSTRIES")
    @PostMapping("/industries/export")
    @Operation(summary = "Export industries (csv / xlsx / pdf)")
    public ResponseEntity<byte[]> exportIndustries(
            @RequestBody IndustryMultipleFiltersRequest filters,
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        try {
            logger.info("Export industries format={}", format);
            filters.setPage(0);
            filters.setSize(Integer.MAX_VALUE);
            List<IndustryDto> items = organizationServiceProcessor.listIndustriesForExport(filters, locale);
            byte[] data = exportHelper.industriesToBytes(items, format);
            return exportResponse(data, format, "industries");
        } catch (Exception e) {
            logger.error("Export industries failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Export failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "ORG_SYSTEM_IMPORT_INDUSTRIES_CSV")
    @PostMapping("/industries/import-csv")
    @Operation(summary = "Import industries from CSV file")
    public ResponseEntity<ImportSummary> importIndustriesFromCsv(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return handleCsvImport(file, locale, SYSTEM, "industries");
    }

    // =========================================================
    // Branches
    // =========================================================

    @Auditable(action = "ORG_SYSTEM_FIND_BRANCHES_BY_MULTIPLE_FILTERS")
    @PostMapping("/branches/find-by-multiple-filters")
    @Operation(summary = "Find branches across organisations (paginated)")
    public OrganizationResponse findBranchesByMultipleFilters(
            @Valid @RequestBody BranchMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.findBranchesByMultipleFilters(request, locale);
    }

    @Auditable(action = "ORG_SYSTEM_CREATE_BRANCH")
    @PostMapping("/branches")
    @Operation(summary = "Create a new branch for an organisation")
    public OrganizationResponse createBranch(
            @Valid @RequestBody CreateBranchRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.createBranch(request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_GET_BRANCH")
    @GetMapping("/branches/{branchId}")
    @Operation(summary = "Get branch by ID")
    public OrganizationResponse getBranchById(
            @PathVariable Long branchId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.getBranchById(branchId, locale);
    }

    @Auditable(action = "ORG_SYSTEM_UPDATE_BRANCH")
    @PutMapping("/branches/{branchId}")
    @Operation(summary = "Update branch details")
    public OrganizationResponse updateBranch(
            @PathVariable Long branchId,
            @Valid @RequestBody UpdateBranchRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.updateBranch(branchId, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_DELETE_BRANCH")
    @DeleteMapping("/branches/{branchId}")
    @Operation(summary = "Soft-delete a branch")
    public OrganizationResponse deleteBranch(
            @PathVariable Long branchId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.deleteBranch(branchId, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_EXPORT_BRANCHES")
    @PostMapping("/branches/export")
    @Operation(summary = "Export branches (csv / xlsx / pdf)")
    public ResponseEntity<byte[]> exportBranches(
            @RequestBody BranchMultipleFiltersRequest filters,
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        try {
            logger.info("Export branches format={}", format);
            List<BranchDto> items = organizationServiceProcessor.listBranchesForExport(filters, locale);
            byte[] data = exportHelper.branchesToBytes(items, format);
            return exportResponse(data, format, "branches");
        } catch (Exception e) {
            logger.error("Export branches failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Export failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "ORG_SYSTEM_IMPORT_BRANCHES_CSV")
    @PostMapping("/branches/import-csv")
    @Operation(summary = "Import branches from CSV file")
    public ResponseEntity<ImportSummary> importBranchesFromCsv(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return handleCsvImport(file, locale, SYSTEM, "branches");
    }

    // =========================================================
    // Agents
    // =========================================================

    @Auditable(action = "ORG_SYSTEM_FIND_AGENTS_BY_MULTIPLE_FILTERS")
    @PostMapping("/agents/find-by-multiple-filters")
    @Operation(summary = "Find agents across organisations (paginated)")
    public OrganizationResponse findAgentsByMultipleFilters(
            @Valid @RequestBody AgentMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.findAgentsByMultipleFilters(request, locale);
    }

    @Auditable(action = "ORG_SYSTEM_CREATE_AGENT")
    @PostMapping("/agents")
    @Operation(summary = "Create a new agent for an organisation")
    public OrganizationResponse createAgent(
            @Valid @RequestBody CreateAgentRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.createAgent(request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_GET_AGENT")
    @GetMapping("/agents/{agentId}")
    @Operation(summary = "Get agent by ID")
    public OrganizationResponse getAgentById(
            @PathVariable Long agentId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.getAgentById(agentId, locale);
    }

    @Auditable(action = "ORG_SYSTEM_UPDATE_AGENT")
    @PutMapping("/agents/{agentId}")
    @Operation(summary = "Update agent details")
    public OrganizationResponse updateAgent(
            @PathVariable Long agentId,
            @Valid @RequestBody UpdateAgentRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.updateAgent(agentId, request, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_DELETE_AGENT")
    @DeleteMapping("/agents/{agentId}")
    @Operation(summary = "Soft-delete an agent")
    public OrganizationResponse deleteAgent(
            @PathVariable Long agentId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.deleteAgent(agentId, locale, SYSTEM);
    }

    @Auditable(action = "ORG_SYSTEM_EXPORT_AGENTS")
    @PostMapping("/agents/export")
    @Operation(summary = "Export agents (csv / xlsx / pdf)")
    public ResponseEntity<byte[]> exportAgents(
            @RequestBody AgentMultipleFiltersRequest filters,
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        try {
            logger.info("Export agents format={}", format);
            List<AgentDto> items = organizationServiceProcessor.listAgentsForExport(filters, locale);
            byte[] data = exportHelper.agentsToBytes(items, format);
            return exportResponse(data, format, "agents");
        } catch (Exception e) {
            logger.error("Export agents failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Export failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "ORG_SYSTEM_IMPORT_AGENTS_CSV")
    @PostMapping("/agents/import-csv")
    @Operation(summary = "Import agents from CSV file")
    public ResponseEntity<ImportSummary> importAgentsFromCsv(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return handleCsvImport(file, locale, SYSTEM, "agents");
    }

    // =========================================================
    // Private helpers
    // =========================================================

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

    private ResponseEntity<ImportSummary> handleCsvImport(MultipartFile file, Locale locale,
                                                           String actor, String entityLabel) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Empty file", 0, 0, 0, List.of("Empty file provided")));
        }
        try (InputStream is = file.getInputStream()) {
            ImportSummary summary;
            if ("branches".equals(entityLabel)) {
                summary = organizationServiceProcessor.importBranchesFromCsv(is, locale, actor);
            } else if ("agents".equals(entityLabel)) {
                summary = organizationServiceProcessor.importAgentsFromCsv(is, locale, actor);
            } else {
                summary = organizationServiceProcessor.importIndustriesFromCsv(is, locale, actor);
            }
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            logger.error("CSV import failed for {}", entityLabel, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Import failed: " + e.getMessage(), 0, 0, 0,
                            List.of(e.getMessage())));
        }
    }
}
