package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.InventoryIntegrationCredentialServiceAuditable;
import projectlx.inventory.management.business.logic.api.InventoryIntegrationCredentialService;
import projectlx.inventory.management.business.validator.api.InventoryIntegrationCredentialServiceValidator;
import projectlx.inventory.management.model.InventoryIntegrationCredential;
import projectlx.inventory.management.model.IntegrationCredentialStatus;
import projectlx.inventory.management.repository.InventoryIntegrationCredentialRepository;
import projectlx.inventory.management.utils.dtos.InventoryIntegrationCredentialDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.requests.EditInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.responses.InventoryIntegrationCredentialResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class InventoryIntegrationCredentialServiceImpl implements InventoryIntegrationCredentialService {

    private final InventoryIntegrationCredentialRepository repository;
    private final InventoryIntegrationCredentialServiceAuditable auditable;
    private final InventoryIntegrationCredentialServiceValidator validator;
    private final ModelMapper modelMapper;
    private final MessageService messageService;

    // ============================================================
    // CREATE
    // ============================================================

    @Override
    @Transactional
    public InventoryIntegrationCredentialResponse create(
            CreateInventoryIntegrationCredentialRequest request, Locale locale, String username) {

        String message;

        // STEP 1: Validate request
        ValidatorDto validatorDto = validator.isCreateCredentialRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_INVALID_REQUEST.getCode(), locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        // STEP 2: Generate unique api_key (UUID-based, 64 chars via two UUIDs)
        String apiKey = generateUniqueApiKey();

        // STEP 3: Build and persist credential
        InventoryIntegrationCredential credential = new InventoryIntegrationCredential();
        credential.setOrganizationId(request.getOrganizationId());
        credential.setCredentialLabel(request.getCredentialLabel());
        credential.setApiKey(apiKey);
        credential.setWebhookUrl(request.getWebhookUrl());
        credential.setCallbackGrvUrl(request.getCallbackGrvUrl());
        credential.setStatus(IntegrationCredentialStatus.ACTIVE);
        credential.setEntityStatus(EntityStatus.ACTIVE);
        credential.setCreatedBy(username);

        InventoryIntegrationCredential saved = auditable.create(credential, locale, username);

        message = messageService.getMessage(
                I18Code.MESSAGE_INTEGRATION_CREDENTIAL_CREATED_SUCCESSFULLY.getCode(), locale);
        log.info("Integration credential created [id={}] for org [{}] by user [{}]",
                saved.getId(), saved.getOrganizationId(), username);

        return buildResponse(201, true, message, mapToDto(saved), null, null);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    @Transactional
    public InventoryIntegrationCredentialResponse update(
            EditInventoryIntegrationCredentialRequest request, Locale locale, String username) {

        String message;

        // STEP 1: Validate
        ValidatorDto validatorDto = validator.isEditCredentialRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_UPDATE_INVALID.getCode(), locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        // STEP 2: Load existing
        Optional<InventoryIntegrationCredential> optional =
                repository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (optional.isEmpty()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_NOT_FOUND.getCode(), locale);
            return buildResponse(404, false, message, null, null, null);
        }

        // STEP 3: Apply changes
        InventoryIntegrationCredential credential = optional.get();
        if (request.getCredentialLabel() != null && !request.getCredentialLabel().isBlank()) {
            credential.setCredentialLabel(request.getCredentialLabel());
        }
        if (request.getWebhookUrl() != null) {
            credential.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getCallbackGrvUrl() != null) {
            credential.setCallbackGrvUrl(request.getCallbackGrvUrl());
        }
        if (request.getStatus() != null) {
            credential.setStatus(request.getStatus());
        }
        credential.setModifiedBy(username);
        credential.setModifiedAt(LocalDateTime.now());

        InventoryIntegrationCredential saved = auditable.update(credential, locale, username);

        message = messageService.getMessage(
                I18Code.MESSAGE_INTEGRATION_CREDENTIAL_UPDATED_SUCCESSFULLY.getCode(), locale);
        log.info("Integration credential updated [id={}] by user [{}]", saved.getId(), username);

        return buildResponse(200, true, message, mapToDto(saved), null, null);
    }

    // ============================================================
    // QUERY
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public InventoryIntegrationCredentialResponse findById(Long id, Locale locale, String username) {

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<InventoryIntegrationCredential> optional =
                repository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (optional.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_NOT_FOUND.getCode(), locale);
            return buildResponse(404, false, message, null, null, null);
        }

        String message = messageService.getMessage(
                I18Code.MESSAGE_INTEGRATION_CREDENTIAL_RETRIEVED_SUCCESSFULLY.getCode(), locale);
        return buildResponse(200, true, message, mapToDto(optional.get()), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryIntegrationCredentialResponse findAllByOrganization(
            Long organizationId, Locale locale, String username) {

        List<InventoryIntegrationCredentialDto> dtoList = repository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        String message = messageService.getMessage(
                I18Code.MESSAGE_INTEGRATION_CREDENTIAL_RETRIEVED_SUCCESSFULLY.getCode(), locale);
        return buildResponse(200, true, message, null, dtoList, null);
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Override
    @Transactional
    public InventoryIntegrationCredentialResponse delete(Long id, Locale locale, String username) {

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<InventoryIntegrationCredential> optional =
                repository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (optional.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_NOT_FOUND.getCode(), locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryIntegrationCredential credential = optional.get();
        credential.setEntityStatus(EntityStatus.DELETED);
        credential.setModifiedBy(username);
        credential.setModifiedAt(LocalDateTime.now());

        auditable.delete(credential, locale);
        log.info("Integration credential soft-deleted [id={}] by user [{}]", id, username);

        String message = messageService.getMessage(
                I18Code.MESSAGE_INTEGRATION_CREDENTIAL_DELETED_SUCCESSFULLY.getCode(), locale);
        return buildResponse(200, true, message, null, null, null);
    }

    // ============================================================
    // SYSTEM RESOLVE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryIntegrationCredential> resolveByApiKey(String apiKey) {
        return repository.findByApiKeyAndEntityStatusNot(apiKey, EntityStatus.DELETED);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private String generateUniqueApiKey() {
        String candidate;
        do {
            candidate = UUID.randomUUID().toString().replace("-", "") +
                        UUID.randomUUID().toString().replace("-", "").substring(0, 32);
            candidate = candidate.substring(0, 64);
        } while (repository.existsByApiKey(candidate));
        return candidate;
    }

    private InventoryIntegrationCredentialDto mapToDto(InventoryIntegrationCredential credential) {
        return modelMapper.map(credential, InventoryIntegrationCredentialDto.class);
    }

    private InventoryIntegrationCredentialResponse buildResponse(
            int statusCode, boolean success, String message,
            InventoryIntegrationCredentialDto dto,
            List<InventoryIntegrationCredentialDto> dtoList,
            List<String> errorMessages) {

        InventoryIntegrationCredentialResponse response = new InventoryIntegrationCredentialResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(success);
        response.setMessage(message);
        response.setInventoryIntegrationCredentialDto(dto);
        response.setInventoryIntegrationCredentialDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }
}
