package projectlx.co.zw.organizationmanagement.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.organizationmanagement.service.processor.api.OrganizationServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.requests.KycActionRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycRejectRequest;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-organization-management/v1/backoffice/organization")
@Tag(name = "Organization (backoffice)", description = "Internal KYC queue and actions (no JWT)")
@RequiredArgsConstructor
public class OrganizationBackofficeResource {

    private static final String BACKOFFICE = "BACKOFFICE";

    private final OrganizationServiceProcessor organizationServiceProcessor;

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

    @Auditable(action = "ORG_SYSTEM_GET_BY_ID")
    @GetMapping("/{id}")
    public OrganizationResponse getById(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.getById(id, locale);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE1_APPROVE")
    @PostMapping("/{id}/kyc/stage1/approve")
    public OrganizationResponse stage1Approve(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage1Approve(id, request != null ? request : new KycActionRequest(), locale, BACKOFFICE);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE1_REJECT")
    @PostMapping("/{id}/kyc/stage1/reject")
    public OrganizationResponse stage1Reject(
            @PathVariable Long id,
            @RequestBody KycRejectRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage1Reject(id, request, locale, BACKOFFICE);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE2_APPROVE")
    @PostMapping("/{id}/kyc/stage2/approve")
    public OrganizationResponse stage2Approve(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage2Approve(id, request != null ? request : new KycActionRequest(), locale, BACKOFFICE);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_STAGE2_REJECT")
    @PostMapping("/{id}/kyc/stage2/reject")
    public OrganizationResponse stage2Reject(
            @PathVariable Long id,
            @RequestBody KycRejectRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.stage2Reject(id, request, locale, BACKOFFICE);
    }

    @Auditable(action = "ORG_SYSTEM_ALLOW_RESUBMISSION")
    @PostMapping("/{id}/allow-resubmission")
    public OrganizationResponse allowResubmission(
            @PathVariable Long id,
            @RequestBody(required = false) KycActionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.allowResubmission(id, request != null ? request : new KycActionRequest(), locale, BACKOFFICE);
    }

    @Auditable(action = "ORG_SYSTEM_KYC_REVIEWS")
    @GetMapping("/{id}/kyc/reviews")
    public OrganizationResponse kycReviews(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return organizationServiceProcessor.listKycReviews(id, locale);
    }
}
