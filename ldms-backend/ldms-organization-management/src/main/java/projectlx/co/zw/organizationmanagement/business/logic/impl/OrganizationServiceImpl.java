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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import projectlx.co.zw.organizationmanagement.business.auditable.api.BranchServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.IndustryServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationKycReviewServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.kyc.KycApproverAssignmentService;
import projectlx.co.zw.organizationmanagement.business.kyc.KycApprovalStageResolver;
import projectlx.co.zw.organizationmanagement.business.kyc.KycStageSupport;
import projectlx.co.zw.organizationmanagement.business.kyc.KycStateMachine;
import projectlx.co.zw.organizationmanagement.business.kyc.OrganizationEventPublisher;
import projectlx.co.zw.organizationmanagement.clients.UserManagementServiceClient;
import projectlx.co.zw.organizationmanagement.business.logic.api.OrganizationService;
import projectlx.co.zw.organizationmanagement.business.logic.support.BranchHierarchySupport;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationDirectoryAdminService;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationTradingCapabilitySupport;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationDirectoryNotifier;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationFleetNotifier;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationFileUploadHelper;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationApprovedCredentialsSupport;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationContactPersonProvisioningSupport;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationKycNotifier;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationRegistrationAddressSupport;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationRegistrationNotifier;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationSupplierRegisteredEvent;
import projectlx.co.zw.organizationmanagement.business.logic.support.SupplierRegisteredOnboardingResult;
import projectlx.co.zw.organizationmanagement.business.logic.support.SupplierRegisteredOrganizationOnboardingSupport;
import projectlx.co.zw.shared_library.business.logic.impl.TokenService;
import projectlx.co.zw.shared_library.utils.generators.SecureTokenGenerator;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationFileUploadHelper.UploadOutcome;
import projectlx.co.zw.organizationmanagement.business.logic.support.OrganizationFileUploadHelper.UploadPart;
import projectlx.co.zw.organizationmanagement.business.validation.api.OrganizationServiceValidator;
import projectlx.co.zw.organizationmanagement.model.ContractedTransporterLink;
import projectlx.co.zw.organizationmanagement.model.FleetVehicle;
import projectlx.co.zw.organizationmanagement.model.Agent;
import projectlx.co.zw.organizationmanagement.model.AgentKind;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.organizationmanagement.model.KycDecision;
import projectlx.co.zw.organizationmanagement.model.KycReviewStage;
import projectlx.co.zw.organizationmanagement.model.KycStatus;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;
import projectlx.co.zw.organizationmanagement.model.OrganizationType;
import projectlx.co.zw.organizationmanagement.model.OrganizationKycReview;
import projectlx.co.zw.organizationmanagement.model.PlatformKycPolicy;
import projectlx.co.zw.organizationmanagement.repository.ContractedTransporterLinkRepository;
import projectlx.co.zw.organizationmanagement.repository.FleetVehicleRepository;
import projectlx.co.zw.organizationmanagement.repository.AgentRepository;
import projectlx.co.zw.organizationmanagement.repository.BranchRepository;
import projectlx.co.zw.organizationmanagement.repository.IndustryRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationKycReviewRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;
import projectlx.co.zw.organizationmanagement.repository.PlatformKycPolicyRepository;
import projectlx.co.zw.organizationmanagement.repository.specification.AgentSpecifications;
import projectlx.co.zw.organizationmanagement.repository.specification.BranchSpecifications;
import projectlx.co.zw.organizationmanagement.repository.specification.IndustrySpecifications;
import projectlx.co.zw.organizationmanagement.repository.specification.OrganizationSpecifications;
import projectlx.co.zw.organizationmanagement.utils.dtos.OnboardingStatusDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.FleetVehicleDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryMapping;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryUsageDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.KycApprovalPolicyDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationKycReviewDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationMapping;
import projectlx.co.zw.organizationmanagement.utils.enums.BranchLevel;
import projectlx.co.zw.organizationmanagement.utils.enums.FleetVehicleOwnershipType;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.utils.exceptions.BusinessRuleException;
import projectlx.co.zw.organizationmanagement.utils.dtos.ImportSummary;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateFleetVehicleRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.FleetRegisteredNotificationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.ValidateFleetOwnershipRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.ValidateTransporterAssignmentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.EditFleetVehicleRequest;
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
import projectlx.co.zw.organizationmanagement.utils.requests.LinkClearingAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkCustomerRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationKycStagesRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateKycApprovalPolicyRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;
import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.organizationmanagement.clients.UsersMultipleFiltersFeignRequest;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
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
    private final ContractedTransporterLinkRepository contractedTransporterLinkRepository;
    private final FleetVehicleRepository fleetVehicleRepository;
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
    private final KycApprovalStageResolver kycApprovalStageResolver;
    private final PlatformKycPolicyRepository platformKycPolicyRepository;
    private final UserManagementServiceClient userManagementServiceClient;
    private final MessageService messageService;
    private final OrganizationFileUploadHelper organizationFileUploadHelper;
    private final OrganizationDirectoryAdminService organizationDirectoryAdminService;
    private final BranchHierarchySupport branchHierarchySupport;
    private final OrganizationRegistrationNotifier organizationRegistrationNotifier;
    private final OrganizationContactPersonProvisioningSupport organizationContactPersonProvisioningSupport;
    private final OrganizationKycNotifier organizationKycNotifier;
    private final OrganizationApprovedCredentialsSupport organizationApprovedCredentialsSupport;
    private final OrganizationDirectoryNotifier organizationDirectoryNotifier;
    private final OrganizationFleetNotifier organizationFleetNotifier;
    private final OrganizationRegistrationAddressSupport organizationRegistrationAddressSupport;
    private final SupplierRegisteredOrganizationOnboardingSupport supplierRegisteredOrganizationOnboardingSupport;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TokenService tokenService;

    @Override
    public OrganizationResponse register(RegisterOrganizationRequest request, Locale locale, String createdBy) {
        ValidatorDto v = organizationServiceValidator.validateRegister(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<Organization> existingByEmail = organizationRepository
                .findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED);
        if (existingByEmail.isPresent()) {
            if (isSignupResubmissionRegistration(existingByEmail.get())) {
                return applySignupResubmissionRegistration(existingByEmail.get(), request, locale, createdBy);
            }
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
        org.setDuplexMode(Boolean.TRUE.equals(request.getDuplexMode()));
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
        OrganizationResponse locationResponse = applyRegistrationLocationId(org, request, locale);
        if (locationResponse != null) {
            return locationResponse;
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
    @Transactional(readOnly = true)
    public OrganizationResponse getOnboardingStatus(Long organizationId, Locale locale) {
        if (organizationId == null || organizationId < 1) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                            new String[] { "organizationId" }, locale)));
        }
        Organization org = organizationRepository.findByIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        OnboardingStatusDto dto = new OnboardingStatusDto();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setKycStatus(org.getKycStatus() != null ? org.getKycStatus().name() : KycStatus.DRAFT.name());
        dto.setVerified(org.isVerified());
        dto.setRequiredApprovalStages(kycApprovalStageResolver.resolveRequiredStages(org));
        dto.setLastRejectionReason(org.getLastRejectionReason());
        dto.setSubmittedAt(org.getSubmittedAt());
        dto.setModifiedAt(org.getModifiedAt());
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setOnboardingStatusDto(dto);
        return res;
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
        afterCommit(() -> {
            organizationEventPublisher.publishSubmitted(snapshot);
            organizationKycNotifier.sendKycSubmitted(snapshot);
        });
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
        organizationRegistrationAddressSupport.enrichOrganizationAddress(dto, locale);
        java.util.Map<Long, String> orgRegionCache = new java.util.HashMap<>();
        List<BranchDto> branchDtos = branchRepository.findByOrganizationAndEntityStatusNot(org, EntityStatus.DELETED)
                .stream()
                .map(branch -> {
                    BranchDto branchDto = OrganizationMapping.toBranchDto(branch);
                    organizationRegistrationAddressSupport.enrichHeadOfficeBranchRegion(
                            branch, branchDto, locale, orgRegionCache);
                    return branchDto;
                })
                .toList();
        dto.setBranchDtoList(branchDtos);
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
        Optional<String> hierarchyError = branchHierarchySupport.validateHierarchyForCreate(
                org, request.getParentBranchId(), locale);
        if (hierarchyError.isPresent()) {
            return buildOrganizationResponseWithErrors(List.of(hierarchyError.get()));
        }
        Branch b = new Branch();
        b.setOrganization(org);
        branchHierarchySupport.applyHierarchyOnCreate(b, org, request.getParentBranchId(), request.getDepot());
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
        Branch saved = branchServiceAuditable.save(b);
        organizationDirectoryNotifier.sendBranchCreated(saved, username);
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
    public OrganizationResponse getBranchByIdForUser(Long branchId, Locale locale, String username) {
        OrganizationResponse denied = assertBranchOwnedByUser(branchId, username, locale);
        if (denied != null) {
            return denied;
        }
        return getBranchById(branchId, locale);
    }

    @Override
    public OrganizationResponse updateBranchForUser(
            Long branchId, UpdateBranchRequest request, Locale locale, String username) {
        OrganizationResponse denied = assertBranchOwnedByUser(branchId, username, locale);
        if (denied != null) {
            return denied;
        }
        Organization org = loadForUser(username);
        request.setOrganizationId(org.getId());
        OrganizationResponse response = updateBranch(branchId, request, locale, username);
        if (!response.isSuccess()) {
            return response;
        }
        return listBranches(locale, username);
    }

    @Override
    public OrganizationResponse deleteBranchForUser(Long branchId, Locale locale, String username) {
        OrganizationResponse denied = assertBranchOwnedByUser(branchId, username, locale);
        if (denied != null) {
            return denied;
        }
        OrganizationResponse response = deleteBranch(branchId, locale, username);
        if (!response.isSuccess()) {
            return response;
        }
        return listBranches(locale, username);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse findBranchesByMultipleFiltersForUser(
            BranchMultipleFiltersRequest request, Locale locale, String username) {
        Organization org = loadForUser(username);
        request.setOrganizationId(org.getId());
        return findBranchesByMultipleFilters(request, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDto> listBranchesForExportForUser(
            BranchMultipleFiltersRequest request, Locale locale, String username) {
        Organization org = loadForUser(username);
        request.setOrganizationId(org.getId());
        return listBranchesForExport(request, locale);
    }

    @Override
    public ImportSummary importBranchesFromCsvForUser(InputStream inputStream, Locale locale, String username)
            throws IOException {
        Organization org = loadForUser(username);
        return organizationDirectoryAdminService.importBranchesFromCsvForOrganization(
                inputStream, org.getId(), locale, username);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse listAgents(Locale locale, String username) {
        Organization org = loadForUser(username);
        OrganizationResponse res = buildOrganizationResponse(null);
        OrganizationDto dto = OrganizationMapping.toDto(org);
        Specification<Agent> agentSpec = Specification.where(AgentSpecifications.notDeleted())
                .and(AgentSpecifications.organizationIdEquals(org.getId()));
        dto.setAgentDtoList(OrganizationMapping.toAgentDtos(agentRepository.findAll(agentSpec)));
        res.setOrganizationDto(dto);
        return res;
    }

    @Override
    public OrganizationResponse createAgentForUser(CreateAgentRequest request, Locale locale, String username) {
        Organization org = loadForUser(username);
        request.setOrganizationId(org.getId());
        return createAgent(request, locale, username);
    }

    @Override
    public OrganizationResponse updateAgentForUser(Long agentId, UpdateAgentRequest request, Locale locale, String username) {
        OrganizationResponse denied = assertAgentOwnedByUser(agentId, username, locale);
        if (denied != null) {
            return denied;
        }
        request.setOrganizationId(null);
        return updateAgent(agentId, request, locale, username);
    }

    @Override
    public OrganizationResponse deleteAgentForUser(Long agentId, Locale locale, String username) {
        OrganizationResponse denied = assertAgentOwnedByUser(agentId, username, locale);
        if (denied != null) {
            return denied;
        }
        return deleteAgent(agentId, locale, username);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse listCustomers(Locale locale, String username) {
        Organization org = loadForUser(username);
        if (!OrganizationTradingCapabilitySupport.canActAsSupplier(org)) {
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
    @Transactional(readOnly = true)
    public OrganizationResponse listTransporters(Locale locale, String username) {
        Organization org = loadForUser(username);
        List<OrganizationDto> dtos = new ArrayList<>();
        if (OrganizationTradingCapabilitySupport.canContractTransporters(org)) {
            List<ContractedTransporterLink> links = contractedTransporterLinkRepository
                    .findByOrganizationIdAndEntityStatusNotOrderByLinkedAtDesc(org.getId(), EntityStatus.DELETED);
            for (ContractedTransporterLink link : links) {
                Organization transporter = link.getTransporter();
                if (transporter != null && transporter.getEntityStatus() != EntityStatus.DELETED) {
                    OrganizationDto dto = OrganizationMapping.toDto(transporter);
                    applyTransporterContractMetadata(dto, link);
                    dtos.add(dto);
                }
            }
        } else if (org.getOrganizationClassification() == OrganizationClassification.TRANSPORT_COMPANY) {
            List<ContractedTransporterLink> links = contractedTransporterLinkRepository
                    .findByTransporterIdAndEntityStatusNotOrderByLinkedAtDesc(org.getId(), EntityStatus.DELETED);
            for (ContractedTransporterLink link : links) {
                Organization shipper = link.getOrganization();
                if (shipper != null && shipper.getEntityStatus() != EntityStatus.DELETED) {
                    OrganizationDto dto = OrganizationMapping.toDto(shipper);
                    applyTransporterContractMetadata(dto, link);
                    dtos.add(dto);
                }
            }
        }
        OrganizationResponse res = buildOrganizationResponse(null);
        res.setOrganizationDtoList(dtos);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse searchTransportCompanyCandidates(String search, Locale locale, String username) {
        Organization caller = loadForUser(username);
        if (!OrganizationTradingCapabilitySupport.canContractTransporters(caller)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_TRANSPORTER_LINK.getCode(), new String[]{}, locale)));
        }
        java.util.Set<Long> exclude = new java.util.HashSet<>();
        exclude.add(caller.getId());
        for (Organization linked : organizationRepository.findContractedTransportersForSupplier(
                caller.getId(), EntityStatus.DELETED)) {
            if (linked.getId() != null) {
                exclude.add(linked.getId());
            }
        }
        Specification<Organization> spec = Specification.where(OrganizationSpecifications.notDeleted())
                .and(OrganizationSpecifications.organizationClassificationEquals(OrganizationClassification.TRANSPORT_COMPANY))
                .and(OrganizationSpecifications.organizationDirectoryEligible());
        if (StringUtils.hasText(search)) {
            spec = spec.and(OrganizationSpecifications.searchValueLike(search));
        }
        List<Organization> found = organizationRepository.findAll(
                spec, PageRequest.of(0, 80, Sort.by(Sort.Direction.ASC, "name"))).getContent();
        List<OrganizationDto> dtos = new ArrayList<>();
        for (Organization candidate : found) {
            if (candidate.getId() != null && !exclude.contains(candidate.getId())) {
                dtos.add(OrganizationMapping.toDto(candidate));
            }
        }
        OrganizationResponse res = buildOrganizationResponse(null);
        res.setOrganizationDtoList(dtos);
        return res;
    }

    @Override
    public OrganizationResponse registerCustomer(RegisterOrganizationRequest request, Locale locale, String username) {
        request.setOrganizationClassification(OrganizationClassification.CUSTOMER);
        if (request.getOrganizationType() == null) {
            request.setOrganizationType(OrganizationType.PRIVATE);
        }
        request.setCreatedViaSignup(false);

        ValidatorDto v = organizationServiceValidator.validateRegister(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization supplier = loadForUser(username);
        if (!OrganizationTradingCapabilitySupport.canActAsSupplier(supplier)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_CUSTOMERS.getCode(), new String[]{}, locale)));
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<Organization> existingActive = organizationRepository
                .findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED);
        if (existingActive.isPresent()) {
            return buildCustomerRegistrationConflictResponse(existingActive.get(), supplier, locale);
        }
        OrganizationResponse contactEmailConflict = validateSupplierRegisteredContactEmail(request, locale, null, null);
        if (contactEmailConflict != null) {
            return contactEmailConflict;
        }
        Optional<Organization> deletedCustomer = organizationRepository.findByEmail(normalizedEmail)
                .filter(organization -> organization.getEntityStatus() == EntityStatus.DELETED);
        Organization customer = deletedCustomer.orElseGet(Organization::new);
        customer.setName(request.getName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setOrganizationClassification(OrganizationClassification.CUSTOMER);
        if (request.getOrganizationType() != null) {
            customer.setOrganizationType(request.getOrganizationType());
        }
        if (request.getIndustryId() != null) {
            industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED)
                    .ifPresent(customer::setIndustry);
        }
        customer.setContactPersonFirstName(request.getContactPersonFirstName());
        customer.setContactPersonLastName(request.getContactPersonLastName());
        customer.setContactPersonEmail(StringUtils.hasText(request.getContactPersonEmail())
                ? request.getContactPersonEmail().trim().toLowerCase()
                : null);
        customer.setContactPersonPhoneNumber(request.getContactPersonPhoneNumber());
        if (request.getContactPersonGender() != null) {
            customer.setContactPersonGender(request.getContactPersonGender());
        }
        if (StringUtils.hasText(request.getContactPersonDateOfBirth())) {
            customer.setContactPersonDateOfBirth(request.getContactPersonDateOfBirth().trim());
        }
        if (StringUtils.hasText(request.getContactPersonNationalIdNumber())) {
            customer.setContactPersonNationalIdNumber(request.getContactPersonNationalIdNumber().trim());
        }
        if (StringUtils.hasText(request.getContactPersonPassportNumber())) {
            customer.setContactPersonPassportNumber(request.getContactPersonPassportNumber().trim());
        }
        if (request.getRegistrationNumber() != null) {
            customer.setRegistrationNumber(request.getRegistrationNumber().trim());
        }
        if (request.getTaxNumber() != null) {
            customer.setTaxNumber(request.getTaxNumber().trim());
        }
        OrganizationResponse locationResponse = applyRegistrationLocationId(customer, request, locale);
        if (locationResponse != null) {
            return locationResponse;
        }
        customer.setCreatedViaSignup(false);
        // Supplier-registered buyers skip platform KYC; organisation email must still be verified online.
        customer.setKycStatus(KycStatus.APPROVED);
        customer.setVerified(false);
        customer.setEmailVerificationToken(null);
        clearApproverAssignments(customer);
        customer.setEntityStatus(EntityStatus.ACTIVE);
        if (deletedCustomer.isEmpty()) {
            customer.setCreatedAt(LocalDateTime.now());
            customer.setCreatedBy(username);
        }
        customer.setCurrentResubmissionCycle(0);
        customer.setResubmissionCount(0);
        organizationServiceAuditable.save(customer);

        UploadOutcome uploadOutcome = organizationFileUploadHelper.processUploads(
                customer,
                List.of(new UploadPart(
                        request.getTaxClearanceCertificateUpload(),
                        request.getTaxClearanceCertificateUploadId(),
                        FileType.TAX_CLEARANCE_CERTIFICATE,
                        null,
                        customer::setTaxClearanceCertificateUploadId)),
                locale,
                false);
        if (!uploadOutcome.success()) {
            return buildOrganizationResponseWithErrors(uploadOutcome.errorMessages());
        }

        UploadOutcome contactIdOutcome = organizationFileUploadHelper.processUploads(
                customer,
                List.of(
                        contactUploadPart(
                                request.getContactPersonNationalIdUpload(),
                                request.getContactPersonNationalIdUploadId(),
                                FileType.NATIONAL_ID,
                                customer,
                                customer::setContactPersonNationalIdUploadId,
                                request.getContactPersonNationalIdExpiryDate()),
                        contactUploadPart(
                                request.getContactPersonPassportUpload(),
                                request.getContactPersonPassportUploadId(),
                                FileType.PASSPORT,
                                customer,
                                customer::setContactPersonPassportUploadId,
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
            organizationServiceAuditable.save(customer);
        }

        Organization savedCustomer = customer;
        supplier.getCustomers().add(savedCustomer);
        savedCustomer.getSuppliers().add(supplier);
        organizationServiceAuditable.save(supplier);

        final Long registeredOrgId = savedCustomer.getId();
        applicationEventPublisher.publishEvent(new OrganizationSupplierRegisteredEvent(registeredOrgId));
        return buildOrganizationResponse(OrganizationMapping.toDto(savedCustomer));
    }

    @Override
    public OrganizationResponse registerTransporter(RegisterOrganizationRequest request, Locale locale, String username) {
        request.setOrganizationClassification(OrganizationClassification.TRANSPORT_COMPANY);
        if (request.getOrganizationType() == null) {
            request.setOrganizationType(OrganizationType.PRIVATE);
        }
        request.setCreatedViaSignup(false);

        ValidatorDto v = organizationServiceValidator.validateRegister(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization supplier = loadForUser(username);
        if (!OrganizationTradingCapabilitySupport.canActAsSupplier(supplier)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_TRANSPORTER_LINK.getCode(), new String[]{}, locale)));
        }
        ValidatorDto contractValidation = organizationServiceValidator.validateTransporterContract(
                request.getContractStartDate(), request.getContractEndDate(), locale);
        if (Boolean.FALSE.equals(contractValidation.getSuccess())) {
            return buildOrganizationResponseWithErrors(contractValidation.getErrorMessages());
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (organizationRepository.findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED).isPresent()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_EMAIL_EXISTS.getCode(), new String[]{}, locale)));
        }
        OrganizationResponse contactEmailConflict = validateSupplierRegisteredContactEmail(request, locale, null, null);
        if (contactEmailConflict != null) {
            return contactEmailConflict;
        }
        Optional<Organization> deletedTransporter = organizationRepository.findByEmail(normalizedEmail)
                .filter(organization -> organization.getEntityStatus() == EntityStatus.DELETED);
        Organization transporter = deletedTransporter.orElseGet(Organization::new);
        transporter.setName(request.getName().trim());
        transporter.setEmail(normalizedEmail);
        transporter.setPhoneNumber(request.getPhoneNumber());
        transporter.setOrganizationClassification(OrganizationClassification.TRANSPORT_COMPANY);
        if (request.getOrganizationType() != null) {
            transporter.setOrganizationType(request.getOrganizationType());
        }
        if (request.getIndustryId() != null) {
            industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED)
                    .ifPresent(transporter::setIndustry);
        }
        transporter.setContactPersonFirstName(request.getContactPersonFirstName());
        transporter.setContactPersonLastName(request.getContactPersonLastName());
        transporter.setContactPersonEmail(StringUtils.hasText(request.getContactPersonEmail())
                ? request.getContactPersonEmail().trim().toLowerCase()
                : null);
        transporter.setContactPersonPhoneNumber(request.getContactPersonPhoneNumber());
        if (request.getContactPersonGender() != null) {
            transporter.setContactPersonGender(request.getContactPersonGender());
        }
        if (StringUtils.hasText(request.getContactPersonDateOfBirth())) {
            transporter.setContactPersonDateOfBirth(request.getContactPersonDateOfBirth().trim());
        }
        if (StringUtils.hasText(request.getContactPersonNationalIdNumber())) {
            transporter.setContactPersonNationalIdNumber(request.getContactPersonNationalIdNumber().trim());
        }
        if (StringUtils.hasText(request.getContactPersonPassportNumber())) {
            transporter.setContactPersonPassportNumber(request.getContactPersonPassportNumber().trim());
        }
        if (request.getRegistrationNumber() != null) {
            transporter.setRegistrationNumber(request.getRegistrationNumber().trim());
        }
        if (request.getTaxNumber() != null) {
            transporter.setTaxNumber(request.getTaxNumber().trim());
        }
        OrganizationResponse locationResponse = applyRegistrationLocationId(transporter, request, locale);
        if (locationResponse != null) {
            return locationResponse;
        }
        transporter.setCreatedViaSignup(false);
        // Supplier-registered transporters skip platform KYC; organisation email must still be verified online.
        transporter.setKycStatus(KycStatus.APPROVED);
        transporter.setVerified(false);
        transporter.setEmailVerificationToken(null);
        clearApproverAssignments(transporter);
        transporter.setEntityStatus(EntityStatus.ACTIVE);
        if (deletedTransporter.isEmpty()) {
            transporter.setCreatedAt(LocalDateTime.now());
            transporter.setCreatedBy(username);
        }
        transporter.setCurrentResubmissionCycle(0);
        transporter.setResubmissionCount(0);
        organizationServiceAuditable.save(transporter);

        UploadOutcome uploadOutcome = organizationFileUploadHelper.processUploads(
                transporter,
                List.of(new UploadPart(
                        request.getTaxClearanceCertificateUpload(),
                        request.getTaxClearanceCertificateUploadId(),
                        FileType.TAX_CLEARANCE_CERTIFICATE,
                        null,
                        transporter::setTaxClearanceCertificateUploadId)),
                locale,
                false);
        if (!uploadOutcome.success()) {
            return buildOrganizationResponseWithErrors(uploadOutcome.errorMessages());
        }

        UploadOutcome contactIdOutcome = organizationFileUploadHelper.processUploads(
                transporter,
                List.of(
                        contactUploadPart(
                                request.getContactPersonNationalIdUpload(),
                                request.getContactPersonNationalIdUploadId(),
                                FileType.NATIONAL_ID,
                                transporter,
                                transporter::setContactPersonNationalIdUploadId,
                                request.getContactPersonNationalIdExpiryDate()),
                        contactUploadPart(
                                request.getContactPersonPassportUpload(),
                                request.getContactPersonPassportUploadId(),
                                FileType.PASSPORT,
                                transporter,
                                transporter::setContactPersonPassportUploadId,
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
            organizationServiceAuditable.save(transporter);
        }

        Organization savedTransporter = transporter;
        ContractedTransporterLink link = createTransporterContractLink(
                supplier,
                savedTransporter,
                request.getContractStartDate(),
                request.getContractEndDate(),
                username);
        organizationServiceAuditable.save(supplier);

        final Long registeredOrgId = savedTransporter.getId();
        applicationEventPublisher.publishEvent(new OrganizationSupplierRegisteredEvent(registeredOrgId));
        OrganizationDto dto = OrganizationMapping.toDto(savedTransporter);
        applyTransporterContractMetadata(dto, link);
        return buildOrganizationResponse(dto);
    }

    @Override
    public OrganizationResponse verifyOrganizationEmail(String email, String token, Locale locale) {
        SecureTokenGenerator.TokenValidationResult validationResult = tokenService.validateVerificationToken(token);
        if (!validationResult.isValid()) {
            String message = validationResult.isExpired()
                    ? messageService.getMessage(I18Code.ORG_VERIFICATION_LINK_EXPIRED.getCode(), new String[] {}, locale)
                    : messageService.getMessage(I18Code.ORG_VERIFICATION_LINK_INVALID.getCode(), new String[] {}, locale);
            return buildOrganizationResponseWithErrors(List.of(message));
        }
        if (!StringUtils.hasText(email)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_VERIFICATION_LINK_INVALID.getCode(), new String[] {}, locale)));
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Organization org = organizationRepository
                .findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED)
                .orElse(null);
        if (org == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_NOT_FOUND.getCode(), new String[] {}, locale)));
        }
        if (org.isVerified()) {
            OrganizationResponse response = buildOrganizationResponse(OrganizationMapping.toDto(org));
            response.setVerified(true);
            response.setMessage(messageService.getMessage(
                    I18Code.ORG_EMAIL_ALREADY_VERIFIED.getCode(), new String[] {}, locale));
            return response;
        }
        if (token == null || !token.equals(org.getEmailVerificationToken())) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_VERIFICATION_LINK_INVALID.getCode(), new String[] {}, locale)));
        }
        supplierRegisteredOrganizationOnboardingSupport.markVerifiedAfterEmailConfirmation(org);
        Organization verified = organizationRepository
                .findByIdAndEntityStatusNot(org.getId(), EntityStatus.DELETED)
                .orElse(org);
        OrganizationResponse response = buildOrganizationResponse(OrganizationMapping.toDto(verified));
        response.setVerified(true);
        response.setMessage(messageService.getMessage(
                I18Code.ORG_EMAIL_VERIFIED_SUCCESSFULLY.getCode(), new String[] {}, locale));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getCustomer(Long customerId, Locale locale, String username) {
        Organization supplier = loadForUser(username);
        Organization customer = requireLinkedCustomer(supplier, customerId, locale);
        OrganizationDto dto = OrganizationMapping.toDto(customer);
        organizationRegistrationAddressSupport.enrichOrganizationAddress(dto, locale);
        return buildOrganizationResponse(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getTransporter(Long transporterId, Locale locale, String username) {
        Organization caller = loadForUser(username);
        Organization partner = requireLinkedTransportPartner(caller, transporterId, locale);
        OrganizationDto dto = OrganizationMapping.toDto(partner);
        resolveTransporterContractMetadata(caller, partner, dto);
        return buildOrganizationResponse(dto);
    }

    @Override
    public OrganizationResponse updateCustomer(
            Long customerId, RegisterOrganizationRequest request, Locale locale, String username) {
        request.setOrganizationClassification(OrganizationClassification.CUSTOMER);
        if (request.getOrganizationType() == null) {
            request.setOrganizationType(OrganizationType.PRIVATE);
        }
        request.setCreatedViaSignup(false);

        Organization supplier = loadForUser(username);
        Organization customer = requireLinkedCustomer(supplier, customerId, locale);
        primeExistingCustomerUploadIds(customer, request);

        ValidatorDto v = organizationServiceValidator.validateRegister(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<Organization> emailOwner = organizationRepository.findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED);
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(customerId)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_EMAIL_EXISTS.getCode(), new String[]{}, locale)));
        }

        OrganizationResponse contactEmailConflict = validateSupplierRegisteredContactEmail(
                request, locale, customerId, customer.getContactPersonUserId());
        if (contactEmailConflict != null) {
            return contactEmailConflict;
        }

        customer.setName(request.getName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhoneNumber(request.getPhoneNumber());
        if (request.getOrganizationType() != null) {
            customer.setOrganizationType(request.getOrganizationType());
        }
        if (request.getIndustryId() != null) {
            industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED)
                    .ifPresent(customer::setIndustry);
        }
        customer.setContactPersonFirstName(request.getContactPersonFirstName());
        customer.setContactPersonLastName(request.getContactPersonLastName());
        customer.setContactPersonEmail(StringUtils.hasText(request.getContactPersonEmail())
                ? request.getContactPersonEmail().trim().toLowerCase()
                : null);
        customer.setContactPersonPhoneNumber(request.getContactPersonPhoneNumber());
        if (request.getContactPersonGender() != null) {
            customer.setContactPersonGender(request.getContactPersonGender());
        }
        if (StringUtils.hasText(request.getContactPersonDateOfBirth())) {
            customer.setContactPersonDateOfBirth(request.getContactPersonDateOfBirth().trim());
        }
        if (StringUtils.hasText(request.getContactPersonNationalIdNumber())) {
            customer.setContactPersonNationalIdNumber(request.getContactPersonNationalIdNumber().trim());
        }
        if (StringUtils.hasText(request.getContactPersonPassportNumber())) {
            customer.setContactPersonPassportNumber(request.getContactPersonPassportNumber().trim());
        }
        if (request.getRegistrationNumber() != null) {
            customer.setRegistrationNumber(request.getRegistrationNumber().trim());
        }
        if (request.getTaxNumber() != null) {
            customer.setTaxNumber(request.getTaxNumber().trim());
        }
        OrganizationResponse locationResponse = applyRegistrationLocationId(customer, request, locale);
        if (locationResponse != null) {
            return locationResponse;
        }
        customer.setModifiedAt(LocalDateTime.now());
        customer.setModifiedBy(username);
        organizationServiceAuditable.save(customer);

        UploadOutcome uploadOutcome = organizationFileUploadHelper.processUploads(
                customer,
                List.of(new UploadPart(
                        request.getTaxClearanceCertificateUpload(),
                        request.getTaxClearanceCertificateUploadId(),
                        FileType.TAX_CLEARANCE_CERTIFICATE,
                        null,
                        customer::setTaxClearanceCertificateUploadId)),
                locale,
                true);
        if (!uploadOutcome.success()) {
            return buildOrganizationResponseWithErrors(uploadOutcome.errorMessages());
        }

        UploadOutcome contactIdOutcome = organizationFileUploadHelper.processUploads(
                customer,
                List.of(
                        contactUploadPart(
                                request.getContactPersonNationalIdUpload(),
                                request.getContactPersonNationalIdUploadId(),
                                FileType.NATIONAL_ID,
                                customer,
                                customer::setContactPersonNationalIdUploadId,
                                request.getContactPersonNationalIdExpiryDate()),
                        contactUploadPart(
                                request.getContactPersonPassportUpload(),
                                request.getContactPersonPassportUploadId(),
                                FileType.PASSPORT,
                                customer,
                                customer::setContactPersonPassportUploadId,
                                request.getContactPersonPassportExpiryDate())),
                locale,
                true);
        if (!contactIdOutcome.success()) {
            return buildOrganizationResponseWithErrors(contactIdOutcome.errorMessages());
        }

        organizationServiceAuditable.save(customer);
        organizationContactPersonProvisioningSupport.syncContactPersonFromOrganization(customer);
        return buildOrganizationResponse(OrganizationMapping.toDto(customer));
    }

    @Override
    public OrganizationResponse deleteCustomer(Long customerId, Locale locale, String username) {
        Organization supplier = loadForUser(username);
        Organization customer = requireLinkedCustomer(supplier, customerId, locale);

        supplier.getCustomers().remove(customer);
        customer.getSuppliers().remove(supplier);
        organizationServiceAuditable.save(supplier);
        organizationServiceAuditable.save(customer);

        if (customer.getSuppliers().isEmpty() && !Boolean.TRUE.equals(customer.getCreatedViaSignup())) {
            customer.setEntityStatus(EntityStatus.DELETED);
            customer.setModifiedAt(LocalDateTime.now());
            customer.setModifiedBy(username);
            organizationServiceAuditable.save(customer);
        }

        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.ORG_DELETED.getCode(), new String[] {}, locale));
        return res;
    }

    @Override
    public OrganizationResponse linkTransporter(LinkTransporterRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateLinkTransporter(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization org = loadForUser(username);
        if (!OrganizationTradingCapabilitySupport.canContractTransporters(org)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_TRANSPORTER_LINK.getCode(), new String[]{}, locale)));
        }
        Organization transporter = organizationRepository.findByIdAndEntityStatusNot(request.getTransporterOrganizationId(), EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (transporter.getOrganizationClassification() != OrganizationClassification.TRANSPORT_COMPANY) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_TRANSPORTER_LINK.getCode(), new String[]{}, locale)));
        }
        if (!org.getContractedTransporters().contains(transporter)) {
            createTransporterContractLink(
                    org,
                    transporter,
                    request.getContractStartDate(),
                    request.getContractEndDate(),
                    username);
            organizationServiceAuditable.save(org);
            organizationServiceAuditable.save(transporter);
            organizationDirectoryNotifier.sendTransporterLinked(org, transporter, username);
        }
        return buildOrganizationResponse(OrganizationMapping.toDto(org));
    }

    @Override
    public OrganizationResponse linkCustomerForOrganization(
            Long supplierId, LinkCustomerRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateOrganizationId(supplierId, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        v = organizationServiceValidator.validateLinkCustomer(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization supplier = organizationRepository.findByIdAndEntityStatusNot(supplierId, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (!OrganizationTradingCapabilitySupport.canActAsSupplier(supplier)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_SUPPLIER_LINK.getCode(), new String[]{}, locale)));
        }
        Organization customer = organizationRepository.findByIdAndEntityStatusNot(request.getCustomerOrganizationId(), EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        boolean enableDuplex = Boolean.TRUE.equals(request.getEnableDuplexMode());
        if (!OrganizationTradingCapabilitySupport.canBeLinkedAsCustomer(customer, enableDuplex || customer.isDuplexMode())) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_CUSTOMER_LINK.getCode(), new String[]{}, locale)));
        }
        if (customer.getOrganizationClassification() == OrganizationClassification.SUPPLIER && enableDuplex) {
            customer.setDuplexMode(true);
        }
        if (!supplier.getCustomers().contains(customer)) {
            supplier.getCustomers().add(customer);
            customer.getSuppliers().add(supplier);
            organizationServiceAuditable.save(supplier);
            organizationServiceAuditable.save(customer);
            organizationDirectoryNotifier.sendCustomerLinked(supplier, customer, username);
        }
        return buildOrganizationResponseForSystem(supplier, locale);
    }

    @Override
    public OrganizationResponse linkTransporterForOrganization(
            Long organizationId, LinkTransporterRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateOrganizationId(organizationId, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        v = organizationServiceValidator.validateLinkTransporter(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization org = organizationRepository.findByIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (!OrganizationTradingCapabilitySupport.canContractTransporters(org)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_SUPPLIER_LINK.getCode(), new String[]{}, locale)));
        }
        Organization transporter = organizationRepository.findByIdAndEntityStatusNot(request.getTransporterOrganizationId(), EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (transporter.getOrganizationClassification() != OrganizationClassification.TRANSPORT_COMPANY) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_TRANSPORTER_LINK.getCode(), new String[]{}, locale)));
        }
        if (!org.getContractedTransporters().contains(transporter)) {
            createTransporterContractLink(
                    org,
                    transporter,
                    request.getContractStartDate(),
                    request.getContractEndDate(),
                    username);
            organizationServiceAuditable.save(org);
            organizationServiceAuditable.save(transporter);
            organizationDirectoryNotifier.sendTransporterLinked(org, transporter, username);
        }
        return buildOrganizationResponseForSystem(org, locale);
    }

    @Override
    public OrganizationResponse linkClearingAgentForOrganization(
            Long supplierId, LinkClearingAgentRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateOrganizationId(supplierId, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        v = organizationServiceValidator.validateLinkClearingAgent(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }
        Organization supplier = organizationRepository.findByIdAndEntityStatusNot(supplierId, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (!OrganizationTradingCapabilitySupport.canActAsSupplier(supplier)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_SUPPLIER_LINK.getCode(), new String[]{}, locale)));
        }
        Organization clearingAgent = organizationRepository.findByIdAndEntityStatusNot(
                        request.getClearingAgentOrganizationId(), EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (clearingAgent.getOrganizationClassification() != OrganizationClassification.CLEARING_AGENT) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_CLEARING_AGENT_LINK.getCode(), new String[]{}, locale)));
        }
        if (!supplier.getContractedClearingAgents().contains(clearingAgent)) {
            supplier.getContractedClearingAgents().add(clearingAgent);
            clearingAgent.getContractingSuppliersForClearing().add(supplier);
            organizationServiceAuditable.save(supplier);
            organizationServiceAuditable.save(clearingAgent);
            organizationDirectoryNotifier.sendClearingAgentLinked(supplier, clearingAgent, username);
        }
        return buildOrganizationResponseForSystem(supplier, locale);
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
        Page<OrganizationDto> dtoPage = page.map(this::toDtoWithKycPolicy);
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
        spec = applyFrontendOrganizationScope(spec, username);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.ASC, "id"));
        Page<Organization> page = organizationRepository.findAll(spec, pageable);
        List<OrganizationDto> dtoList = page.getContent().stream().map(this::toDtoWithKycPolicy).toList();
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
        return buildOrganizationResponse(toDtoWithKycPolicy(org));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getByIdForFrontend(Long id, String username, Locale locale) {
        assertCanAccessOrganization(id, username, locale);
        return getByIdForSystem(id, locale);
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
        dto.setContractedClearingAgentDtoList(mapNonDeletedOrganizations(org.getContractedClearingAgents()));
        return buildOrganizationResponse(dto);
    }

    private OrganizationResponse buildOrganizationResponseForSystem(Organization org, Locale locale) {
        OrganizationDto dto = OrganizationMapping.toDto(org);
        dto.setBranchDtoList(OrganizationMapping.toBranchDtos(
                branchRepository.findByOrganizationAndEntityStatusNot(org, EntityStatus.DELETED)));
        Specification<Agent> agentSpec = Specification.where(AgentSpecifications.notDeleted())
                .and(AgentSpecifications.organizationIdEquals(org.getId()));
        dto.setAgentDtoList(OrganizationMapping.toAgentDtos(agentRepository.findAll(agentSpec)));
        dto.setCustomerDtoList(mapNonDeletedOrganizations(org.getCustomers()));
        dto.setContractedTransporterDtoList(mapNonDeletedOrganizations(org.getContractedTransporters()));
        dto.setContractedClearingAgentDtoList(mapNonDeletedOrganizations(org.getContractedClearingAgents()));
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
        return approveKycStage(1, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage1Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        return rejectKycStage(1, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage2Approve(Long id, KycActionRequest request, Locale locale, String username) {
        return approveKycStage(2, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage2Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        return rejectKycStage(2, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage3Approve(Long id, KycActionRequest request, Locale locale, String username) {
        return approveKycStage(3, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage3Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        return rejectKycStage(3, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage4Approve(Long id, KycActionRequest request, Locale locale, String username) {
        return approveKycStage(4, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage4Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        return rejectKycStage(4, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage5Approve(Long id, KycActionRequest request, Locale locale, String username) {
        return approveKycStage(5, id, request, locale, username);
    }

    @Override
    public OrganizationResponse stage5Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        return rejectKycStage(5, id, request, locale, username);
    }

    private OrganizationResponse approveKycStage(
            int stage,
            Long id,
            KycActionRequest request,
            Locale locale,
            String username) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        KycReviewStage reviewStage = KycStageSupport.toReviewStage(stage);
        assertAssignedKycApprover(org, reviewStage, request, username, locale);
        KycStatus currentStatus = stage == 1
                ? prepareOrganizationForStage1Decision(org, locale)
                : org.getKycStatus();
        KycStatus expectedStatus = KycStageSupport.reviewStatus(stage);
        if (currentStatus != expectedStatus) {
            throw new BusinessRuleException(
                    messageService.getMessage(I18Code.KYC_INVALID_TRANSITION.getCode(), new String[]{}, locale),
                    I18Code.KYC_INVALID_TRANSITION);
        }
        int requiredStages = kycApprovalStageResolver.resolveRequiredStages(org);
        String reviewer = resolveReviewer(request, username);
        LocalDateTime reviewedAt = LocalDateTime.now();
        OrganizationKycReview row = buildReviewRow(
                org, reviewStage, KycDecision.APPROVED, request, reviewer, reviewedAt, null);
        organizationKycReviewServiceAuditable.save(row);
        KycStageSupport.setReviewed(org, stage, reviewer, reviewedAt);
        org.setModifiedAt(reviewedAt);
        org.setModifiedBy(username);

        if (stage >= requiredStages) {
            kycStateMachine.assertCanTransition(expectedStatus, KycStatus.APPROVED, locale);
            org.setKycStatus(KycStatus.APPROVED);
            org.setVerified(true);
            Organization saved = organizationServiceAuditable.save(org);
            afterCommit(() -> {
                organizationEventPublisher.publishStage2Approved(saved, reviewer, reviewedAt);
                organizationEventPublisher.publishVerified(saved, reviewedAt);
                organizationKycNotifier.sendFullyApproved(saved);
                organizationApprovedCredentialsSupport.issueAndEmailCredentials(saved);
            });
            return buildOrganizationResponse(toDtoWithKycPolicy(saved));
        }

        KycStatus nextStatus = KycStageSupport.reviewStatus(stage + 1);
        kycStateMachine.assertCanTransition(expectedStatus, nextStatus, locale);
        org.setKycStatus(nextStatus);
        Organization saved = organizationServiceAuditable.save(org);
        int approvedStage = stage;
        int totalStages = requiredStages;
        afterCommit(() -> {
            organizationEventPublisher.publishStage1Approved(saved, reviewer, reviewedAt);
            organizationKycNotifier.sendStageApproved(saved, approvedStage, totalStages);
        });
        return buildOrganizationResponse(toDtoWithKycPolicy(saved));
    }

    private OrganizationResponse rejectKycStage(
            int stage,
            Long id,
            KycRejectRequest request,
            Locale locale,
            String username) {
        Objects.requireNonNull(request.getRejectionReason(), "rejectionReason");
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        KycReviewStage reviewStage = KycStageSupport.toReviewStage(stage);
        assertAssignedKycApprover(org, reviewStage, request, username, locale);
        if (stage == 1) {
            prepareOrganizationForStage1Decision(org, locale);
        }
        KycStatus expectedStatus = KycStageSupport.reviewStatus(stage);
        if (org.getKycStatus() != expectedStatus) {
            throw new BusinessRuleException(
                    messageService.getMessage(I18Code.KYC_INVALID_TRANSITION.getCode(), new String[]{}, locale),
                    I18Code.KYC_INVALID_TRANSITION);
        }
        kycStateMachine.assertCanTransition(expectedStatus, KycStatus.REJECTED, locale);
        String reviewer = resolveReviewer(request, username);
        LocalDateTime reviewedAt = LocalDateTime.now();
        OrganizationKycReview row = buildReviewRow(
                org, reviewStage, KycDecision.REJECTED, request, reviewer, reviewedAt, request.getRejectionReason());
        organizationKycReviewServiceAuditable.save(row);
        org.setKycStatus(KycStatus.REJECTED);
        KycStageSupport.setReviewed(org, stage, reviewer, reviewedAt);
        org.setLastRejectionReason(request.getRejectionReason());
        org.setModifiedAt(reviewedAt);
        org.setModifiedBy(username);
        Organization saved = organizationServiceAuditable.save(org);
        String rejectionReason = request.getRejectionReason();
        afterCommit(() -> {
            organizationEventPublisher.publishRejected(saved, reviewStage, rejectionReason, reviewer, reviewedAt);
            organizationKycNotifier.sendRejected(saved, rejectionReason);
        });
        return buildOrganizationResponse(OrganizationMapping.toDto(saved));
    }

    @Override
    public OrganizationResponse allowResubmission(Long id, KycActionRequest request, Locale locale, String username) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElseThrow(() -> notFound(locale));
        kycStateMachine.assertCanTransition(KycStatus.REJECTED, KycStatus.DRAFT, locale);
        LocalDateTime now = LocalDateTime.now();
        String resubmissionNotes = request != null && StringUtils.hasText(request.getNotes())
                ? request.getNotes().trim()
                : null;
        org.setKycStatus(KycStatus.DRAFT);
        org.setSubmittedAt(null);
        org.setVerified(false);
        org.setResubmissionCount(org.getResubmissionCount() + 1);
        org.setCurrentResubmissionCycle(org.getCurrentResubmissionCycle() + 1);
        org.setLastRejectionReason(resubmissionNotes);
        org.setModifiedAt(now);
        org.setModifiedBy(username);
        if (Boolean.TRUE.equals(org.getCreatedViaSignup())) {
            KycStageSupport.clearAllAssignedApprovers(org);
            kycApproverAssignmentService.assignApprovers(org);
        }
        Organization saved = organizationServiceAuditable.save(org);
        String allowedBy = resolveReviewer(request, username);
        afterCommit(() -> {
            organizationEventPublisher.publishResubmitted(saved, allowedBy, now);
            organizationKycNotifier.sendResubmissionAllowed(saved, resubmissionNotes);
        });
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
    public OrganizationResponse listActiveIndustriesForPlatform(Locale locale) {
        List<Industry> industries = industryRepository.findByEntityStatusNotOrderByNameAsc(EntityStatus.DELETED);
        List<IndustryUsageDto> usageRows = new ArrayList<>();
        for (Industry industry : industries) {
            if (industry.isActive()) {
                usageRows.add(IndustryMapping.toUsageDto(industry));
            }
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
        if (StringUtils.hasText(request.getBranchLevel())) {
            spec = spec.and(BranchSpecifications.branchLevelEquals(
                    BranchLevel.valueOf(request.getBranchLevel().trim().toUpperCase())));
        }
        if (request.getDepot() != null) {
            spec = spec.and(BranchSpecifications.depotEquals(request.getDepot()));
        }
        if (request.getParentBranchId() != null && request.getParentBranchId() > 0) {
            spec = spec.and(BranchSpecifications.parentBranchIdEquals(request.getParentBranchId()));
        }
        if (StringUtils.hasText(request.getRegion())) {
            spec = spec.and(BranchSpecifications.regionLike(request.getRegion()));
        }
        if (request.getActive() != null) {
            spec = spec.and(BranchSpecifications.activeEquals(request.getActive()));
        }
        Organization scopedOrg = null;
        String scopedOrgRegion = null;
        String scopedOrgBusinessHours = null;
        if (request.getOrganizationId() != null) {
            scopedOrg = organizationRepository
                    .findByIdAndEntityStatusNot(request.getOrganizationId(), EntityStatus.DELETED)
                    .orElse(null);
            if (scopedOrg != null) {
                scopedOrgRegion = organizationRegistrationAddressSupport.resolveOrganizationRegionLabel(
                        scopedOrg, locale);
                if (StringUtils.hasText(scopedOrg.getBusinessHours())) {
                    scopedOrgBusinessHours = scopedOrg.getBusinessHours().trim();
                }
            }
        }
        final String orgRegionLabel = scopedOrgRegion;
        final String orgBusinessHoursLabel = scopedOrgBusinessHours;
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "branchName"));
        java.util.Map<Long, String> orgRegionCache = new java.util.HashMap<>();
        Page<BranchDto> dtoPage = branchRepository.findAll(spec, pageable).map(branch -> {
            BranchDto dto = OrganizationMapping.toBranchDto(branch);
            if (branch.isHeadOffice()) {
                if (!StringUtils.hasText(dto.getRegion()) && StringUtils.hasText(orgRegionLabel)) {
                    dto.setRegion(orgRegionLabel);
                }
                if (!StringUtils.hasText(dto.getBusinessHours()) && StringUtils.hasText(orgBusinessHoursLabel)) {
                    dto.setBusinessHours(orgBusinessHoursLabel);
                }
            }
            organizationRegistrationAddressSupport.enrichHeadOfficeBranchRegion(
                    branch, dto, locale, orgRegionCache);
            return dto;
        });
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

    private OrganizationResponse applyRegistrationLocationId(
            Organization org, RegisterOrganizationRequest request, Locale locale) {
        try {
            Long locationId = organizationRegistrationAddressSupport.resolveLocationId(request, locale);
            if (locationId != null) {
                org.setLocationId(locationId);
            }
            return null;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return buildOrganizationResponseWithErrors(List.of(ex.getMessage()));
        }
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
        KycStageSupport.clearAllAssignedApprovers(org);
    }

    /**
     * Platform signup re-registration for the same organisation email after an admin opened resubmission
     * ({@link KycStatus#DRAFT} with {@code currentResubmissionCycle > 0}).
     */
    private boolean isSignupResubmissionRegistration(Organization org) {
        return Boolean.TRUE.equals(org.getCreatedViaSignup())
                && org.getKycStatus() == KycStatus.DRAFT
                && org.getCurrentResubmissionCycle() > 0;
    }

    private OrganizationResponse applySignupResubmissionRegistration(
            Organization org,
            RegisterOrganizationRequest request,
            Locale locale,
            String createdBy) {
        org.setName(request.getName().trim());
        org.setPhoneNumber(request.getPhoneNumber());
        org.setOrganizationClassification(request.getOrganizationClassification());
        if (request.getOrganizationType() != null) {
            org.setOrganizationType(request.getOrganizationType());
        }
        if (request.getIndustryId() != null) {
            industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED)
                    .ifPresent(org::setIndustry);
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
        org.setKycStatus(KycStatus.DRAFT);
        org.setVerified(false);
        org.setSubmittedAt(null);
        org.setModifiedAt(LocalDateTime.now());
        org.setModifiedBy(createdBy);
        KycStageSupport.clearAllAssignedApprovers(org);
        kycApproverAssignmentService.assignApprovers(org);
        organizationServiceAuditable.save(org);

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
        organizationServiceAuditable.save(org);

        final Long orgId = org.getId();
        afterCommit(() -> organizationRepository.findById(orgId).ifPresent(fresh -> {
            provisionContactPersonUserIfNeeded(fresh, true);
            organizationRepository.findById(orgId).ifPresent(afterProvision ->
                    organizationRegistrationNotifier.sendRegistrationEmails(afterProvision, true));
        }));
        return buildOrganizationResponse(OrganizationMapping.toDto(org));
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
        Long assignedUserId = KycStageSupport.getAssignedApproverUserId(org, KycStageSupport.toStageNumber(stage));
        String assignedUsername = KycStageSupport.getAssignedApproverUsername(org, KycStageSupport.toStageNumber(stage));
        if (assignedUserId == null && (assignedUsername == null || assignedUsername.isBlank())) {
            return;
        }
        Long actingUserId = resolveActingUserId(request, principal);
        if (assignedUserId != null && actingUserId != null && actingUserId.equals(assignedUserId)) {
            assertNotCrossStageApprover(org, stage, actingUserId, principal, locale);
            return;
        }
        if (assignedUsername != null && principalsMatch(principal, assignedUsername)) {
            assertNotCrossStageApprover(org, stage, actingUserId, principal, locale);
            return;
        }
        Long resolvedId = resolveUserIdByPrincipal(principal);
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
        int currentStage = KycStageSupport.toStageNumber(stage);
        if (currentStage <= 1) {
            return;
        }
        if (kycApprovalStageResolver.resolveRequiredStages(org) <= 1) {
            return;
        }
        for (int priorStage = KycStageSupport.MIN_STAGE; priorStage < currentStage; priorStage++) {
            Long priorUserId = KycStageSupport.getAssignedApproverUserId(org, priorStage);
            if (priorUserId != null && actingUserId != null && priorUserId.equals(actingUserId)) {
                throw sameApproverException(locale);
            }
            String priorUsername = KycStageSupport.getAssignedApproverUsername(org, priorStage);
            if (priorUsername != null && actingUsername != null
                    && priorUsername.equalsIgnoreCase(actingUsername.trim())) {
                throw sameApproverException(locale);
            }
        }
    }

    private BusinessRuleException sameApproverException(Locale locale) {
        return new BusinessRuleException(
                messageService.getMessage(I18Code.KYC_SAME_APPROVER.getCode(), new String[]{}, locale),
                I18Code.KYC_SAME_APPROVER);
    }

    /**
     * Returns the acting user-id for KYC approver checks.
     * Prefers an explicit id from the request; falls back to a Feign lookup by principal.
     */
    private Long resolveActingUserId(KycActionRequest request, String principal) {
        if (request != null && request.getReviewerUserId() != null) {
            return request.getReviewerUserId();
        }
        return resolveUserIdByPrincipal(principal);
    }

    /**
     * Matches {@code principal} against {@code assignedUsername} tolerating:
     * <ul>
     *   <li>Case-insensitive exact match (e.g. "kaydizz" vs "KAYDIZZ")</li>
     *   <li>Email local-part match (e.g. "kaydizz@example.com" vs "kaydizz")</li>
     * </ul>
     */
    private boolean principalsMatch(String principal, String assignedUsername) {
        if (assignedUsername == null) {
            return false;
        }
        String trimmedAssigned = assignedUsername.trim();
        if (principal.equalsIgnoreCase(trimmedAssigned)) {
            return true;
        }
        int atIndex = principal.indexOf('@');
        if (atIndex > 0) {
            String localPart = principal.substring(0, atIndex);
            return localPart.equalsIgnoreCase(trimmedAssigned);
        }
        return false;
    }

    private Long resolveUserIdByPrincipal(String principal) {
        try {
            UserResponse response = userManagementServiceClient.findByUsername(principal);
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
                org.setContactPersonEmail(request.getContactPersonEmail().trim().toLowerCase(Locale.ROOT));
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
        Long organizationId = resolveCallerOrganizationId(username);
        if (organizationId != null) {
            return organizationRepository
                    .findByIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                    .orElseThrow(() -> notFound(Locale.getDefault()));
        }
        if (!StringUtils.hasText(username)) {
            throw notFound(Locale.getDefault());
        }
        String principal = username.trim();
        if (principal.contains("@")) {
            return organizationRepository
                    .findByEmailAndEntityStatusNot(principal.toLowerCase(Locale.ROOT), EntityStatus.DELETED)
                    .orElseThrow(() -> notFound(Locale.getDefault()));
        }
        throw notFound(Locale.getDefault());
    }

    private OrganizationResponse assertAgentOwnedByUser(Long agentId, String username, Locale locale) {
        Organization org = loadForUser(username);
        Optional<Agent> agentOpt = agentRepository.findByIdAndEntityStatusNot(agentId, EntityStatus.DELETED);
        if (agentOpt.isEmpty()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.AGENT_NOT_FOUND.getCode(), new String[]{}, locale)));
        }
        if (!agentOpt.get().getOrganization().getId().equals(org.getId())) {
            return buildOrganizationResponseWithErrors(List.of("Agent does not belong to your organisation."));
        }
        return null;
    }

    private OrganizationResponse assertBranchOwnedByUser(Long branchId, String username, Locale locale) {
        Organization org = loadForUser(username);
        Optional<Branch> branchOpt = branchRepository.findByIdAndEntityStatusNot(branchId, EntityStatus.DELETED);
        if (branchOpt.isEmpty()) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.BRANCH_NOT_FOUND.getCode(), new String[]{}, locale)));
        }
        if (!branchOpt.get().getOrganization().getId().equals(org.getId())) {
            return buildOrganizationResponseWithErrors(List.of("Branch does not belong to your organisation."));
        }
        return null;
    }

    private Long resolveCallerOrganizationId(String username) {
        if (!StringUtils.hasText(username) || TRUSTED_ADMIN_MODIFIERS.contains(username.trim())) {
            return null;
        }
        String principal = username.trim();
        try {
            UserResponse userResponse = userManagementServiceClient.findSessionProfileByUsername(principal);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getOrganizationId() != null
                    && userResponse.getUserDto().getOrganizationId() > 0) {
                return userResponse.getUserDto().getOrganizationId();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve organisation id for user {} via user-management: {}", principal, ex.getMessage());
        }
        return null;
    }

    private Specification<Organization> applyFrontendOrganizationScope(Specification<Organization> spec, String username) {
        if (!StringUtils.hasText(username) || TRUSTED_ADMIN_MODIFIERS.contains(username.trim())) {
            return spec;
        }
        Long callerOrganizationId = resolveCallerOrganizationId(username);
        if (callerOrganizationId == null) {
            return spec;
        }
        return spec.and(OrganizationSpecifications.idEquals(callerOrganizationId));
    }

    private void assertCanAccessOrganization(Long organizationId, String username, Locale locale) {
        if (organizationId == null || organizationId < 1) {
            throw notFound(locale);
        }
        if (!StringUtils.hasText(username) || TRUSTED_ADMIN_MODIFIERS.contains(username.trim())) {
            return;
        }
        Long callerOrganizationId = resolveCallerOrganizationId(username);
        if (callerOrganizationId != null && callerOrganizationId.equals(organizationId)) {
            return;
        }
        throw notFound(locale);
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
                        log.error("Post-commit action failed after organisation transaction: {}", ex.getMessage(), ex);
                    }
                }
            });
        } else {
            action.run();
        }
    }

    @Override
    public OrganizationResponse completeSupplierRegisteredOnboarding(Long organizationId, Locale locale) {
        if (organizationId == null || organizationId < 1) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_NOT_FOUND.getCode(), new String[] {}, locale)));
        }
        Organization org = organizationRepository.findByIdAndEntityStatusNot(organizationId, EntityStatus.DELETED).orElse(null);
        if (org == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_NOT_FOUND.getCode(), new String[] {}, locale)));
        }
        if (Boolean.TRUE.equals(org.getCreatedViaSignup())) {
            return buildOrganizationResponseWithErrors(
                    List.of("Onboarding retry is only supported for supplier-registered customers and transporters."));
        }
        SupplierRegisteredOnboardingResult outcome =
                supplierRegisteredOrganizationOnboardingSupport.completeOnboarding(org);
        if (!outcome.contactCredentialsQueued()) {
            return buildOrganizationResponseWithErrors(List.of(outcome.detailMessage()));
        }
        OrganizationResponse response = buildOrganizationResponse(OrganizationMapping.toDto(org));
        response.setMessage(outcome.detailMessage());
        return response;
    }

    @Override
    public OrganizationResponse retryCustomerOnboarding(Long customerId, Locale locale, String username) {
        Organization customer = requireLinkedCustomer(loadForUser(username), customerId, locale);
        if (Boolean.TRUE.equals(customer.getCreatedViaSignup())) {
            return buildOrganizationResponseWithErrors(
                    List.of("Onboarding email retry applies to supplier-registered customers only."));
        }
        return completeSupplierRegisteredOnboarding(customer.getId(), locale);
    }

    /**
     * Rejects supplier registration when the contact email already belongs to a user linked to another organisation.
     */
    private OrganizationResponse validateSupplierRegisteredContactEmail(
            RegisterOrganizationRequest request, Locale locale, Long organizationId, Long linkedContactUserId) {
        if (request == null || !StringUtils.hasText(request.getContactPersonEmail())) {
            return null;
        }
        String normalizedEmail = request.getContactPersonEmail().trim().toLowerCase(Locale.ROOT);
        try {
            UsersMultipleFiltersFeignRequest filters = new UsersMultipleFiltersFeignRequest();
            filters.setPage(0);
            filters.setSize(5);
            filters.setSearchValue(normalizedEmail);
            var response = userManagementServiceClient.findUsersByMultipleFilters(filters);
            if (response == null || !response.isSuccess() || response.getUserDtoPage() == null) {
                return null;
            }
            for (UserDto user : response.getUserDtoPage().getContent()) {
                if (user == null || user.getEmail() == null) {
                    continue;
                }
                if (!normalizedEmail.equals(user.getEmail().trim().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (linkedContactUserId != null && linkedContactUserId.equals(user.getId())) {
                    continue;
                }
                Long linkedOrgId = user.getOrganizationId();
                if (linkedOrgId != null && linkedOrgId > 0
                        && (organizationId == null || !linkedOrgId.equals(organizationId))) {
                    return buildOrganizationResponseWithErrors(
                            List.of(messageService.getMessage(
                                    I18Code.ORG_CONTACT_EMAIL_LINKED.getCode(), new String[] {}, locale)));
                }
            }
        } catch (Exception ex) {
            log.warn("Could not validate contact person email {} during supplier registration: {}", normalizedEmail,
                    ex.getMessage());
        }
        return null;
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
        if (errors != null && !errors.isEmpty()) {
            r.setMessage(errors.get(0));
        }
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
    public OrganizationResponse getHeadOfficeBranch(Long organizationId, Locale locale) {
        return organizationDirectoryAdminService.getHeadOfficeBranch(organizationId, locale);
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

    @Override
    @Transactional(readOnly = true)
    public OrganizationManagementResponse getKycApprovalPolicy(Locale locale) {
        KycApprovalPolicyDto policy = buildKycApprovalPolicyDto();
        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setKycApprovalPolicyDto(policy);
        return response;
    }

    @Override
    public OrganizationManagementResponse updateKycApprovalPolicy(
            UpdateKycApprovalPolicyRequest request, Locale locale, String modifiedBy) {
        if (request == null || request.getDefaultRequiredApprovalStages() == null) {
            return buildPolicyErrorResponse(
                    List.of(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[]{}, locale)));
        }
        int stages = kycApprovalStageResolver.clampStages(request.getDefaultRequiredApprovalStages());
        PlatformKycPolicy policy = platformKycPolicyRepository
                .findFirstByEntityStatusNotOrderByIdAsc(EntityStatus.DELETED)
                .orElseGet(() -> {
                    PlatformKycPolicy created = new PlatformKycPolicy();
                    created.setEntityStatus(EntityStatus.ACTIVE);
                    created.setCreatedAt(LocalDateTime.now());
                    created.setCreatedBy(modifiedBy);
                    return created;
                });
        policy.setDefaultRequiredApprovalStages(stages);
        policy.setModifiedAt(LocalDateTime.now());
        policy.setModifiedBy(modifiedBy);
        platformKycPolicyRepository.save(policy);
        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setKycApprovalPolicyDto(buildKycApprovalPolicyDto());
        return response;
    }

    @Override
    public OrganizationResponse updateOrganizationKycStages(
            Long id, UpdateOrganizationKycStagesRequest request, Locale locale, String modifiedBy) {
        Organization org = organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        Integer stages = request != null ? request.getKycRequiredApprovalStages() : null;
        if (stages != null) {
            org.setKycRequiredApprovalStages(kycApprovalStageResolver.clampStages(stages));
        } else {
            org.setKycRequiredApprovalStages(null);
        }
        org.setModifiedAt(LocalDateTime.now());
        org.setModifiedBy(modifiedBy);
        if (Boolean.TRUE.equals(org.getCreatedViaSignup())) {
            kycApproverAssignmentService.assignApprovers(org);
        }
        Organization saved = organizationServiceAuditable.save(org);
        return buildOrganizationResponse(toDtoWithKycPolicy(saved));
    }

    private OrganizationDto toDtoWithKycPolicy(Organization org) {
        OrganizationDto dto = OrganizationMapping.toDto(org);
        if (dto != null) {
            dto.setEffectiveKycRequiredApprovalStages(kycApprovalStageResolver.resolveRequiredStages(org));
        }
        return dto;
    }

    private KycApprovalPolicyDto buildKycApprovalPolicyDto() {
        KycApprovalPolicyDto dto = new KycApprovalPolicyDto();
        dto.setDefaultRequiredApprovalStages(kycApprovalStageResolver.loadPlatformDefault());
        dto.setMinAllowedStages(KycApprovalStageResolver.MIN_STAGES);
        dto.setMaxAllowedStages(KycApprovalStageResolver.MAX_STAGES);
        return dto;
    }

    private OrganizationManagementResponse buildPolicyErrorResponse(List<String> errors) {
        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(false);
        response.setStatusCode(400);
        response.setErrorMessages(errors);
        return response;
    }

    /**
     * Resolves the transport-company partner visible to the caller:
     * - SUPPLIER or CUSTOMER: the target must be a TRANSPORT_COMPANY in the caller's contractedTransporters.
     * - TRANSPORT_COMPANY: the target must be one of the shippers that have contracted this transporter
     *   (i.e. present in findContractingOrganizationsForTransporter).
     */
    private Organization requireLinkedTransportPartner(Organization caller, Long transporterId, Locale locale) {
        Organization partner = organizationRepository.findByIdAndEntityStatusNot(transporterId, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));

        if (OrganizationTradingCapabilitySupport.canContractTransporters(caller)) {
            if (partner.getOrganizationClassification() != OrganizationClassification.TRANSPORT_COMPANY) {
                throw notFound(locale);
            }
            List<Organization> contracted = organizationRepository.findContractedTransportersForSupplier(
                    caller.getId(), EntityStatus.DELETED);
            if (!contracted.contains(partner)) {
                throw notFound(locale);
            }
        } else if (caller.getOrganizationClassification() == OrganizationClassification.TRANSPORT_COMPANY) {
            List<Organization> contracting = organizationRepository.findContractingOrganizationsForTransporter(
                    caller.getId(), EntityStatus.DELETED);
            if (!contracting.contains(partner)) {
                throw notFound(locale);
            }
        } else {
            throw notFound(locale);
        }

        return partner;
    }

    private OrganizationResponse validateFleetVehicleContractDatesWithinPartnerLink(
            Organization org,
            Long transporterId,
            String contractScopeRaw,
            String contractStartDateRaw,
            String contractEndDateRaw,
            Locale locale) {
        String scope = contractScopeRaw == null || contractScopeRaw.isBlank()
                ? "LONG_TERM"
                : contractScopeRaw.trim().toUpperCase();
        if (!"LONG_TERM".equals(scope)) {
            return null;
        }

        if (!StringUtils.hasText(contractStartDateRaw)) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACT_START_REQUIRED.getCode(), new String[]{}, locale)));
        }

        LocalDate vehicleStart;
        LocalDate vehicleEnd = null;
        try {
            vehicleStart = LocalDate.parse(contractStartDateRaw.trim());
            if (StringUtils.hasText(contractEndDateRaw)) {
                vehicleEnd = LocalDate.parse(contractEndDateRaw.trim());
            }
        } catch (java.time.format.DateTimeParseException ex) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACT_START_REQUIRED.getCode(), new String[]{}, locale)));
        }

        if (vehicleEnd != null && vehicleEnd.isBefore(vehicleStart)) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACT_DATES_OUT_OF_RANGE.getCode(), new String[]{}, locale)));
        }

        ContractedTransporterLink link = contractedTransporterLinkRepository
                .findByOrganizationIdAndTransporterId(org.getId(), transporterId)
                .orElse(null);
        if (link == null) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACTED_TRANSPORTER_INVALID.getCode(), new String[]{}, locale)));
        }

        LocalDate linkStart = link.getContractStartDate();
        LocalDate linkEnd = link.getContractEndDate();
        if (linkStart != null && vehicleStart.isBefore(linkStart)) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACT_DATES_OUT_OF_RANGE.getCode(), new String[]{}, locale)));
        }
        if (linkEnd != null) {
            if (vehicleStart.isAfter(linkEnd)) {
                return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                        I18Code.FLEET_VEHICLE_CONTRACT_DATES_OUT_OF_RANGE.getCode(), new String[]{}, locale)));
            }
            if (vehicleEnd != null && vehicleEnd.isAfter(linkEnd)) {
                return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                        I18Code.FLEET_VEHICLE_CONTRACT_DATES_OUT_OF_RANGE.getCode(), new String[]{}, locale)));
            }
        }

        return null;
    }

    /**
     * Platform self-service may edit only while KYC is draft/resubmitted.
     * Admin/system/backoffice updates and admin-registered organisations are exempt.
     */
    private ContractedTransporterLink createTransporterContractLink(
            Organization supplier,
            Organization transporter,
            String contractStartDateRaw,
            String contractEndDateRaw,
            String username) {
        LocalDate start = LocalDate.parse(contractStartDateRaw.trim());
        LocalDate end = StringUtils.hasText(contractEndDateRaw) ? LocalDate.parse(contractEndDateRaw.trim()) : null;
        LocalDateTime now = LocalDateTime.now();

        ContractedTransporterLink link = contractedTransporterLinkRepository
                .findByOrganizationIdAndTransporterId(supplier.getId(), transporter.getId())
                .orElseGet(ContractedTransporterLink::new);
        link.setOrganizationId(supplier.getId());
        link.setTransporterId(transporter.getId());
        link.setOrganization(supplier);
        link.setTransporter(transporter);
        link.setContractStartDate(start);
        link.setContractEndDate(end);
        if (link.getLinkedAt() == null) {
            link.setLinkedAt(now);
        }
        if (link.getCreatedAt() == null) {
            link.setCreatedAt(now);
            link.setCreatedBy(username);
        }
        link.setModifiedAt(now);
        link.setModifiedBy(username);
        link.setEntityStatus(EntityStatus.ACTIVE);
        contractedTransporterLinkRepository.save(link);
        return link;
    }

    private void applyTransporterContractMetadata(OrganizationDto dto, ContractedTransporterLink link) {
        if (dto == null || link == null) {
            return;
        }
        dto.setContractStartDate(link.getContractStartDate());
        dto.setContractEndDate(link.getContractEndDate());
        dto.setContractLinkedAt(link.getLinkedAt());
    }

    private void resolveTransporterContractMetadata(Organization caller, Organization partner, OrganizationDto dto) {
        ContractedTransporterLink link = null;
        if (OrganizationTradingCapabilitySupport.canContractTransporters(caller)) {
            link = contractedTransporterLinkRepository
                    .findByOrganizationIdAndTransporterId(caller.getId(), partner.getId())
                    .orElse(null);
        } else if (caller.getOrganizationClassification() == OrganizationClassification.TRANSPORT_COMPANY) {
            link = contractedTransporterLinkRepository
                    .findByOrganizationIdAndTransporterId(partner.getId(), caller.getId())
                    .orElse(null);
        }
        applyTransporterContractMetadata(dto, link);
    }

    private Organization requireLinkedCustomer(Organization supplier, Long customerId, Locale locale) {
        if (!OrganizationTradingCapabilitySupport.canActAsSupplier(supplier)) {
            throw notFound(locale);
        }
        Organization customer = organizationRepository.findByIdAndEntityStatusNot(customerId, EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (!supplier.getCustomers().contains(customer)) {
            throw notFound(locale);
        }
        return customer;
    }

    private void primeExistingCustomerUploadIds(Organization customer, RegisterOrganizationRequest request) {
        if (request.getTaxClearanceCertificateUploadId() == null
                && customer.getTaxClearanceCertificateUploadId() != null) {
            request.setTaxClearanceCertificateUploadId(customer.getTaxClearanceCertificateUploadId());
        }
        if (request.getContactPersonNationalIdUploadId() == null
                && customer.getContactPersonNationalIdUploadId() != null) {
            request.setContactPersonNationalIdUploadId(customer.getContactPersonNationalIdUploadId());
        }
        if (request.getContactPersonPassportUploadId() == null
                && customer.getContactPersonPassportUploadId() != null) {
            request.setContactPersonPassportUploadId(customer.getContactPersonPassportUploadId());
        }
    }

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

    // =========================================================
    // updateTransporter
    // =========================================================
    @Override
    public OrganizationResponse updateTransporter(
            Long transporterId, RegisterOrganizationRequest request, Locale locale, String username) {
        request.setOrganizationClassification(OrganizationClassification.TRANSPORT_COMPANY);
        if (request.getOrganizationType() == null) {
            request.setOrganizationType(OrganizationType.PRIVATE);
        }
        request.setCreatedViaSignup(false);

        Organization supplier = loadForUser(username);
        Organization transporter = requireLinkedTransportPartner(supplier, transporterId, locale);
        primeExistingCustomerUploadIds(transporter, request);

        ValidatorDto v = organizationServiceValidator.validateRegister(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return buildOrganizationResponseWithErrors(v.getErrorMessages());
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<Organization> emailOwner = organizationRepository.findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED);
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(transporterId)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_EMAIL_EXISTS.getCode(), new String[]{}, locale)));
        }

        OrganizationResponse contactEmailConflict = validateSupplierRegisteredContactEmail(
                request, locale, transporterId, transporter.getContactPersonUserId());
        if (contactEmailConflict != null) {
            return contactEmailConflict;
        }

        if (StringUtils.hasText(request.getContractStartDate()) || StringUtils.hasText(request.getContractEndDate())) {
            ValidatorDto contractValidation = organizationServiceValidator.validateTransporterContract(
                    request.getContractStartDate(), request.getContractEndDate(), locale);
            if (Boolean.FALSE.equals(contractValidation.getSuccess())) {
                return buildOrganizationResponseWithErrors(contractValidation.getErrorMessages());
            }
        }

        transporter.setName(request.getName().trim());
        transporter.setEmail(normalizedEmail);
        transporter.setPhoneNumber(request.getPhoneNumber());
        if (request.getOrganizationType() != null) {
            transporter.setOrganizationType(request.getOrganizationType());
        }
        if (request.getIndustryId() != null) {
            industryRepository.findByIdAndEntityStatusNot(request.getIndustryId(), EntityStatus.DELETED)
                    .ifPresent(transporter::setIndustry);
        }
        transporter.setContactPersonFirstName(request.getContactPersonFirstName());
        transporter.setContactPersonLastName(request.getContactPersonLastName());
        transporter.setContactPersonEmail(StringUtils.hasText(request.getContactPersonEmail())
                ? request.getContactPersonEmail().trim().toLowerCase()
                : null);
        transporter.setContactPersonPhoneNumber(request.getContactPersonPhoneNumber());
        if (request.getContactPersonGender() != null) {
            transporter.setContactPersonGender(request.getContactPersonGender());
        }
        if (StringUtils.hasText(request.getContactPersonDateOfBirth())) {
            transporter.setContactPersonDateOfBirth(request.getContactPersonDateOfBirth().trim());
        }
        if (StringUtils.hasText(request.getContactPersonNationalIdNumber())) {
            transporter.setContactPersonNationalIdNumber(request.getContactPersonNationalIdNumber().trim());
        }
        if (StringUtils.hasText(request.getContactPersonPassportNumber())) {
            transporter.setContactPersonPassportNumber(request.getContactPersonPassportNumber().trim());
        }
        if (request.getRegistrationNumber() != null) {
            transporter.setRegistrationNumber(request.getRegistrationNumber().trim());
        }
        if (request.getTaxNumber() != null) {
            transporter.setTaxNumber(request.getTaxNumber().trim());
        }
        OrganizationResponse locationResponse = applyRegistrationLocationId(transporter, request, locale);
        if (locationResponse != null) {
            return locationResponse;
        }
        transporter.setModifiedAt(LocalDateTime.now());
        transporter.setModifiedBy(username);
        organizationServiceAuditable.save(transporter);

        UploadOutcome uploadOutcome = organizationFileUploadHelper.processUploads(
                transporter,
                List.of(new UploadPart(
                        request.getTaxClearanceCertificateUpload(),
                        request.getTaxClearanceCertificateUploadId(),
                        FileType.TAX_CLEARANCE_CERTIFICATE,
                        null,
                        transporter::setTaxClearanceCertificateUploadId)),
                locale,
                true);
        if (!uploadOutcome.success()) {
            return buildOrganizationResponseWithErrors(uploadOutcome.errorMessages());
        }

        UploadOutcome contactIdOutcome = organizationFileUploadHelper.processUploads(
                transporter,
                List.of(
                        contactUploadPart(
                                request.getContactPersonNationalIdUpload(),
                                request.getContactPersonNationalIdUploadId(),
                                FileType.NATIONAL_ID,
                                transporter,
                                transporter::setContactPersonNationalIdUploadId,
                                request.getContactPersonNationalIdExpiryDate()),
                        contactUploadPart(
                                request.getContactPersonPassportUpload(),
                                request.getContactPersonPassportUploadId(),
                                FileType.PASSPORT,
                                transporter,
                                transporter::setContactPersonPassportUploadId,
                                request.getContactPersonPassportExpiryDate())),
                locale,
                true);
        if (!contactIdOutcome.success()) {
            return buildOrganizationResponseWithErrors(contactIdOutcome.errorMessages());
        }

        organizationServiceAuditable.save(transporter);

        // Update contract dates if supplied
        if (StringUtils.hasText(request.getContractStartDate())) {
            createTransporterContractLink(
                    supplier,
                    transporter,
                    request.getContractStartDate(),
                    request.getContractEndDate(),
                    username);
        }

        organizationContactPersonProvisioningSupport.syncContactPersonFromOrganization(transporter);

        OrganizationDto dto = OrganizationMapping.toDto(transporter);
        resolveTransporterContractMetadata(supplier, transporter, dto);
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.TRANSPORTER_UPDATED.getCode(), new String[]{}, locale));
        res.setOrganizationDto(dto);
        return res;
    }

    // =========================================================
    // deleteTransporter
    // =========================================================
    @Override
    public OrganizationResponse deleteTransporter(Long transporterId, Locale locale, String username) {
        Organization supplier = loadForUser(username);
        Organization transporter = requireLinkedTransportPartner(supplier, transporterId, locale);

        // Soft-delete the contract link
        contractedTransporterLinkRepository
                .findByOrganizationIdAndTransporterId(supplier.getId(), transporter.getId())
                .ifPresent(link -> {
                    link.setEntityStatus(EntityStatus.DELETED);
                    link.setModifiedAt(LocalDateTime.now());
                    link.setModifiedBy(username);
                    contractedTransporterLinkRepository.save(link);
                });

        // Soft-delete the transporter organisation if it was supplier-registered and has no remaining active links
        boolean hasOtherActiveLinks = contractedTransporterLinkRepository
                .findByTransporterIdAndEntityStatusNotOrderByLinkedAtDesc(transporter.getId(), EntityStatus.DELETED)
                .stream()
                .anyMatch(link -> !link.getOrganizationId().equals(supplier.getId()));

        if (!hasOtherActiveLinks && !Boolean.TRUE.equals(transporter.getCreatedViaSignup())) {
            transporter.setEntityStatus(EntityStatus.DELETED);
            transporter.setModifiedAt(LocalDateTime.now());
            transporter.setModifiedBy(username);
            organizationServiceAuditable.save(transporter);
        }

        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.TRANSPORTER_DELETED.getCode(), new String[]{}, locale));
        return res;
    }

    // =========================================================
    // Fleet vehicle management
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse listFleetVehicles(Locale locale, String username) {
        Organization org = loadForUser(username);
        List<FleetVehicle> vehicles = fleetVehicleRepository
                .findByOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(org.getId(), EntityStatus.DELETED);
        List<FleetVehicleDto> dtos = vehicles.stream()
                .map(vehicle -> enrichFleetVehicleDto(vehicle, locale))
                .collect(java.util.stream.Collectors.toList());
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setFleetVehicleDtoList(dtos);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse listTransporterFleetVehicles(Long transporterId, Locale locale, String username) {
        Organization caller = loadForUser(username);
        Organization partner = requireLinkedTransportPartner(caller, transporterId, locale);

        List<FleetVehicleDto> dtos = new ArrayList<>();

        if (OrganizationTradingCapabilitySupport.canActAsSupplier(caller)
                && partner.getOrganizationClassification() == OrganizationClassification.TRANSPORT_COMPANY) {
            List<FleetVehicle> partnerOwned = fleetVehicleRepository
                    .findByOrganizationIdAndOwnershipTypeAndEntityStatusNotOrderByCreatedAtDesc(
                            partner.getId(), FleetVehicleOwnershipType.OWNED.name(), EntityStatus.DELETED);
            for (FleetVehicle vehicle : partnerOwned) {
                FleetVehicleDto dto = enrichFleetVehicleDto(vehicle, locale);
                dto.setContractedTransporterOrganizationId(partner.getId());
                dto.setContractedTransporterOrganizationName(partner.getName());
                dtos.add(dto);
            }

            List<FleetVehicle> contractedTagged = fleetVehicleRepository
                    .findByOrganizationIdAndContractedTransporterOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(
                            caller.getId(), partner.getId(), EntityStatus.DELETED);
            for (FleetVehicle vehicle : contractedTagged) {
                dtos.add(enrichFleetVehicleDto(vehicle, locale));
            }
        }

        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setFleetVehicleDtoList(dtos);
        return res;
    }

    @Override
    public OrganizationResponse createFleetVehicle(CreateFleetVehicleRequest request, Locale locale, String username) {
        if (request == null || !StringUtils.hasText(request.getRegistration())
                || !StringUtils.hasText(request.getMakeModel())
                || !StringUtils.hasText(request.getVehicleType())) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_VEHICLE_VALIDATION_FAILED.getCode(), new String[]{}, locale)));
        }

        Organization org = loadForUser(username);
        OrganizationResponse ownershipError = resolveFleetOwnershipForWrite(org, request.getOwnershipType(),
                request.getContractedTransporterOrganizationId(), locale);
        if (ownershipError != null) {
            return ownershipError;
        }

        LocalDateTime now = LocalDateTime.now();
        FleetVehicleOwnershipType ownershipType = normalizeFleetOwnershipType(request.getOwnershipType());

        FleetVehicle vehicle = new FleetVehicle();
        vehicle.setOrganizationId(org.getId());
        vehicle.setOwnershipType(ownershipType.name());
        vehicle.setContractedTransporterOrganizationId(
                ownershipType == FleetVehicleOwnershipType.CONTRACTED
                        ? request.getContractedTransporterOrganizationId()
                        : null);
        vehicle.setRegistration(request.getRegistration().trim().toUpperCase());
        vehicle.setMakeModel(request.getMakeModel().trim());
        vehicle.setVehicleType(request.getVehicleType().trim().toLowerCase());
        vehicle.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus().trim().toLowerCase() : "available");
        vehicle.setDriverName(request.getDriverName());
        vehicle.setUtilizationPct(request.getUtilizationPct() != null ? request.getUtilizationPct() : java.math.BigDecimal.ZERO);
        vehicle.setLastTripAt(request.getLastTripAt());
        vehicle.setEntityStatus(EntityStatus.ACTIVE);
        vehicle.setCreatedAt(now);
        vehicle.setCreatedBy(username);

        FleetVehicle saved = fleetVehicleRepository.save(vehicle);
        dispatchFleetRegisteredNotifications(
                org,
                ownershipType,
                saved.getContractedTransporterOrganizationId(),
                saved.getRegistration(),
                saved.getMakeModel(),
                saved.getVehicleType(),
                username);

        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(201);
        res.setMessage(messageService.getMessage(I18Code.FLEET_VEHICLE_CREATED.getCode(), new String[]{}, locale));
        res.setFleetVehicleDto(enrichFleetVehicleDto(saved, locale));
        return res;
    }

    @Override
    public OrganizationResponse notifyFleetRegistered(FleetRegisteredNotificationRequest request, Locale locale) {
        if (request == null
                || request.getRegisteringOrganizationId() == null
                || request.getRegisteringOrganizationId() < 1
                || !StringUtils.hasText(request.getRegistration())) {
            OrganizationManagementResponse res = new OrganizationManagementResponse();
            res.setSuccess(true);
            res.setStatusCode(200);
            res.setMessage("Fleet registration notification skipped — insufficient data.");
            return res;
        }

        Organization registeringOrg = organizationRepository
                .findByIdAndEntityStatusNot(request.getRegisteringOrganizationId(), EntityStatus.DELETED)
                .orElse(null);
        if (registeringOrg == null) {
            log.warn(
                    "Fleet registration notification skipped — registering organisation {} not found",
                    request.getRegisteringOrganizationId());
            OrganizationManagementResponse res = new OrganizationManagementResponse();
            res.setSuccess(true);
            res.setStatusCode(200);
            res.setMessage("Fleet registration notification skipped — organisation not found.");
            return res;
        }

        FleetVehicleOwnershipType ownershipType = normalizeFleetOwnershipType(request.getOwnershipType());
        dispatchFleetRegisteredNotifications(
                registeringOrg,
                ownershipType,
                request.getContractedTransporterOrganizationId(),
                request.getRegistration(),
                request.getMakeModel(),
                request.getAssetType(),
                StringUtils.hasText(request.getPerformedBy()) ? request.getPerformedBy() : SYSTEM_MODIFIER);

        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage("Fleet registration notification dispatched.");
        return res;
    }

    @Override
    public OrganizationResponse updateFleetVehicle(Long id, EditFleetVehicleRequest request, Locale locale, String username) {
        Organization org = loadForUser(username);
        FleetVehicle vehicle = fleetVehicleRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElseThrow(() -> new projectlx.co.zw.organizationmanagement.utils.exceptions.BusinessRuleException(
                        messageService.getMessage(I18Code.FLEET_VEHICLE_NOT_FOUND.getCode(), new String[]{}, locale),
                        I18Code.FLEET_VEHICLE_NOT_FOUND));

        if (!vehicle.getOrganizationId().equals(org.getId())) {
            throw new projectlx.co.zw.organizationmanagement.utils.exceptions.BusinessRuleException(
                    messageService.getMessage(I18Code.FLEET_VEHICLE_NOT_FOUND.getCode(), new String[]{}, locale),
                    I18Code.FLEET_VEHICLE_NOT_FOUND);
        }

        if (request != null) {
            OrganizationResponse ownershipError = resolveFleetOwnershipForWrite(org, request.getOwnershipType(),
                    request.getContractedTransporterOrganizationId(), locale);
            if (ownershipError != null) {
                return ownershipError;
            }
            if (StringUtils.hasText(request.getOwnershipType())) {
                FleetVehicleOwnershipType ownershipType = normalizeFleetOwnershipType(request.getOwnershipType());
                vehicle.setOwnershipType(ownershipType.name());
                vehicle.setContractedTransporterOrganizationId(
                        ownershipType == FleetVehicleOwnershipType.CONTRACTED
                                ? request.getContractedTransporterOrganizationId()
                                : null);
            } else if (request.getContractedTransporterOrganizationId() != null
                    && FleetVehicleOwnershipType.CONTRACTED.name().equals(vehicle.getOwnershipType())) {
                OrganizationResponse ownershipErrorOnTransporter = resolveFleetOwnershipForWrite(
                        org, FleetVehicleOwnershipType.CONTRACTED.name(),
                        request.getContractedTransporterOrganizationId(), locale);
                if (ownershipErrorOnTransporter != null) {
                    return ownershipErrorOnTransporter;
                }
                vehicle.setContractedTransporterOrganizationId(request.getContractedTransporterOrganizationId());
            }
        }

        if (request != null && StringUtils.hasText(request.getRegistration())) {
            vehicle.setRegistration(request.getRegistration().trim().toUpperCase());
        }
        if (request != null && StringUtils.hasText(request.getMakeModel())) {
            vehicle.setMakeModel(request.getMakeModel().trim());
        }
        if (request != null && StringUtils.hasText(request.getVehicleType())) {
            vehicle.setVehicleType(request.getVehicleType().trim().toLowerCase());
        }
        if (request != null && StringUtils.hasText(request.getStatus())) {
            vehicle.setStatus(request.getStatus().trim().toLowerCase());
        }
        if (request != null && request.getDriverName() != null) {
            vehicle.setDriverName(request.getDriverName());
        }
        if (request != null && request.getUtilizationPct() != null) {
            vehicle.setUtilizationPct(request.getUtilizationPct());
        }
        if (request != null && request.getLastTripAt() != null) {
            vehicle.setLastTripAt(request.getLastTripAt());
        }
        vehicle.setModifiedAt(LocalDateTime.now());
        vehicle.setModifiedBy(username);

        FleetVehicle saved = fleetVehicleRepository.save(vehicle);

        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.FLEET_VEHICLE_UPDATED.getCode(), new String[]{}, locale));
        res.setFleetVehicleDto(enrichFleetVehicleDto(saved, locale));
        return res;
    }

    @Override
    public OrganizationResponse deleteFleetVehicle(Long id, Locale locale, String username) {
        Organization org = loadForUser(username);
        FleetVehicle vehicle = fleetVehicleRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElseThrow(() -> new projectlx.co.zw.organizationmanagement.utils.exceptions.BusinessRuleException(
                        messageService.getMessage(I18Code.FLEET_VEHICLE_NOT_FOUND.getCode(), new String[]{}, locale),
                        I18Code.FLEET_VEHICLE_NOT_FOUND));

        if (!vehicle.getOrganizationId().equals(org.getId())) {
            throw new projectlx.co.zw.organizationmanagement.utils.exceptions.BusinessRuleException(
                    messageService.getMessage(I18Code.FLEET_VEHICLE_NOT_FOUND.getCode(), new String[]{}, locale),
                    I18Code.FLEET_VEHICLE_NOT_FOUND);
        }

        vehicle.setEntityStatus(EntityStatus.DELETED);
        vehicle.setModifiedAt(LocalDateTime.now());
        vehicle.setModifiedBy(username);
        fleetVehicleRepository.save(vehicle);

        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        res.setMessage(messageService.getMessage(I18Code.FLEET_VEHICLE_DELETED.getCode(), new String[]{}, locale));
        return res;
    }

    private FleetVehicleDto toFleetVehicleDto(FleetVehicle vehicle) {
        FleetVehicleDto dto = new FleetVehicleDto();
        dto.setId(vehicle.getId());
        dto.setOrganizationId(vehicle.getOrganizationId());
        dto.setOwnershipType(StringUtils.hasText(vehicle.getOwnershipType())
                ? vehicle.getOwnershipType()
                : FleetVehicleOwnershipType.OWNED.name());
        dto.setContractedTransporterOrganizationId(vehicle.getContractedTransporterOrganizationId());
        dto.setRegistration(vehicle.getRegistration());
        dto.setMakeModel(vehicle.getMakeModel());
        dto.setVehicleType(vehicle.getVehicleType());
        dto.setStatus(vehicle.getStatus());
        dto.setDriverName(vehicle.getDriverName());
        dto.setUtilizationPct(vehicle.getUtilizationPct());
        dto.setLastTripAt(vehicle.getLastTripAt());
        dto.setCreatedAt(vehicle.getCreatedAt());
        dto.setCreatedBy(vehicle.getCreatedBy());
        dto.setModifiedAt(vehicle.getModifiedAt());
        dto.setModifiedBy(vehicle.getModifiedBy());
        return dto;
    }

    private FleetVehicleDto enrichFleetVehicleDto(FleetVehicle vehicle, Locale locale) {
        FleetVehicleDto dto = toFleetVehicleDto(vehicle);
        if (vehicle.getContractedTransporterOrganizationId() != null) {
            organizationRepository.findByIdAndEntityStatusNot(
                            vehicle.getContractedTransporterOrganizationId(), EntityStatus.DELETED)
                    .ifPresent(transporter -> dto.setContractedTransporterOrganizationName(transporter.getName()));
        }
        return dto;
    }

    private FleetVehicleOwnershipType normalizeFleetOwnershipType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return FleetVehicleOwnershipType.OWNED;
        }
        try {
            return FleetVehicleOwnershipType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return FleetVehicleOwnershipType.OWNED;
        }
    }

    private void dispatchFleetRegisteredNotifications(
            Organization registeringOrg,
            FleetVehicleOwnershipType ownershipType,
            Long contractedTransporterOrganizationId,
            String registration,
            String makeModel,
            String assetType,
            String performedBy) {
        if (registeringOrg == null) {
            return;
        }
        Organization transporterOrg = null;
        if (ownershipType == FleetVehicleOwnershipType.CONTRACTED
                && contractedTransporterOrganizationId != null
                && contractedTransporterOrganizationId > 0) {
            transporterOrg = organizationRepository
                    .findByIdAndEntityStatusNot(contractedTransporterOrganizationId, EntityStatus.DELETED)
                    .orElse(null);
        }
        organizationFleetNotifier.sendFleetRegistered(
                registeringOrg,
                transporterOrg,
                registration,
                makeModel,
                assetType,
                ownershipType,
                performedBy);
    }

    private OrganizationResponse resolveFleetOwnershipForWrite(
            Organization org,
            String ownershipTypeRaw,
            Long contractedTransporterOrganizationId,
            Locale locale) {
        FleetVehicleOwnershipType ownershipType = normalizeFleetOwnershipType(ownershipTypeRaw);
        if (ownershipType == FleetVehicleOwnershipType.OWNED) {
            return null;
        }
        // CONTRACTED is permitted for SUPPLIER and CUSTOMER (not TRANSPORT_COMPANY)
        if (org.getOrganizationClassification() == OrganizationClassification.TRANSPORT_COMPANY) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_VEHICLE_VALIDATION_FAILED.getCode(), new String[]{}, locale)));
        }
        if (!OrganizationTradingCapabilitySupport.canContractTransporters(org)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_VEHICLE_VALIDATION_FAILED.getCode(), new String[]{}, locale)));
        }
        if (contractedTransporterOrganizationId == null || contractedTransporterOrganizationId <= 0) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACTED_TRANSPORTER_REQUIRED.getCode(), new String[]{}, locale)));
        }
        try {
            requireLinkedTransportPartner(org, contractedTransporterOrganizationId, locale);
        } catch (RuntimeException ex) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACTED_TRANSPORTER_INVALID.getCode(), new String[]{}, locale)));
        }
        return null;
    }

    /**
     * Fleet ownership validation endpoint called by fleet-management service.
     *
     * Rules:
     * - OWNED: allowed for SUPPLIER, TRANSPORT_COMPANY, CUSTOMER
     * - CONTRACTED: allowed for SUPPLIER and CUSTOMER only; transport companies may only register OWNED assets
     */
    @Override
    public OrganizationResponse validateFleetOwnership(ValidateFleetOwnershipRequest request, Locale locale) {
        if (request == null || request.getRegisteringOrganizationId() == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_OWNERSHIP_ORG_NOT_FOUND.getCode(), new String[]{}, locale)));
        }

        Organization org = organizationRepository.findByIdAndEntityStatusNot(
                request.getRegisteringOrganizationId(), EntityStatus.DELETED).orElse(null);
        if (org == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_OWNERSHIP_ORG_NOT_FOUND.getCode(), new String[]{}, locale)));
        }

        FleetVehicleOwnershipType ownershipType = normalizeFleetOwnershipType(request.getOwnershipType());

        if (ownershipType == FleetVehicleOwnershipType.OWNED) {
            OrganizationResponse res = buildOrganizationResponse(null);
            res.setMessage(messageService.getMessage(I18Code.FLEET_OWNERSHIP_VALIDATION_OK.getCode(), new String[]{}, locale));
            return res;
        }

        // CONTRACTED: only SUPPLIER and CUSTOMER permitted
        if (org.getOrganizationClassification() == OrganizationClassification.TRANSPORT_COMPANY) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_OWNERSHIP_CONTRACTED_NOT_ALLOWED.getCode(), new String[]{}, locale)));
        }

        if (!OrganizationTradingCapabilitySupport.canContractTransporters(org)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_OWNERSHIP_CONTRACTED_NOT_ALLOWED.getCode(), new String[]{}, locale)));
        }

        if (request.getContractedTransporterOrganizationId() == null
                || request.getContractedTransporterOrganizationId() <= 0) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACTED_TRANSPORTER_REQUIRED.getCode(), new String[]{}, locale)));
        }

        try {
            requireLinkedTransportPartner(org, request.getContractedTransporterOrganizationId(), locale);
        } catch (RuntimeException ex) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACTED_TRANSPORTER_INVALID.getCode(), new String[]{}, locale)));
        }

        OrganizationResponse contractDateError = validateFleetVehicleContractDatesWithinPartnerLink(
                org,
                request.getContractedTransporterOrganizationId(),
                request.getContractScope(),
                request.getContractStartDate(),
                request.getContractEndDate(),
                locale);
        if (contractDateError != null) {
            return contractDateError;
        }

        OrganizationResponse res = buildOrganizationResponse(null);
        res.setMessage(messageService.getMessage(I18Code.FLEET_OWNERSHIP_VALIDATION_OK.getCode(), new String[]{}, locale));
        return res;
    }

    @Override
    public OrganizationResponse validateTransporterAssignment(ValidateTransporterAssignmentRequest request, Locale locale) {
        if (request == null
                || request.getShipperOrganizationId() == null
                || request.getTransportCompanyOrganizationId() == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_OWNERSHIP_ORG_NOT_FOUND.getCode(), new String[]{}, locale)));
        }

        Organization shipper = organizationRepository.findByIdAndEntityStatusNot(
                request.getShipperOrganizationId(), EntityStatus.DELETED).orElse(null);
        if (shipper == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.FLEET_OWNERSHIP_ORG_NOT_FOUND.getCode(), new String[]{}, locale)));
        }

        Long transportCompanyId = request.getTransportCompanyOrganizationId();
        if (transportCompanyId.equals(shipper.getId())) {
            OrganizationResponse res = buildOrganizationResponse(null);
            res.setMessage(messageService.getMessage(I18Code.FLEET_OWNERSHIP_VALIDATION_OK.getCode(), new String[]{}, locale));
            return res;
        }

        Organization transportCompany = organizationRepository.findByIdAndEntityStatusNot(
                transportCompanyId, EntityStatus.DELETED).orElse(null);
        if (transportCompany == null
                || transportCompany.getOrganizationClassification() != OrganizationClassification.TRANSPORT_COMPANY) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACTED_TRANSPORTER_INVALID.getCode(), new String[]{}, locale)));
        }

        if (!OrganizationTradingCapabilitySupport.canContractTransporters(shipper)) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACTED_TRANSPORTER_INVALID.getCode(), new String[]{}, locale)));
        }

        List<Organization> contracted = organizationRepository.findContractedTransportersForSupplier(
                shipper.getId(), EntityStatus.DELETED);
        if (!contracted.contains(transportCompany)) {
            return buildOrganizationResponseWithErrors(List.of(messageService.getMessage(
                    I18Code.FLEET_VEHICLE_CONTRACTED_TRANSPORTER_INVALID.getCode(), new String[]{}, locale)));
        }

        OrganizationResponse res = buildOrganizationResponse(null);
        res.setMessage(messageService.getMessage(I18Code.FLEET_OWNERSHIP_VALIDATION_OK.getCode(), new String[]{}, locale));
        return res;
    }

    @Override
    public OrganizationResponse checkCustomerRegistrationEmail(String email, Locale locale, String username) {
        if (!StringUtils.hasText(email)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[]{"email"}, locale)));
        }
        Organization supplier = loadForUser(username);
        if (!OrganizationTradingCapabilitySupport.canActAsSupplier(supplier)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_CUSTOMERS.getCode(), new String[]{}, locale)));
        }
        String normalizedEmail = email.trim().toLowerCase();
        Optional<Organization> existing = organizationRepository
                .findByEmailAndEntityStatusNot(normalizedEmail, EntityStatus.DELETED);
        OrganizationResponse response = new OrganizationResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        if (existing.isEmpty()) {
            response.setCustomerRegistrationEmailStatus(
                    projectlx.co.zw.organizationmanagement.utils.enums.CustomerRegistrationEmailStatus.AVAILABLE.name());
            response.setMessage("Email is available for new customer registration.");
            return response;
        }
        Organization target = existing.get();
        if (supplier.getCustomers().contains(target)) {
            response.setCustomerRegistrationEmailStatus(
                    projectlx.co.zw.organizationmanagement.utils.enums.CustomerRegistrationEmailStatus.ALREADY_LINKED.name());
            response.setMessage(messageService.getMessage(
                    I18Code.ORG_CUSTOMER_REGISTRATION_ALREADY_LINKED.getCode(), new String[]{}, locale));
            return response;
        }
        if (target.getOrganizationClassification() == OrganizationClassification.CUSTOMER
                || OrganizationTradingCapabilitySupport.canBeLinkedAsCustomer(target, false)) {
            if (target.getOrganizationClassification() == OrganizationClassification.SUPPLIER) {
                response.setCustomerRegistrationEmailStatus(
                        projectlx.co.zw.organizationmanagement.utils.enums.CustomerRegistrationEmailStatus.DUPLEX_OFFERED.name());
                response.setDuplexLinkOffered(true);
                response.setExistingOrganizationForLink(OrganizationMapping.toDto(target));
                response.setMessage(messageService.getMessage(
                        I18Code.ORG_CUSTOMER_REGISTRATION_DUPLEX_OFFERED.getCode(), new String[]{}, locale));
            } else {
                response.setCustomerRegistrationEmailStatus(
                        projectlx.co.zw.organizationmanagement.utils.enums.CustomerRegistrationEmailStatus.LINKABLE_CUSTOMER.name());
                response.setExistingOrganizationForLink(OrganizationMapping.toDto(target));
                response.setMessage("Existing customer organisation can be linked to your supplier account.");
            }
            return response;
        }
        response.setCustomerRegistrationEmailStatus(
                projectlx.co.zw.organizationmanagement.utils.enums.CustomerRegistrationEmailStatus.NOT_LINKABLE.name());
        response.setMessage(messageService.getMessage(
                I18Code.ORG_CUSTOMER_REGISTRATION_NOT_LINKABLE.getCode(), new String[]{}, locale));
        return response;
    }

    @Override
    public OrganizationResponse linkExistingOrganizationAsCustomer(
            projectlx.co.zw.organizationmanagement.utils.requests.LinkExistingOrganizationAsCustomerRequest request,
            Locale locale,
            String username) {
        if (request == null || request.getExistingOrganizationId() == null) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[]{"existingOrganizationId"}, locale)));
        }
        Organization supplier = loadForUser(username);
        if (!OrganizationTradingCapabilitySupport.canActAsSupplier(supplier)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_FORBIDDEN_CUSTOMERS.getCode(), new String[]{}, locale)));
        }
        Organization target = organizationRepository
                .findByIdAndEntityStatusNot(request.getExistingOrganizationId(), EntityStatus.DELETED)
                .orElseThrow(() -> notFound(locale));
        if (supplier.getCustomers().contains(target)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_CUSTOMER_REGISTRATION_ALREADY_LINKED.getCode(), new String[]{}, locale)));
        }
        boolean enableDuplex = request.getEnableDuplexMode() == null || Boolean.TRUE.equals(request.getEnableDuplexMode());
        if (!OrganizationTradingCapabilitySupport.canBeLinkedAsCustomer(target, enableDuplex)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_CUSTOMER_REGISTRATION_NOT_LINKABLE.getCode(), new String[]{}, locale)));
        }
        if (target.getOrganizationClassification() == OrganizationClassification.SUPPLIER && enableDuplex) {
            target.setDuplexMode(true);
        }
        supplier.getCustomers().add(target);
        target.getSuppliers().add(supplier);
        organizationServiceAuditable.save(target);
        organizationServiceAuditable.save(supplier);
        organizationDirectoryNotifier.sendCustomerLinked(supplier, target, username);
        OrganizationResponse response = buildOrganizationResponse(OrganizationMapping.toDto(target));
        response.setMessage(messageService.getMessage(I18Code.ORG_CUSTOMER_LINKED_SUCCESS.getCode(), new String[]{}, locale));
        return response;
    }

    private OrganizationResponse buildCustomerRegistrationConflictResponse(
            Organization existing,
            Organization supplier,
            Locale locale) {
        if (supplier.getCustomers().contains(existing)) {
            return buildOrganizationResponseWithErrors(
                    List.of(messageService.getMessage(I18Code.ORG_CUSTOMER_REGISTRATION_ALREADY_LINKED.getCode(), new String[]{}, locale)));
        }
        if (existing.getOrganizationClassification() == OrganizationClassification.SUPPLIER) {
            OrganizationResponse response = buildOrganizationResponse(null);
            response.setSuccess(false);
            response.setStatusCode(409);
            response.setDuplexLinkOffered(true);
            response.setExistingOrganizationForLink(OrganizationMapping.toDto(existing));
            response.setCustomerRegistrationEmailStatus(
                    projectlx.co.zw.organizationmanagement.utils.enums.CustomerRegistrationEmailStatus.DUPLEX_OFFERED.name());
            response.setErrorMessages(List.of(messageService.getMessage(
                    I18Code.ORG_CUSTOMER_REGISTRATION_DUPLEX_OFFERED.getCode(), new String[]{}, locale)));
            return response;
        }
        if (existing.getOrganizationClassification() == OrganizationClassification.CUSTOMER) {
            OrganizationResponse response = buildOrganizationResponse(null);
            response.setSuccess(false);
            response.setStatusCode(409);
            response.setExistingOrganizationForLink(OrganizationMapping.toDto(existing));
            response.setCustomerRegistrationEmailStatus(
                    projectlx.co.zw.organizationmanagement.utils.enums.CustomerRegistrationEmailStatus.LINKABLE_CUSTOMER.name());
            response.setErrorMessages(List.of(
                    "This organisation already exists as a customer. Link them instead of creating a duplicate."));
            return response;
        }
        return buildOrganizationResponseWithErrors(
                List.of(messageService.getMessage(I18Code.ORG_CUSTOMER_REGISTRATION_NOT_LINKABLE.getCode(), new String[]{}, locale)));
    }
}
