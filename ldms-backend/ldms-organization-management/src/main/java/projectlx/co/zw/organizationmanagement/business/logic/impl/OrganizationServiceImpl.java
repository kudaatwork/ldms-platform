package projectlx.co.zw.organizationmanagement.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import projectlx.co.zw.organizationmanagement.business.auditable.api.BranchServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.IndustryServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationKycReviewServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.kyc.KycApproverAssignmentService;
import projectlx.co.zw.organizationmanagement.business.kyc.KycStateMachine;
import projectlx.co.zw.organizationmanagement.business.kyc.OrganizationEventPublisher;
import projectlx.co.zw.organizationmanagement.clients.UserManagementServiceClient;
import projectlx.co.zw.organizationmanagement.business.logic.api.OrganizationService;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationDirectoryAdminService;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationFileUploadHelper;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationContactPersonProvisioningSupport;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationKycNotifier;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationRegistrationNotifier;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationFileUploadHelper.UploadOutcome;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationFileUploadHelper.UploadPart;
import projectlx.co.zw.organizationmanagement.business.validation.api.OrganizationServiceValidator;
import projectlx.co.zw.organizationmanagement.model.Agent;
import projectlx.co.zw.organizationmanagement.model.AgentKind;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.organizationmanagement.model.KycDecision;
import projectlx.co.zw.organizationmanagement.model.KycReviewStage;
import projectlx.co.zw.organizationmanagement.model.KycStatus;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;
import projectlx.co.zw.organizationmanagement.model.OrganizationKycReview;
import projectlx.co.zw.organizationmanagement.repository.AgentRepository;
import projectlx.co.zw.organizationmanagement.repository.BranchRepository;
import projectlx.co.zw.organizationmanagement.repository.IndustryRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationKycReviewRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;
import projectlx.co.zw.organizationmanagement.repository.specification.AgentSpecifications;
import projectlx.co.zw.organizationmanagement.repository.specification.BranchSpecifications;
import projectlx.co.zw.organizationmanagement.repository.specification.IndustrySpecifications;
import projectlx.co.zw.organizationmanagement.repository.specification.OrganizationSpecifications;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryMapping;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryUsageDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationKycReviewDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationMapping;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.utils.exceptions.BusinessRuleException;
import projectlx.co.zw.organizationmanagement.utils.dtos.ImportSummary;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;
import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateIndustryRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateIndustryRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.AgentMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.BranchMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.IndustryMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycActionRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycRejectRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;
import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongConsumer;

@Transactional
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceImpl.class);
    private static final String SYSTEM_MODIFIER = "SYSTEM";
    private static final Set<String> TRUSTED_ADMIN_MODIFIERS = Set.of(SYSTEM_MODIFIER, "BACKOFFICE");

    private final OrganizationRepository organizationRepository;
    private final IndustryRepository industryRepository;
    private final IndustryServiceAuditable industryServiceAuditable;
    private final BranchRepository branchRepository;
    private final AgentRepository agentRepository;
    private final OrganizationKycReviewRepository organizationKycReviewRepository;
    private final OrganizationServiceAuditable organizationServiceAuditable;
    private final OrganizationKycReviewServiceAuditable organizationKycReviewServiceAuditable;
    private final BranchServiceAuditable branchServiceAuditable;
    private final OrganizationServiceValidator organizationServiceValidator;
    private final KycStateMachine kycStateMachine;
    private final OrganizationEventPublisher organizationEventPublisher;
    private final KycApproverAssignmentService kycApproverAssignmentService;
    private final UserManagementServiceClient userManagementServiceClient;
    private final MessageService messageService;
    private final OrganizationFileUploadHelper organizationFileUploadHelper;
    private final OrganizationDirectoryAdminService organizationDirectoryAdminService;
    private final OrganizationRegistrationNotifier organizationRegistrationNotifier;
    private final OrganizationContactPersonProvisioningSupport organizationContactPersonProvisioningSupport;
    private final OrganizationKycNotifier organizationKycNotifier;

    @Override
    public OrganizationResponse register(RegisterOrganizationRequest request, Locale locale, String createdBy) {
        ValidatorDto v = organizationServiceValidator.validateRegister(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (organizationRepository.findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED).isPresent()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_EMAIL_EXISTS.getCode(), new String[]{}, locale)));
        }
        Optional<Organization> deletedOrganization = organizationRepository.findByEmail(normalizedEmail)
                .filter(organization -> organization.getEntityStatus() == EntityStatus.DELETED);
        Organization org = deletedOrganization.orElseGet(Organization::new);
        org.setName(request.getName().trim());
        org.setEmail(normalizedEmail);
        org.setPhoneNumber(request.getPhoneNumber());
        org.setOrganizationClassification(request.getOrganizationClassification());
        if (request.getOrganizationType() != null) {
            org.setOrganizationType(request.getOrganizationType());
        }
        if (request.getIndustryId() != null) {
            industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED).ifPresent(org::setIndustry);
        }
        org.setContactPersonFirstName(request.getContactPersonFirstName());
        org.setContactPersonLastName(request.getContactPersonLastName());
        org.setContactPersonEmail(StringUtils.hasText(request.getContactPersonEmail())
                ? request.getContactPersonEmail().trim().toLowerCase()
                : null);
        org.setContactPersonPhoneNumber(request.getContactPersonPhoneNumber());
        if (request.getContactPersonGender() != null) {
            org.setContactPersonGender(request.getContactPersonGender());
        }
        if (StringUtils.hasText(request.getContactPersonDateOfBirth())) {
            org.setContactPersonDateOfBirth(request.getContactPersonDateOfBirth().trim());
        }
        if (StringUtils.hasText(request.getContactPersonNationalIdNumber())) {
            org.setContactPersonNationalIdNumber(request.getContactPersonNationalIdNumber().trim());
        }
        if (StringUtils.hasText(request.getContactPersonPassportNumber())) {
            org.setContactPersonPassportNumber(request.getContactPersonPassportNumber().trim());
        }
        if (request.getRegistrationNumber() != null) {
            org.setRegistrationNumber(request.getRegistrationNumber().trim());
        }
        if (request.getTaxNumber() != null) {
            org.setTaxNumber(request.getTaxNumber().trim());
        }
        boolean viaSignup = request.getCreatedViaSignup() == null || Boolean.TRUE.equals(request.getCreatedViaSignup());
        org.setCreatedViaSignup(viaSignup);
        if (viaSignup) {
            org.setKycStatus(KycStatus.DRAFT);
            org.setVerified(false);
        } else {
            org.setKycStatus(KycStatus.APPROVED);
            org.setVerified(true);
            clearApproverAssignments(org);
        }
        org.setEntityStatus(EntityStatus.ACTIVE);
        if (deletedOrganization.isEmpty()) {
            org.setCreatedAt(LocalDateTime.now());
            org.setCreatedBy(createdBy);
        }
        org.setCurrentResubmissionCycle(0);
        org.setResubmissionCount(0);
        organizationServiceAuditable.save(org);
        if (viaSignup) {
            kycApproverAssignmentService.assignApprovers(org);
            organizationServiceAuditable.save(org);
        }

        UploadOutcome uploadOutcome = organizationFileUploadHelper.processUploads(
                org,
                List.of(new UploadPart(
                        request.getTaxClearanceCertificateUpload(),
                        request.getTaxClearanceCertificateUploadId(),
                        FileType.TAX_CLEARANCE_CERTIFICATE,
                        null,
                        org::setTaxClearanceCertificateUploadId)),
                locale,
                false);
        if (!uploadOutcome.success()) {
            return buildOrganizationResponseWithErrors(uploadOutcome.errorMessages());
        }

        UploadOutcome contactIdOutcome = organizationFileUploadHelper.processUploads(
                org,
                List.of(
                        contactUploadPart(
                                request.getContactPersonNationalIdUpload(),
                                request.getContactPersonNationalIdUploadId(),
                                FileType.NATIONAL_ID,
                                org,
                                org::setContactPersonNationalIdUploadId,
                                request.getContactPersonNationalIdExpiryDate()),
                        contactUploadPart(
                                request.getContactPersonPassportUpload(),
                                request.getContactPersonPassportUploadId(),
                                FileType.PASSPORT,
                                org,
                                org::setContactPersonPassportUploadId,
                                request.getContactPersonPassportExpiryDate())),
                locale,
                false);
        if (!contactIdOutcome.success()) {
            return buildOrganizationResponseWithErrors(contactIdOutcome.errorMessages());
        }

        if ((request.getTaxClearanceCertificateUpload() != null && !request.getTaxClearanceCertificateUpload().isEmpty())
                || request.getTaxClearanceCertificateUploadId() != null
                || (request.getContactPersonNationalIdUpload() != null && !request.getContactPersonNationalIdUpload().isEmpty())
                || request.getContactPersonNationalIdUploadId() != null
                || (request.getContactPersonPassportUpload() != null && !request.getContactPersonPassportUpload().isEmpty())
                || request.getContactPersonPassportUploadId() != null) {
            organizationServiceAuditable.save(org);
        }

        final Long registeredOrgId = org.getId();
        final boolean signupFlow = viaSignup;
        afterCommit(() -> {
            if (registeredOrgId == null) {
                return;
            }
            organizationRepository.findById(registeredOrgId).ifPresent(fresh -> {
                provisionContactPersonUserIfNeeded(fresh, signupFlow);
                organizationRepository.findById(registeredOrgId).ifPresent(afterProvision ->
                        organizationRegistrationNotifier.sendRegistrationEmails(afterProvision, signupFlow));
            });
        });
        return buildOrganizationResponse(OrganizationMapping.toDto(org));
    }

    @Override
    public OrganizationResponse provisionContactPerson(Long id, Locale locale, String modifiedBy) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (!StringUtils.hasText(org.getContactPersonEmail())) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                            new String[] { "contactPersonEmail" }, locale)));
        }
        boolean viaSignup = Boolean.TRUE.equals(org.getCreatedViaSignup());
        Long contactUserId = organizationContactPersonProvisioningSupport.provisionContactPersonUser(org, viaSignup);
        if (contactUserId == null) {
            return buildOrganizationResponseWithErrors(List.of(
                    "Could not create or link the contact person user. Confirm ldms-user-management is running, "
                            + "the contact email is unique, and check user-management logs for "
                            + "POST /ldms-user-management/v1/system/user/provision-organization-contact-person."));
        }
        org.setContactPersonUserId(contactUserId);
        org.setModifiedAt(LocalDateTime.now());
        org.setModifiedBy(modifiedBy);
        Organization saved = organizationServiceAuditable.save(org);
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    private void provisionContactPersonUserIfNeeded(Organization org, boolean viaSignup) {
        if (!StringUtils.hasText(org.getContactPersonEmail())) {
            return;
        }
        if (org.getContactPersonUserId() != null && org.getContactPersonUserId() > 0) {
            return;
        }
        Long contactUserId = organizationContactPersonProvisioningSupport.provisionContactPersonUser(org, viaSignup);
        if (contactUserId == null) {
            log.error(
                    "Contact person user was not provisioned for organisation {} ({}). "
                            + "Call POST .../organization/{}/provision-contact-person after user-management is up.",
                    org.getId(),
                    org.getContactPersonEmail(),
                    org.getId());
            return;
        }
        organizationRepository.findById(org.getId()).ifPresent(reloaded -> {
            if (reloaded.getContactPersonUserId() == null || reloaded.getContactPersonUserId() <= 0) {
                reloaded.setContactPersonUserId(contactUserId);
                reloaded.setModifiedAt(LocalDateTime.now());
                reloaded.setModifiedBy(SYSTEM_MODIFIER);
                organizationServiceAuditable.save(reloaded);
            }
        });
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
        if (Boolean.TRUE.equals(org.getCreatedViaSignup())) {
            kycApproverAssignmentService.assignApprovers(org);
        }
        Organization saved = organizationServiceAuditable.save(org);
        Organization snapshot = saved;
        afterCommit(() -> organizationEventPublisher.publishSubmitted(snapshot));
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse update(Long id, UpdateOrganizationRequest request, Locale locale, String modifiedBy) {
        ValidatorDto v = organizationServiceValidator.validateUpdate(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (!canUpdateOrganizationProfile(org, modifiedBy)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_UPDATE_FORBIDDEN_STATUS.getCode(), new String[]{}, locale)));
        }
        boolean identityLocked = isContactIdentityLocked(org) && !isTrustedAdminModifier(modifiedBy);
        if (!identityLocked && request.getEmail() != null) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            if (StringUtils.hasText(normalizedEmail)) {
                Optional<Organization> emailOwner = organizationRepository
                        .findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED);
                if (emailOwner.isPresent() && !emailOwner.get().getId().equals(id)) {
                    return buildOrganizationResponseWithErrors(
                            List.of(messageService.getMessage(I18Code.ORG_EMAIL_EXISTS.getCode(), new String[]{}, locale)));
                }
                org.setEmail(normalizedEmail);
            }
        }
        applyUpdateScalars(org, request, identityLocked);
        org.setModifiedAt(LocalDateTime.now());
        org.setModifiedBy(modifiedBy);
        organizationServiceAuditable.save(org);

        UploadOutcome uploadOutcome = organizationFileUploadHelper.processUploads(
                org, buildUpdateUploadParts(request, org, identityLocked), locale, true);
        if (!uploadOutcome.success()) {
            return buildOrganizationResponseWithErrors(uploadOutcome.errorMessages());
        }
        organizationServiceAuditable.save(org);
        return buildOrganizationResponse(OrganizationMapping.toDto(org));
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
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (organizationRepository.findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED).isPresent()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_EMAIL_EXISTS.getCode(), new String[]{}, locale)));
        }
        Optional<Organization> deletedCustomer = organizationRepository.findByEmail(normalizedEmail)
                .filter(organization -> organization.getEntityStatus() == EntityStatus.DELETED);
        Organization customer = deletedCustomer.orElseGet(Organization::new);
        customer.setName(request.getName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setOrganizationClassification(OrganizationClassification.CUSTOMER);
        customer.setCreatedViaSignup(false);
        customer.setKycStatus(KycStatus.DRAFT);
        customer.setEntityStatus(EntityStatus.ACTIVE);
        if (deletedCustomer.isEmpty()) {
            customer.setCreatedAt(LocalDateTime.now());
            customer.setCreatedBy(username);
        }
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
    public OrganizationResponse findByMultipleFilters(
            OrganizationMultipleFiltersRequest request, String username, Locale locale) {
        ValidatorDto validation = organizationServiceValidator.validateFindByMultipleFilters(request, locale);
        if (!validation.getSuccess()) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }

        Specification<Organization> spec = buildOrganizationFilterSpec(request);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.ASC, "id"));
        Page<Organization> page = organizationRepository.findAll(spec, pageable);
        List<OrganizationDto> dtoList = page.getContent().stream().map(OrganizationMapping::toDto).toList();
        Page<OrganizationDto> dtoPage = new PageImpl<>(dtoList, pageable, page.getTotalElements());
        OrganizationResponse res = buildOrganizationResponse(null);
        res.setOrganizationDtoPage(dtoPage);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationDto> listOrganizationsForExport(
            OrganizationMultipleFiltersRequest request, String username, Locale locale) {
        ValidatorDto validation = organizationServiceValidator.validateFindByMultipleFilters(request, locale);
        if (!validation.getSuccess()) {
            throw new IllegalArgumentException(String.join("; ", validation.getErrorMessages()));
        }
        Specification<Organization> spec = buildOrganizationFilterSpec(request);
        return organizationRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(OrganizationMapping::toDto)
                .toList();
    }

    private Specification<Organization> buildOrganizationFilterSpec(OrganizationMultipleFiltersRequest request) {
        Specification<Organization> spec = OrganizationSpecifications.notDeleted();

        if (StringUtils.hasText(request.getName())) {
            spec = spec.and(OrganizationSpecifications.nameLike(request.getName()));
        }
        if (StringUtils.hasText(request.getEmail())) {
            spec = spec.and(OrganizationSpecifications.emailLike(request.getEmail()));
        }
        if (StringUtils.hasText(request.getOrganizationClassification())) {
            OrganizationClassification classification =
                    OrganizationClassification.valueOf(request.getOrganizationClassification().trim());
            spec = spec.and(OrganizationSpecifications.organizationClassificationEquals(classification));
        }
        if (request.getIndustryId() != null) {
            var industry = industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED);
            if (industry.isPresent()) {
                spec = spec.and(OrganizationSpecifications.industryEquals(industry.get()));
            }
        }
        if (StringUtils.hasText(request.getKycStatus())) {
            KycStatus kycStatus = KycStatus.valueOf(request.getKycStatus().trim());
            spec = spec.and(OrganizationSpecifications.kycStatusEquals(kycStatus));
        }
        if (Boolean.TRUE.equals(request.getKycQueueOnly())) {
            spec = spec.and(OrganizationSpecifications.signupOrganizationsOnly());
            if (!StringUtils.hasText(request.getKycStatus())) {
                spec = spec.and(OrganizationSpecifications.kycStatusIn(OrganizationSpecifications.SIGNUP_PIPELINE_QUEUE_STATUSES));
            }
        }
        if (Boolean.TRUE.equals(request.getOrganizationDirectoryOnly())) {
            spec = spec.and(OrganizationSpecifications.organizationDirectoryEligible());
        }
        if (StringUtils.hasText(request.getKycAssignedToUsername())) {
            spec = spec.and(OrganizationSpecifications.kycAssignedToReviewer(request.getKycAssignedToUsername()));
        }
        if (StringUtils.hasText(request.getSearchValue())) {
            spec = spec.and(OrganizationSpecifications.searchValueLike(request.getSearchValue()));
        }
        return spec;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getById(Long id, Locale locale) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        return buildOrganizationResponse(OrganizationMapping.toDto(org));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getByIdForSystem(Long id, Locale locale) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        OrganizationDto dto = OrganizationMapping.toDto(org);
        dto.setBranchDtoList(OrganizationMapping.toBranchDtos(
                branchRepository.findByOrganizationAndEntityStatusNot(org, EntityStatus.DELETED)));
        Specification<Agent> agentSpec = Specification.where(AgentSpecifications.notDeleted())
                .and(AgentSpecifications.organizationIdEquals(org.getId()));
        dto.setAgentDtoList(OrganizationMapping.toAgentDtos(agentRepository.findAll(agentSpec)));
        dto.setCustomerDtoList(mapNonDeletedOrganizations(org.getCustomers()));
        dto.setContractedTransporterDtoList(mapNonDeletedOrganizations(org.getContractedTransporters()));
        return buildOrganizationResponse(dto);
    }

    @Override
    public OrganizationResponse delete(Long id, Locale locale, String modifiedBy) {
        ValidatorDto validation = organizationServiceValidator.validateOrganizationId(id, locale);
        if (Boolean.FALSE.equals(validation.getSuccess())) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (org == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_NOT_FOUND.getCode(), new String[] {}, locale)));
        }
        org.setEntityStatus(EntityStatus.DELETED);
        org.setModifiedAt(LocalDateTime.now());
        org.setModifiedBy(modifiedBy);
        organizationServiceAuditable.save(org);
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.ORG_DELETED.getCode(), new String[] {}, locale));
        return res;
    }

    private List<OrganizationDto> mapNonDeletedOrganizations(List<Organization> linked) {
        if (linked == null || linked.isEmpty()) {
            return new ArrayList<>();
        }
        List<OrganizationDto> out = new ArrayList<>(linked.size());
        for (Organization o : linked) {
            if (o.getEntityStatus() != EntityStatus.DELETED) {
                out.add(OrganizationMapping.toDto(o));
            }
        }
        return out;
    }

    @Override
    public OrganizationResponse stage1Approve(Long id, KycActionRequest request, Locale locale, String username) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        assertAssignedKycApprover(org, KycReviewStage.STAGE_1, request, username, locale);
        KycStatus s = prepareOrganizationForStage1Decision(org, locale);
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
        afterCommit(() -> {
            organizationEventPublisher.publishStage1Approved(saved, reviewer, reviewedAt);
            organizationKycNotifier.sendStage1Approved(saved);
        });
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse stage1Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        Objects.requireNonNull(request.getRejectionReason(), "rejectionReason");
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        assertAssignedKycApprover(org, KycReviewStage.STAGE_1, request, username, locale);
        KycStatus s = prepareOrganizationForStage1Decision(org, locale);
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
        assertAssignedKycApprover(org, KycReviewStage.STAGE_2, request, username, locale);
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
            organizationKycNotifier.sendStage2Approved(saved);
        });
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse stage2Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        Objects.requireNonNull(request.getRejectionReason(), "rejectionReason");
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        assertAssignedKycApprover(org, KycReviewStage.STAGE_2, request, username, locale);
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
        if (Boolean.TRUE.equals(org.getCreatedViaSignup())) {
            kycApproverAssignmentService.assignApprovers(org);
        }
        Organization saved = organizationServiceAuditable.save(org);
        String allowedBy = resolveReviewer(request, username);
        afterCommit(() -> organizationEventPublisher.publishResubmitted(saved, allowedBy, now));
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse listIndustriesWithUsage(Locale locale) {
        List<Industry> industries = industryRepository.findByEntityStatusNotOrderByNameAsc(EntityStatus.DELETED);
        List<IndustryUsageDto> usageRows = new ArrayList<>();
        for (Industry industry : industries) {
            usageRows.add(enrichIndustryUsage(industry));
        }
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setIndustryUsageDtoList(usageRows);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse findIndustriesByMultipleFilters(IndustryMultipleFiltersRequest request, Locale locale) {
        ValidatorDto validation = organizationServiceValidator.validateFindIndustriesByMultipleFilters(request, locale);
        if (!validation.getSuccess()) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }
        Specification<Industry> spec = IndustrySpecifications.notDeleted();
        if (StringUtils.hasText(request.getName())) {
            spec = spec.and(IndustrySpecifications.nameLike(request.getName()));
        }
        if (StringUtils.hasText(request.getIndustryCode())) {
            spec = spec.and(IndustrySpecifications.industryCodeLike(request.getIndustryCode()));
        }
        if (StringUtils.hasText(request.getSearchValue())) {
            spec = spec.and(IndustrySpecifications.searchValueLike(request.getSearchValue()));
        }
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.ASC, "name"));
        Page<IndustryUsageDto> dtoPage = industryRepository.findAll(spec, pageable).map(this::enrichIndustryUsage);
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setIndustryUsageDtoPage(dtoPage);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getIndustryById(Long id, Locale locale) {
        ValidatorDto validation = organizationServiceValidator.validateIndustryId(id, locale);
        if (!validation.getSuccess()) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }
        Optional<Industry> industry = industryRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (industry.isEmpty()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.INDUSTRY_NOT_FOUND.getCode(), new String[] {}, locale)));
        }
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.INDUSTRY_RETRIEVED.getCode(), new String[] {}, locale));
        res.setIndustryDto(IndustryMapping.toDto(industry.get()));
        return res;
    }

    @Override
    public OrganizationResponse createIndustry(CreateIndustryRequest request, Locale locale, String username) {
        ValidatorDto validation = organizationServiceValidator.validateCreateIndustry(request, locale);
        if (!validation.getSuccess()) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }
        String normalizedName = request.getName().trim();
        if (industryRepository.findByNameIgnoreCaseAndEntityStatusNot(normalizedName, EntityStatus.DELETED).isPresent()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.INDUSTRY_NAME_EXISTS.getCode(), new String[] {}, locale)));
        }
        Industry industry = new Industry();
        IndustryMapping.applyCreate(request, industry);
        industry.setEntityStatus(EntityStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        industry.setCreatedAt(now);
        industry.setCreatedBy(username);
        Industry saved = industryServiceAuditable.save(industry);
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(201);
        res.setMessage(messageService.getMessage(I18Code.INDUSTRY_CREATED.getCode(), new String[] {}, locale));
        res.setIndustryDto(IndustryMapping.toDto(saved));
        return res;
    }

    @Override
    public OrganizationResponse updateIndustry(Long id, UpdateIndustryRequest request, Locale locale, String username) {
        ValidatorDto idValidation = organizationServiceValidator.validateIndustryId(id, locale);
        if (!idValidation.getSuccess()) {
            return buildOrganizationResponseWithErrors(idValidation.getErrorMessages());
        }
        ValidatorDto validation = organizationServiceValidator.validateUpdateIndustry(request, locale);
        if (!validation.getSuccess()) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }
        Industry industry = industryRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElse(null);
        if (industry == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.INDUSTRY_NOT_FOUND.getCode(), new String[] {}, locale)));
        }
        String normalizedName = request.getName().trim();
        if (industryRepository.existsByNameIgnoreCaseAndIdNotAndEntityStatusNot(normalizedName, id, EntityStatus.DELETED)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.INDUSTRY_NAME_EXISTS.getCode(), new String[] {}, locale)));
        }
        IndustryMapping.applyUpdate(request, industry);
        industry.setModifiedAt(LocalDateTime.now());
        industry.setModifiedBy(username);
        Industry saved = industryServiceAuditable.save(industry);
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.INDUSTRY_UPDATED.getCode(), new String[] {}, locale));
        res.setIndustryDto(IndustryMapping.toDto(saved));
        return res;
    }

    @Override
    public OrganizationResponse deleteIndustry(Long id, Locale locale, String username) {
        ValidatorDto validation = organizationServiceValidator.validateIndustryId(id, locale);
        if (!validation.getSuccess()) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }
        Industry industry = industryRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElse(null);
        if (industry == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.INDUSTRY_NOT_FOUND.getCode(), new String[] {}, locale)));
        }
        long linked = organizationRepository.countByIndustryAndEntityStatusNot(industry, EntityStatus.DELETED);
        if (linked > 0) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.INDUSTRY_IN_USE.getCode(), new String[] {}, locale)));
        }
        industry.setEntityStatus(EntityStatus.DELETED);
        industry.setModifiedAt(LocalDateTime.now());
        industry.setModifiedBy(username);
        industryServiceAuditable.save(industry);
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.INDUSTRY_DELETED.getCode(), new String[] {}, locale));
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse findBranchesByMultipleFilters(BranchMultipleFiltersRequest request, Locale locale) {
        ValidatorDto validation = organizationServiceValidator.validateFindBranchesByMultipleFilters(request, locale);
        if (!validation.getSuccess()) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }
        Specification<Branch> spec = BranchSpecifications.notDeleted();
        if (StringUtils.hasText(request.getBranchName())) {
            spec = spec.and(BranchSpecifications.branchNameLike(request.getBranchName()));
        }
        if (request.getOrganizationId() != null) {
            spec = spec.and(BranchSpecifications.organizationIdEquals(request.getOrganizationId()));
        }
        if (StringUtils.hasText(request.getSearchValue())) {
            spec = spec.and(BranchSpecifications.searchValueLike(request.getSearchValue()));
        }
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "branchName"));
        Page<BranchDto> dtoPage = branchRepository.findAll(spec, pageable).map(OrganizationMapping::toBranchDto);
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setBranchDtoPage(dtoPage);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse findAgentsByMultipleFilters(AgentMultipleFiltersRequest request, Locale locale) {
        ValidatorDto validation = organizationServiceValidator.validateFindAgentsByMultipleFilters(request, locale);
        if (!validation.getSuccess()) {
            return buildOrganizationResponseWithErrors(validation.getErrorMessages());
        }
        Specification<Agent> spec = AgentSpecifications.notDeleted();
        if (request.getOrganizationId() != null) {
            spec = spec.and(AgentSpecifications.organizationIdEquals(request.getOrganizationId()));
        }
        if (StringUtils.hasText(request.getAgentKind())) {
            spec = spec.and(AgentSpecifications.agentKindEquals(AgentKind.valueOf(request.getAgentKind().trim())));
        }
        if (StringUtils.hasText(request.getSearchValue())) {
            spec = spec.and(AgentSpecifications.searchValueLike(request.getSearchValue()));
        }
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
        Page<AgentDto> dtoPage = agentRepository.findAll(spec, pageable).map(OrganizationMapping::toAgentDto);
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setAgentDtoPage(dtoPage);
        return res;
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

    private IndustryUsageDto enrichIndustryUsage(Industry industry) {
        IndustryUsageDto row = IndustryMapping.toUsageDto(industry);
        row.setOrganizationCount(organizationRepository.countByIndustryAndEntityStatusNot(industry, EntityStatus.DELETED));
        row.setVerifiedOrganizationCount(
                organizationRepository.countByIndustryAndIsVerifiedTrueAndEntityStatusNot(industry, EntityStatus.DELETED));
        Pageable samplePage = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "name"));
        List<String> names = new ArrayList<>();
        for (Organization org : organizationRepository.findByIndustryAndEntityStatusNotOrderByNameAsc(
                industry, EntityStatus.DELETED, samplePage)) {
            names.add(org.getName());
        }
        row.setLinkedOrganizationNames(names);
        return row;
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

    private void clearApproverAssignments(Organization org) {
        org.setAssignedStage1ApproverUserId(null);
        org.setAssignedStage1ApproverUsername(null);
        org.setAssignedStage2ApproverUserId(null);
        org.setAssignedStage2ApproverUsername(null);
    }

    /**
     * Signup organisations may sit in {@link KycStatus#DRAFT} in the admin queue before the applicant
     * calls submit-kyc. Stage-1 approve/reject advances them through SUBMITTED into stage-1 review.
     */
    private KycStatus prepareOrganizationForStage1Decision(Organization org, Locale locale) {
        KycStatus status = org.getKycStatus();
        LocalDateTime now = LocalDateTime.now();
        if (status == KycStatus.DRAFT) {
            kycStateMachine.assertCanTransition(KycStatus.DRAFT, KycStatus.SUBMITTED, locale);
            org.setKycStatus(KycStatus.SUBMITTED);
            if (org.getSubmittedAt() == null) {
                org.setSubmittedAt(now);
            }
            status = KycStatus.SUBMITTED;
        } else if (status == KycStatus.RESUBMITTED) {
            kycStateMachine.assertCanTransition(KycStatus.RESUBMITTED, KycStatus.SUBMITTED, locale);
            org.setKycStatus(KycStatus.SUBMITTED);
            org.setSubmittedAt(now);
            status = KycStatus.SUBMITTED;
        }
        if (status == KycStatus.SUBMITTED) {
            kycStateMachine.assertCanTransition(KycStatus.SUBMITTED, KycStatus.STAGE_1_REVIEW, locale);
            org.setKycStatus(KycStatus.STAGE_1_REVIEW);
            return KycStatus.STAGE_1_REVIEW;
        }
        if (status == KycStatus.STAGE_1_REVIEW) {
            return status;
        }
        return status;
    }

    private void assertAssignedKycApprover(
            Organization org,
            KycReviewStage stage,
            KycActionRequest request,
            String actingUsername,
            Locale locale) {
        if (!Boolean.TRUE.equals(org.getCreatedViaSignup())) {
            return;
        }
        String principal = resolveReviewer(request, actingUsername);
        if (principal == null || principal.isBlank()) {
            return;
        }
        String normalized = principal.trim().toUpperCase();
        if ("ADMIN".equals(normalized) || "SYSTEM".equals(normalized)) {
            return;
        }
        Long assignedUserId = stage == KycReviewStage.STAGE_1
                ? org.getAssignedStage1ApproverUserId()
                : org.getAssignedStage2ApproverUserId();
        String assignedUsername = stage == KycReviewStage.STAGE_1
                ? org.getAssignedStage1ApproverUsername()
                : org.getAssignedStage2ApproverUsername();
        if (assignedUserId == null && (assignedUsername == null || assignedUsername.isBlank())) {
            return;
        }
        Long actingUserId = request != null ? request.getReviewerUserId() : null;
        if (assignedUserId != null && actingUserId != null && actingUserId.equals(assignedUserId)) {
            assertNotCrossStageApprover(org, stage, actingUserId, principal, locale);
            return;
        }
        if (assignedUsername != null && principal.equalsIgnoreCase(assignedUsername.trim())) {
            assertNotCrossStageApprover(org, stage, actingUserId, principal, locale);
            return;
        }
        Long resolvedId = resolveUserIdByUsername(principal);
        if (resolvedId != null && resolvedId.equals(assignedUserId)) {
            assertNotCrossStageApprover(org, stage, resolvedId, principal, locale);
            return;
        }
        throw new BusinessRuleException(
                messageService.getMessage(I18Code.KYC_NOT_ASSIGNED_APPROVER.getCode(), new String[]{}, locale),
                I18Code.KYC_NOT_ASSIGNED_APPROVER);
    }

    private void assertNotCrossStageApprover(
            Organization org,
            KycReviewStage stage,
            Long actingUserId,
            String actingUsername,
            Locale locale) {
        if (stage != KycReviewStage.STAGE_2) {
            return;
        }
        Long stage1UserId = org.getAssignedStage1ApproverUserId();
        if (stage1UserId != null && actingUserId != null && stage1UserId.equals(actingUserId)) {
            throw sameApproverException(locale);
        }
        String stage1Username = org.getAssignedStage1ApproverUsername();
        if (stage1Username != null && actingUsername != null
                && stage1Username.equalsIgnoreCase(actingUsername.trim())) {
            throw sameApproverException(locale);
        }
    }

    private BusinessRuleException sameApproverException(Locale locale) {
        return new BusinessRuleException(
                messageService.getMessage(I18Code.KYC_SAME_APPROVER.getCode(), new String[]{}, locale),
                I18Code.KYC_SAME_APPROVER);
    }

    private Long resolveUserIdByUsername(String username) {
        try {
            UserResponse response = userManagementServiceClient.findByUsername(username);
            if (response != null && response.isSuccess() && response.getUserDto() != null) {
                return response.getUserDto().getId();
            }
        } catch (Exception ignored) {
            // Feign unavailable — username match already attempted
        }
        return null;
    }

    private void applyUpdateScalars(Organization org, UpdateOrganizationRequest request, boolean identityLocked) {
        if (!identityLocked && request.getName() != null) {
            org.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            org.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getLocationId() != null) {
            org.setLocationId(request.getLocationId());
        }
        if (!identityLocked && request.getOrganizationType() != null) {
            org.setOrganizationType(request.getOrganizationType());
        }
        if (!identityLocked && request.getIndustryId() != null) {
            industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED)
                    .ifPresent(org::setIndustry);
        }
        if (!identityLocked) {
            if (request.getContactPersonFirstName() != null) {
                org.setContactPersonFirstName(request.getContactPersonFirstName());
            }
            if (request.getContactPersonLastName() != null) {
                org.setContactPersonLastName(request.getContactPersonLastName());
            }
            if (request.getContactPersonEmail() != null) {
                org.setContactPersonEmail(request.getContactPersonEmail());
            }
            if (request.getContactPersonPhoneNumber() != null) {
                org.setContactPersonPhoneNumber(request.getContactPersonPhoneNumber());
            }
            if (request.getContactPersonPosition() != null) {
                org.setContactPersonPosition(request.getContactPersonPosition());
            }
            if (request.getContactPersonGender() != null) {
                org.setContactPersonGender(request.getContactPersonGender());
            }
            if (request.getContactPersonNationalIdNumber() != null) {
                org.setContactPersonNationalIdNumber(request.getContactPersonNationalIdNumber());
            }
            if (request.getContactPersonPassportNumber() != null) {
                org.setContactPersonPassportNumber(request.getContactPersonPassportNumber());
            }
            if (request.getContactPersonDateOfBirth() != null) {
                org.setContactPersonDateOfBirth(request.getContactPersonDateOfBirth());
            }
            if (request.getRegistrationNumber() != null) {
                org.setRegistrationNumber(request.getRegistrationNumber().trim());
            }
            if (request.getTaxNumber() != null) {
                org.setTaxNumber(request.getTaxNumber().trim());
            }
            if (request.getRepresentativeNationalIdNumber() != null) {
                org.setRepresentativeNationalIdNumber(request.getRepresentativeNationalIdNumber());
            }
            if (request.getRepresentativePassportNumber() != null) {
                org.setRepresentativePassportNumber(request.getRepresentativePassportNumber());
            }
        }
        if (request.getWebsiteUrl() != null) {
            org.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getOrganizationDescription() != null) {
            org.setOrganizationDescription(request.getOrganizationDescription());
        }
        if (request.getIncorporationDate() != null) {
            org.setIncorporationDate(request.getIncorporationDate());
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
        if (request.getSubscriptionPlanId() != null) {
            org.setSubscriptionPlanId(request.getSubscriptionPlanId());
        }
        if (request.getDataProtectionOfficerContact() != null) {
            org.setDataProtectionOfficerContact(request.getDataProtectionOfficerContact());
        }
        if (request.getTwoFactorAuthenticationEnabled() != null) {
            org.setTwoFactorAuthenticationEnabled(request.getTwoFactorAuthenticationEnabled());
        }
        if (request.getLinkedInUrl() != null) {
            org.setLinkedInUrl(request.getLinkedInUrl());
        }
        if (request.getFacebookUrl() != null) {
            org.setFacebookUrl(request.getFacebookUrl());
        }
        if (request.getTwitterUrl() != null) {
            org.setTwitterUrl(request.getTwitterUrl());
        }
        if (request.getInstagramUrl() != null) {
            org.setInstagramUrl(request.getInstagramUrl());
        }
        if (request.getYoutubeUrl() != null) {
            org.setYoutubeUrl(request.getYoutubeUrl());
        }
        if (request.getLatitude() != null) {
            org.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            org.setLongitude(request.getLongitude());
        }
        if (request.getAssignedAccountManagerUserId() != null) {
            org.setAssignedAccountManagerUserId(request.getAssignedAccountManagerUserId());
        }
    }

    private List<UploadPart> buildUpdateUploadParts(UpdateOrganizationRequest request, Organization org, boolean identityLocked) {
        List<UploadPart> parts = new ArrayList<>();
        parts.add(uploadPart(request.getLogoUpload(), request.getLogoUploadId(), FileType.ORGANIZATION_LOGO, org, org::setLogoUploadId));
        if (!identityLocked) {
            parts.add(uploadPart(request.getRegistrationCertificateUpload(), request.getRegistrationCertificateUploadId(),
                    FileType.COMPANY_REGISTRATION_CERTIFICATE, org, org::setRegistrationCertificateUploadId));
            parts.add(uploadPart(request.getTaxClearanceCertificateUpload(), request.getTaxClearanceCertificateUploadId(),
                    FileType.TAX_CLEARANCE_CERTIFICATE, org, org::setTaxClearanceCertificateUploadId));
            parts.add(uploadPart(request.getContactPersonNationalIdUpload(), request.getContactPersonNationalIdUploadId(),
                    FileType.NATIONAL_ID, org, org::setContactPersonNationalIdUploadId));
            parts.add(uploadPart(request.getContactPersonPassportUpload(), request.getContactPersonPassportUploadId(),
                    FileType.PASSPORT, org, org::setContactPersonPassportUploadId));
        }
        parts.add(uploadPart(request.getBusinessLicenseUpload(), request.getBusinessLicenseUploadId(),
                FileType.BUSINESS_LICENSE, org, org::setBusinessLicenseUploadId));
        parts.add(uploadPart(request.getProofOfAddressUpload(), request.getProofOfAddressUploadId(),
                FileType.PROOF_OF_ADDRESS, org, org::setProofOfAddressUploadId));
        parts.add(uploadPart(request.getIndustrySpecificLicenseUpload(), request.getIndustrySpecificLicenseUploadId(),
                FileType.INDUSTRY_SPECIFIC_LICENSE, org, org::setIndustrySpecificLicenseUploadId));
        return parts;
    }

    private boolean isContactIdentityLocked(Organization org) {
        if (org.getContactPersonUserId() != null && org.getContactPersonUserId() > 0) {
            return true;
        }
        if (Boolean.TRUE.equals(org.getCreatedViaSignup())) {
            KycStatus ks = org.getKycStatus();
            return ks != KycStatus.DRAFT && ks != KycStatus.RESUBMITTED;
        }
        return false;
    }

    private boolean isTrustedAdminModifier(String modifiedBy) {
        return modifiedBy != null && TRUSTED_ADMIN_MODIFIERS.contains(modifiedBy.trim());
    }

    private UploadPart uploadPart(
            MultipartFile multipart,
            Long preassignedId,
            FileType fileType,
            Organization org,
            LongConsumer assigner) {
        return new UploadPart(
                multipart,
                preassignedId,
                fileType,
                organizationFileUploadHelper.existingId(org, fileType),
                assigner);
    }

    private UploadPart contactUploadPart(
            MultipartFile multipart,
            Long preassignedId,
            FileType fileType,
            Organization org,
            LongConsumer assigner,
            String expiresAt) {
        return new UploadPart(
                multipart,
                preassignedId,
                fileType,
                organizationFileUploadHelper.existingId(org, fileType),
                assigner,
                expiresAt);
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

    @Override
    public OrganizationResponse createBranch(CreateBranchRequest request, Locale locale, String username) {
        return organizationDirectoryAdminService.createBranch(request, locale, username);
    }

    @Override
    public OrganizationResponse updateBranch(Long id, UpdateBranchRequest request, Locale locale, String username) {
        return organizationDirectoryAdminService.updateBranch(id, request, locale, username);
    }

    @Override
    public OrganizationResponse getBranchById(Long id, Locale locale) {
        return organizationDirectoryAdminService.getBranchById(id, locale);
    }

    @Override
    public OrganizationResponse deleteBranch(Long id, Locale locale, String username) {
        return organizationDirectoryAdminService.deleteBranch(id, locale, username);
    }

    @Override
    public OrganizationResponse createAgent(CreateAgentRequest request, Locale locale, String username) {
        return organizationDirectoryAdminService.createAgent(request, locale, username);
    }

    @Override
    public OrganizationResponse updateAgent(Long id, UpdateAgentRequest request, Locale locale, String username) {
        return organizationDirectoryAdminService.updateAgent(id, request, locale, username);
    }

    @Override
    public OrganizationResponse getAgentById(Long id, Locale locale) {
        return organizationDirectoryAdminService.getAgentById(id, locale);
    }

    @Override
    public OrganizationResponse deleteAgent(Long id, Locale locale, String username) {
        return organizationDirectoryAdminService.deleteAgent(id, locale, username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDto> listBranchesForExport(BranchMultipleFiltersRequest request, Locale locale) {
        return organizationDirectoryAdminService.listBranchesForExport(request, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentDto> listAgentsForExport(AgentMultipleFiltersRequest request, Locale locale) {
        return organizationDirectoryAdminService.listAgentsForExport(request, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndustryDto> listIndustriesForExport(IndustryMultipleFiltersRequest request, Locale locale) {
        return organizationDirectoryAdminService.listIndustriesForExport(request, locale);
    }

    @Override
    public ImportSummary importBranchesFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        return organizationDirectoryAdminService.importBranchesFromCsv(inputStream, locale, username);
    }

    @Override
    public ImportSummary importAgentsFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        return organizationDirectoryAdminService.importAgentsFromCsv(inputStream, locale, username);
    }

    @Override
    public ImportSummary importIndustriesFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        return organizationDirectoryAdminService.importIndustriesFromCsv(inputStream, locale, username);
    }

    /**
     * Platform self-service may edit only while KYC is draft/resubmitted.
     * Admin/system/backoffice updates and admin-registered organisations are exempt.
     */
    private boolean canUpdateOrganizationProfile(Organization org, String modifiedBy) {
        KycStatus ks = org.getKycStatus();
        if (ks == KycStatus.DRAFT || ks == KycStatus.RESUBMITTED) {
            return true;
        }
        if (modifiedBy != null && TRUSTED_ADMIN_MODIFIERS.contains(modifiedBy.trim())) {
            return true;
        }
        return !Boolean.TRUE.equals(org.getCreatedViaSignup());
    }
}
