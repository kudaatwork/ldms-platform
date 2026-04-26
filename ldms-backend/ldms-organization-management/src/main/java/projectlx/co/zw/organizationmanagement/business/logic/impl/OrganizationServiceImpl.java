package projectlx.co.zw.organizationmanagement.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import projectlx.co.zw.organizationmanagement.business.auditable.api.BranchServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationKycReviewServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.kyc.KycStateMachine;
import projectlx.co.zw.organizationmanagement.business.kyc.OrganizationEventPublisher;
import projectlx.co.zw.organizationmanagement.business.logic.api.OrganizationService;
import projectlx.co.zw.organizationmanagement.business.validation.api.OrganizationServiceValidator;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.KycDecision;
import projectlx.co.zw.organizationmanagement.model.KycReviewStage;
import projectlx.co.zw.organizationmanagement.model.KycStatus;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;
import projectlx.co.zw.organizationmanagement.model.OrganizationKycReview;
import projectlx.co.zw.organizationmanagement.repository.BranchRepository;
import projectlx.co.zw.organizationmanagement.repository.IndustryRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationKycReviewRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;
import projectlx.co.zw.organizationmanagement.repository.specification.OrganizationSpecifications;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationKycReviewDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationMapping;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.utils.exceptions.BusinessRuleException;
import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycActionRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycRejectRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Transactional
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final IndustryRepository industryRepository;
    private final BranchRepository branchRepository;
    private final OrganizationKycReviewRepository organizationKycReviewRepository;
    private final OrganizationServiceAuditable organizationServiceAuditable;
    private final OrganizationKycReviewServiceAuditable organizationKycReviewServiceAuditable;
    private final BranchServiceAuditable branchServiceAuditable;
    private final OrganizationServiceValidator organizationServiceValidator;
    private final KycStateMachine kycStateMachine;
    private final OrganizationEventPublisher organizationEventPublisher;
    private final MessageService messageService;

    @Override
    public OrganizationResponse register(RegisterOrganizationRequest request, Locale locale, String createdBy) {
        ValidatorDto v = organizationServiceValidator.validateRegister(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        if (organizationRepository.findByEmailAndEntityStatusNot(request.getEmail().trim(), EntityStatus.DELETED).isPresent()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_EMAIL_EXISTS.getCode(), new String[]{}, locale)));
        }
        Organization org = new Organization();
        org.setName(request.getName().trim());
        org.setEmail(request.getEmail().trim().toLowerCase());
        org.setPhoneNumber(request.getPhoneNumber());
        org.setOrganizationClassification(request.getOrganizationClassification());
        if (request.getIndustryId() != null) {
            industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED).ifPresent(org::setIndustry);
        }
        org.setContactPersonFirstName(request.getContactPersonFirstName());
        org.setContactPersonLastName(request.getContactPersonLastName());
        org.setContactPersonEmail(request.getContactPersonEmail());
        org.setContactPersonPhoneNumber(request.getContactPersonPhoneNumber());
        org.setKycStatus(KycStatus.DRAFT);
        org.setEntityStatus(EntityStatus.ACTIVE);
        org.setCreatedAt(LocalDateTime.now());
        org.setCreatedBy(createdBy);
        org.setVerified(false);
        org.setCurrentResubmissionCycle(0);
        org.setResubmissionCount(0);
        Organization saved = organizationServiceAuditable.save(org);
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse submitKyc(Locale locale, String username) {
        Organization org = loadForUser(username);
        KycStatus from = org.getKycStatus();
        if (from == KycStatus.DRAFT) {
            kycStateMachine.assertCanTransition(KycStatus.DRAFT, KycStatus.SUBMITTED, locale);
        } else if (from == KycStatus.RESUBMITTED) {
            kycStateMachine.assertCanTransition(KycStatus.RESUBMITTED, KycStatus.SUBMITTED, locale);
        } else {
            throw new BusinessRuleException(
                    messageService.getMessage(I18Code.KYC_INVALID_TRANSITION.getCode(), new String[]{}, locale),
                    I18Code.KYC_INVALID_TRANSITION);
        }
        org.setKycStatus(KycStatus.SUBMITTED);
        org.setSubmittedAt(LocalDateTime.now());
        org.setModifiedAt(LocalDateTime.now());
        org.setModifiedBy(username);
        Organization saved = organizationServiceAuditable.save(org);
        Organization snapshot = saved;
        afterCommit(() -> organizationEventPublisher.publishSubmitted(snapshot));
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getMy(Locale locale, String username) {
        Organization org = loadForUser(username);
        OrganizationDto dto = OrganizationMapping.toDto(org);
        dto.setBranchDtoList(OrganizationMapping.toBranchDtos(branchRepository.findByOrganizationAndEntityStatusNot(org, EntityStatus.DELETED)));
        return buildOrganizationResponse(dto);
    }

    @Override
    public OrganizationResponse updateMy(UpdateMyOrganizationRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateUpdateMy(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization org = loadForUser(username);
        KycStatus ks = org.getKycStatus();
        if (ks != KycStatus.DRAFT && ks != KycStatus.RESUBMITTED) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_UPDATE_FORBIDDEN_STATUS.getCode(), new String[]{}, locale)));
        }
        if (request.getName() != null) {
            org.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            org.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getLocationId() != null) {
            org.setLocationId(request.getLocationId());
        }
        if (request.getWebsiteUrl() != null) {
            org.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getOrganizationDescription() != null) {
            org.setOrganizationDescription(request.getOrganizationDescription());
        }
        if (request.getBusinessHours() != null) {
            org.setBusinessHours(request.getBusinessHours());
        }
        if (request.getNumberOfEmployees() != null) {
            org.setNumberOfEmployees(request.getNumberOfEmployees());
        }
        if (request.getAnnualRevenueEstimate() != null) {
            org.setAnnualRevenueEstimate(request.getAnnualRevenueEstimate());
        }
        if (request.getRegionsServed() != null) {
            org.setRegionsServed(request.getRegionsServed());
        }
        org.setModifiedAt(LocalDateTime.now());
        org.setModifiedBy(username);
        Organization saved = organizationServiceAuditable.save(org);
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse addBranch(AddBranchRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateAddBranch(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization org = loadForUser(username);
        Branch b = new Branch();
        b.setOrganization(org);
        b.setBranchName(request.getBranchName());
        b.setBranchCode(request.getBranchCode());
        b.setLocationId(request.getLocationId());
        b.setPhoneNumber(request.getPhoneNumber());
        b.setEmail(request.getEmail());
        b.setLatitude(request.getLatitude());
        b.setLongitude(request.getLongitude());
        b.setHeadOffice(request.isHeadOffice());
        b.setRegion(request.getRegion());
        b.setBusinessHours(request.getBusinessHours());
        b.setActive(true);
        b.setEntityStatus(EntityStatus.ACTIVE);
        b.setCreatedAt(LocalDateTime.now());
        b.setCreatedBy(username);
        branchServiceAuditable.save(b);
        return getMy(locale, username);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse listBranches(Locale locale, String username) {
        Organization org = loadForUser(username);
        OrganizationResponse res = buildOrganizationResponse(null);
        OrganizationDto dto = OrganizationMapping.toDto(org);
        dto.setBranchDtoList(OrganizationMapping.toBranchDtos(branchRepository.findByOrganizationAndEntityStatusNot(org, EntityStatus.DELETED)));
        res.setOrganizationDto(dto);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse listCustomers(Locale locale, String username) {
        Organization org = loadForUser(username);
        if (org.getOrganizationClassification() != OrganizationClassification.SUPPLIER) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_CUSTOMERS.getCode(), new String[]{}, locale)));
        }
        List<OrganizationDto> dtos = new ArrayList<>();
        for (Organization c : org.getCustomers()) {
            if (c.getEntityStatus() != EntityStatus.DELETED) {
                dtos.add(OrganizationMapping.toDto(c));
            }
        }
        OrganizationResponse res = buildOrganizationResponse(null);
        res.setOrganizationDtoList(dtos);
        return res;
    }

    @Override
    public OrganizationResponse registerCustomer(RegisterCustomerOrganizationRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateRegisterCustomer(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization supplier = loadForUser(username);
        if (supplier.getOrganizationClassification() != OrganizationClassification.SUPPLIER) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_CUSTOMERS.getCode(), new String[]{}, locale)));
        }
        if (organizationRepository.findByEmailAndEntityStatusNot(request.getEmail().trim(), EntityStatus.DELETED).isPresent()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_EMAIL_EXISTS.getCode(), new String[]{}, locale)));
        }
        Organization customer = new Organization();
        customer.setName(request.getName().trim());
        customer.setEmail(request.getEmail().trim().toLowerCase());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setOrganizationClassification(OrganizationClassification.CUSTOMER);
        customer.setCreatedViaSignup(false);
        customer.setKycStatus(KycStatus.DRAFT);
        customer.setEntityStatus(EntityStatus.ACTIVE);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setCreatedBy(username);
        customer.setVerified(false);
        customer.setCurrentResubmissionCycle(0);
        customer.setResubmissionCount(0);
        Organization savedCustomer = organizationServiceAuditable.save(customer);
        supplier.getCustomers().add(savedCustomer);
        savedCustomer.getSuppliers().add(supplier);
        organizationServiceAuditable.save(supplier);
        return buildOrganizationResponse(OrganizationMapping.toDto(savedCustomer));
    }

    @Override
    public OrganizationResponse linkTransporter(LinkTransporterRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateLinkTransporter(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization org = loadForUser(username);
        Organization transporter = organizationRepository.findByIdAndEntityStatusNot(request.getTransporterOrganizationId(), EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (transporter.getOrganizationClassification() != OrganizationClassification.TRANSPORT_COMPANY) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_TRANSPORTER_LINK.getCode(), new String[]{}, locale)));
        }
        if (!org.getContractedTransporters().contains(transporter)) {
            org.getContractedTransporters().add(transporter);
            transporter.getContractingOrganizations().add(org);
            organizationServiceAuditable.save(org);
            organizationServiceAuditable.save(transporter);
        }
        return buildOrganizationResponse(OrganizationMapping.toDto(org));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getKycQueue(String status, String classification, Pageable pageable, Locale locale) {
        OrganizationClassification oc = null;
        if (classification != null && !classification.isBlank()) {
            oc = OrganizationClassification.valueOf(classification.trim());
        }
        Specification<Organization> spec = OrganizationSpecifications.kycQueue(status, oc);
        Page<Organization> page = organizationRepository.findAll(spec, pageable);
        Page<OrganizationDto> dtoPage = page.map(OrganizationMapping::toDto);
        OrganizationResponse res = buildOrganizationResponse(null);
        res.setOrganizationDtoPage(dtoPage);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getById(Long id, Locale locale) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        return buildOrganizationResponse(OrganizationMapping.toDto(org));
    }

    @Override
    public OrganizationResponse stage1Approve(Long id, KycActionRequest request, Locale locale, String username) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        KycStatus s = org.getKycStatus();
        if (s == KycStatus.SUBMITTED) {
            kycStateMachine.assertCanTransition(KycStatus.SUBMITTED, KycStatus.STAGE_1_REVIEW, locale);
            s = KycStatus.STAGE_1_REVIEW;
        }
        if (s != KycStatus.STAGE_1_REVIEW) {
            throw new BusinessRuleException(
                    messageService.getMessage(I18Code.KYC_INVALID_TRANSITION.getCode(), new String[]{}, locale),
                    I18Code.KYC_INVALID_TRANSITION);
        }
        kycStateMachine.assertCanTransition(KycStatus.STAGE_1_REVIEW, KycStatus.STAGE_2_REVIEW, locale);
        String reviewer = resolveReviewer(request, username);
        LocalDateTime reviewedAt = LocalDateTime.now();
        OrganizationKycReview row = buildReviewRow(org, KycReviewStage.STAGE_1, KycDecision.APPROVED, request, reviewer, reviewedAt, null);
        organizationKycReviewServiceAuditable.save(row);
        org.setKycStatus(KycStatus.STAGE_2_REVIEW);
        org.setStage1ReviewedBy(reviewer);
        org.setStage1ReviewedAt(reviewedAt);
        org.setModifiedAt(reviewedAt);
        org.setModifiedBy(username);
        Organization saved = organizationServiceAuditable.save(org);
        afterCommit(() -> organizationEventPublisher.publishStage1Approved(saved, reviewer, reviewedAt));
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse stage1Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        Objects.requireNonNull(request.getRejectionReason(), "rejectionReason");
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        KycStatus s = org.getKycStatus();
        if (s == KycStatus.SUBMITTED) {
            kycStateMachine.assertCanTransition(KycStatus.SUBMITTED, KycStatus.STAGE_1_REVIEW, locale);
            s = KycStatus.STAGE_1_REVIEW;
        }
        if (s != KycStatus.STAGE_1_REVIEW) {
            throw new BusinessRuleException(
                    messageService.getMessage(I18Code.KYC_INVALID_TRANSITION.getCode(), new String[]{}, locale),
                    I18Code.KYC_INVALID_TRANSITION);
        }
        kycStateMachine.assertCanTransition(KycStatus.STAGE_1_REVIEW, KycStatus.REJECTED, locale);
        String reviewer = resolveReviewer(request, username);
        LocalDateTime reviewedAt = LocalDateTime.now();
        OrganizationKycReview row = buildReviewRow(
                org, KycReviewStage.STAGE_1, KycDecision.REJECTED, request, reviewer, reviewedAt, request.getRejectionReason());
        organizationKycReviewServiceAuditable.save(row);
        org.setKycStatus(KycStatus.REJECTED);
        org.setStage1ReviewedBy(reviewer);
        org.setStage1ReviewedAt(reviewedAt);
        org.setLastRejectionReason(request.getRejectionReason());
        org.setModifiedAt(reviewedAt);
        org.setModifiedBy(username);
        Organization saved = organizationServiceAuditable.save(org);
        afterCommit(() -> organizationEventPublisher.publishRejected(saved, KycReviewStage.STAGE_1, request.getRejectionReason(), reviewer, reviewedAt));
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse stage2Approve(Long id, KycActionRequest request, Locale locale, String username) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        kycStateMachine.assertCanTransition(KycStatus.STAGE_2_REVIEW, KycStatus.APPROVED, locale);
        String reviewer = resolveReviewer(request, username);
        LocalDateTime reviewedAt = LocalDateTime.now();
        OrganizationKycReview row = buildReviewRow(org, KycReviewStage.STAGE_2, KycDecision.APPROVED, request, reviewer, reviewedAt, null);
        organizationKycReviewServiceAuditable.save(row);
        org.setKycStatus(KycStatus.APPROVED);
        org.setVerified(true);
        org.setStage2ReviewedBy(reviewer);
        org.setStage2ReviewedAt(reviewedAt);
        org.setModifiedAt(reviewedAt);
        org.setModifiedBy(username);
        Organization saved = organizationServiceAuditable.save(org);
        afterCommit(() -> {
            organizationEventPublisher.publishStage2Approved(saved, reviewer, reviewedAt);
            organizationEventPublisher.publishVerified(saved, reviewedAt);
        });
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse stage2Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        Objects.requireNonNull(request.getRejectionReason(), "rejectionReason");
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        kycStateMachine.assertCanTransition(KycStatus.STAGE_2_REVIEW, KycStatus.REJECTED, locale);
        String reviewer = resolveReviewer(request, username);
        LocalDateTime reviewedAt = LocalDateTime.now();
        OrganizationKycReview row = buildReviewRow(
                org, KycReviewStage.STAGE_2, KycDecision.REJECTED, request, reviewer, reviewedAt, request.getRejectionReason());
        organizationKycReviewServiceAuditable.save(row);
        org.setKycStatus(KycStatus.REJECTED);
        org.setStage2ReviewedBy(reviewer);
        org.setStage2ReviewedAt(reviewedAt);
        org.setLastRejectionReason(request.getRejectionReason());
        org.setModifiedAt(reviewedAt);
        org.setModifiedBy(username);
        Organization saved = organizationServiceAuditable.save(org);
        afterCommit(() -> organizationEventPublisher.publishRejected(saved, KycReviewStage.STAGE_2, request.getRejectionReason(), reviewer, reviewedAt));
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse allowResubmission(Long id, KycActionRequest request, Locale locale, String username) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        kycStateMachine.assertCanTransition(KycStatus.REJECTED, KycStatus.RESUBMITTED, locale);
        LocalDateTime now = LocalDateTime.now();
        org.setKycStatus(KycStatus.RESUBMITTED);
        org.setResubmissionCount(org.getResubmissionCount() + 1);
        org.setCurrentResubmissionCycle(org.getCurrentResubmissionCycle() + 1);
        org.setLastRejectionReason(null);
        org.setModifiedAt(now);
        org.setModifiedBy(username);
        Organization saved = organizationServiceAuditable.save(org);
        String allowedBy = resolveReviewer(request, username);
        afterCommit(() -> organizationEventPublisher.publishResubmitted(saved, allowedBy, now));
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse listKycReviews(Long id, Locale locale) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        List<OrganizationKycReview> rows = organizationKycReviewRepository.findByOrganizationOrderByReviewedAtDesc(org);
        List<OrganizationKycReviewDto> dtos = new ArrayList<>();
        for (OrganizationKycReview r : rows) {
            OrganizationKycReviewDto d = new OrganizationKycReviewDto();
            d.setId(r.getId());
            d.setStage(r.getStage() != null ? r.getStage().name() : null);
            d.setDecision(r.getDecision() != null ? r.getDecision().name() : null);
            d.setReviewerUserId(r.getReviewerUserId());
            d.setReviewerUsername(r.getReviewerUsername());
            d.setRejectionReason(r.getRejectionReason());
            d.setNotes(r.getNotes());
            d.setResubmissionCycle(r.getResubmissionCycle());
            d.setReviewedAt(r.getReviewedAt());
            dtos.add(d);
        }
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setKycReviews(dtos);
        return res;
    }

    private OrganizationKycReview buildReviewRow(
            Organization org,
            KycReviewStage stage,
            KycDecision decision,
            KycActionRequest request,
            String reviewerUsername,
            LocalDateTime reviewedAt,
            String rejectionReason) {
        OrganizationKycReview r = new OrganizationKycReview();
        r.setOrganization(org);
        r.setStage(stage);
        r.setDecision(decision);
        r.setReviewerUserId(request != null ? request.getReviewerUserId() : null);
        r.setReviewerUsername(reviewerUsername);
        r.setRejectionReason(rejectionReason);
        r.setNotes(request != null ? request.getNotes() : null);
        r.setResubmissionCycle(org.getCurrentResubmissionCycle());
        r.setReviewedAt(reviewedAt);
        r.setEntityStatus(EntityStatus.ACTIVE);
        r.setCreatedAt(reviewedAt);
        r.setCreatedBy(reviewerUsername);
        return r;
    }

    private String resolveReviewer(KycActionRequest request, String defaultName) {
        if (request != null && request.getReviewerUsername() != null && !request.getReviewerUsername().isBlank()) {
            return request.getReviewerUsername().trim();
        }
        return defaultName;
    }

    private Organization loadForUser(String username) {
        return organizationRepository
                .findByEmailAndEntityStatusNot(username.toLowerCase(), EntityStatus.DELETED)
                .orElseThrow(() -> new BusinessRuleException(
                        messageService.getMessage(I18Code.ORG_NOT_FOUND.getCode(), new String[]{}, Locale.getDefault()),
                        I18Code.ORG_NOT_FOUND));
    }

    private BusinessRuleException notFound(Locale locale) {
        return new BusinessRuleException(
                messageService.getMessage(I18Code.ORG_NOT_FOUND.getCode(), new String[]{}, locale), I18Code.ORG_NOT_FOUND);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (RuntimeException ex) {
                        // Publisher swallows failures; extra safety
                    }
                }
            });
        } else {
            action.run();
        }
    }

    protected OrganizationResponse buildOrganizationResponse(OrganizationDto dto) {
        OrganizationResponse r = new OrganizationResponse();
        r.setSuccess(true);
        r.setStatusCode(200);
        r.setOrganizationDto(dto);
        return r;
    }

    protected OrganizationResponse buildOrganizationResponseWithErrors(List<String> errors) {
        OrganizationResponse r = new OrganizationResponse();
        r.setSuccess(false);
        r.setStatusCode(400);
        r.setErrorMessages(errors);
        return r;
    }
}
