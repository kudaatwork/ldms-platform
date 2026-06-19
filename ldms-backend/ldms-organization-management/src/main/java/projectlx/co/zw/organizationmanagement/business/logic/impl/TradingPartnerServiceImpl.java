package projectlx.co.zw.organizationmanagement.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.business.auditable.api.TradingPartnerServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.logic.api.OrganizationService;
import projectlx.co.zw.organizationmanagement.business.logic.api.TradingPartnerService;
import projectlx.co.zw.organizationmanagement.business.validation.api.TradingPartnerServiceValidator;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.TradingPartner;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;
import projectlx.co.zw.organizationmanagement.repository.TradingPartnerRepository;
import projectlx.co.zw.organizationmanagement.utils.dtos.TradingPartnerDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.TradingPartnerMapping;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class TradingPartnerServiceImpl implements TradingPartnerService {

    private final TradingPartnerRepository tradingPartnerRepository;
    private final OrganizationRepository organizationRepository;
    private final TradingPartnerServiceAuditable tradingPartnerServiceAuditable;
    private final TradingPartnerServiceValidator tradingPartnerServiceValidator;
    private final MessageService messageService;

    // =========================================================
    // LIST
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public OrganizationManagementResponse list(Locale locale, String username) {

        // ============================================================
        // STEP 1: Resolve organisation for authenticated user
        // ============================================================
        Organization org = resolveOrgForUser(username);
        if (org == null) {
            return buildError(I18Code.ORG_NOT_FOUND.getCode(), locale);
        }

        // ============================================================
        // STEP 2: Load all active trading partners
        // ============================================================
        List<TradingPartner> partners = tradingPartnerRepository
                .findByOrganizationAndEntityStatusNot(org, EntityStatus.DELETED);

        log.info("Listed {} trading partners for org={} user={}", partners.size(), org.getId(), username);

        List<TradingPartnerDto> dtos = TradingPartnerMapping.toDtos(partners);
        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setTradingPartnerDtoList(dtos);
        return response;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrganizationManagementResponse create(CreateTradingPartnerRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = tradingPartnerServiceValidator.validateCreate(request, locale);
        if (!validation.getSuccess()) {
            OrganizationManagementResponse response = new OrganizationManagementResponse();
            response.setSuccess(false);
            response.setStatusCode(400);
            response.setErrorMessages(validation.getErrorMessages());
            return response;
        }

        // ============================================================
        // STEP 2: Resolve owning organisation
        // ============================================================
        Organization org = resolveOrgForUser(username);
        if (org == null) {
            return buildError(I18Code.ORG_NOT_FOUND.getCode(), locale);
        }

        // ============================================================
        // STEP 3: Build and persist entity
        // ============================================================
        TradingPartner partner = new TradingPartner();
        partner.setOrganization(org);
        partner.setPartnerRole(request.getPartnerRole());
        partner.setName(request.getName().trim());
        if (StringUtils.hasText(request.getEmail())) {
            partner.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(request.getPhone())) {
            partner.setPhone(request.getPhone().trim());
        }
        partner.setLocationId(request.getLocationId());
        partner.setNotes(request.getNotes());
        if (request.getLinkedOrganizationId() != null) {
            partner.setLinkedOrganizationId(request.getLinkedOrganizationId());
            partner.setRecordOnly(false);
        } else {
            partner.setRecordOnly(true);
        }
        partner.setEntityStatus(EntityStatus.ACTIVE);
        partner.setCreatedAt(LocalDateTime.now());
        partner.setCreatedBy(username);

        TradingPartner saved = tradingPartnerServiceAuditable.save(partner);
        log.info("Created trading partner id={} role={} org={} user={}", saved.getId(), saved.getPartnerRole(), org.getId(), username);

        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(true);
        response.setStatusCode(201);
        response.setMessage(messageService.getMessage(I18Code.TRADING_PARTNER_CREATED.getCode(), new String[]{}, locale));
        response.setTradingPartnerDto(TradingPartnerMapping.toDto(saved));
        return response;
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrganizationManagementResponse update(Long id, UpdateTradingPartnerRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request + id
        // ============================================================
        ValidatorDto idValidation = tradingPartnerServiceValidator.validateId(id, locale);
        if (!idValidation.getSuccess()) {
            return buildValidationError(idValidation, locale);
        }
        ValidatorDto reqValidation = tradingPartnerServiceValidator.validateUpdate(request, locale);
        if (!reqValidation.getSuccess()) {
            return buildValidationError(reqValidation, locale);
        }

        // ============================================================
        // STEP 2: Resolve owning organisation
        // ============================================================
        Organization org = resolveOrgForUser(username);
        if (org == null) {
            return buildError(I18Code.ORG_NOT_FOUND.getCode(), locale);
        }

        // ============================================================
        // STEP 3: Load trading partner (scoped to organisation)
        // ============================================================
        Optional<TradingPartner> partnerOpt = tradingPartnerRepository
                .findByIdAndOrganizationAndEntityStatusNot(id, org, EntityStatus.DELETED);
        if (partnerOpt.isEmpty()) {
            return buildError(I18Code.TRADING_PARTNER_NOT_FOUND.getCode(), locale);
        }

        // ============================================================
        // STEP 4: Apply updates and persist
        // ============================================================
        TradingPartner partner = partnerOpt.get();
        if (request.getPartnerRole() != null) {
            partner.setPartnerRole(request.getPartnerRole());
        }
        if (StringUtils.hasText(request.getName())) {
            partner.setName(request.getName().trim());
        }
        if (request.getEmail() != null) {
            partner.setEmail(StringUtils.hasText(request.getEmail())
                    ? request.getEmail().trim().toLowerCase(Locale.ROOT) : null);
        }
        if (request.getPhone() != null) {
            partner.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        }
        if (request.getLocationId() != null) {
            partner.setLocationId(request.getLocationId());
        }
        if (request.getNotes() != null) {
            partner.setNotes(request.getNotes());
        }
        if (request.getLinkedOrganizationId() != null) {
            partner.setLinkedOrganizationId(request.getLinkedOrganizationId());
            partner.setRecordOnly(false);
        }
        partner.setModifiedAt(LocalDateTime.now());
        partner.setModifiedBy(username);

        TradingPartner saved = tradingPartnerServiceAuditable.save(partner);
        log.info("Updated trading partner id={} org={} user={}", saved.getId(), org.getId(), username);

        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(I18Code.TRADING_PARTNER_UPDATED.getCode(), new String[]{}, locale));
        response.setTradingPartnerDto(TradingPartnerMapping.toDto(saved));
        return response;
    }

    // =========================================================
    // DELETE (soft)
    // =========================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrganizationManagementResponse delete(Long id, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate id
        // ============================================================
        ValidatorDto idValidation = tradingPartnerServiceValidator.validateId(id, locale);
        if (!idValidation.getSuccess()) {
            return buildValidationError(idValidation, locale);
        }

        // ============================================================
        // STEP 2: Resolve owning organisation
        // ============================================================
        Organization org = resolveOrgForUser(username);
        if (org == null) {
            return buildError(I18Code.ORG_NOT_FOUND.getCode(), locale);
        }

        // ============================================================
        // STEP 3: Load trading partner (scoped to organisation)
        // ============================================================
        Optional<TradingPartner> partnerOpt = tradingPartnerRepository
                .findByIdAndOrganizationAndEntityStatusNot(id, org, EntityStatus.DELETED);
        if (partnerOpt.isEmpty()) {
            return buildError(I18Code.TRADING_PARTNER_NOT_FOUND.getCode(), locale);
        }

        // ============================================================
        // STEP 4: Soft-delete
        // ============================================================
        TradingPartner partner = partnerOpt.get();
        partner.setEntityStatus(EntityStatus.DELETED);
        partner.setModifiedAt(LocalDateTime.now());
        partner.setModifiedBy(username);
        tradingPartnerServiceAuditable.save(partner);

        log.info("Deleted trading partner id={} org={} user={}", id, org.getId(), username);

        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(I18Code.TRADING_PARTNER_DELETED.getCode(), new String[]{}, locale));
        return response;
    }

    // =========================================================
    // Helpers
    // =========================================================

    /**
     * Resolves the owning organisation for the authenticated username.
     * The username is the contact person's email address (stored as org email on the JWT subject).
     */
    private Organization resolveOrgForUser(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        String principal = username.trim().toLowerCase(Locale.ROOT);
        return organizationRepository.findByEmailAndEntityStatusNot(principal, EntityStatus.DELETED)
                .orElse(null);
    }

    private OrganizationManagementResponse buildError(String i18nCode, Locale locale) {
        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(false);
        response.setStatusCode(404);
        response.setErrorMessages(List.of(messageService.getMessage(i18nCode, new String[]{}, locale)));
        return response;
    }

    private OrganizationManagementResponse buildValidationError(ValidatorDto validatorDto, Locale locale) {
        OrganizationManagementResponse response = new OrganizationManagementResponse();
        response.setSuccess(false);
        response.setStatusCode(400);
        response.setErrorMessages(validatorDto.getErrorMessages());
        return response;
    }
}
