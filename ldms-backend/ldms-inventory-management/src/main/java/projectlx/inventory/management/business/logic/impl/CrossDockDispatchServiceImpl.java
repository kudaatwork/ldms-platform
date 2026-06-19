package projectlx.inventory.management.business.logic.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import projectlx.inventory.management.business.auditable.api.CrossDockDispatchServiceAuditable;
import projectlx.inventory.management.business.auditable.api.InventoryIntegrationCredentialServiceAuditable;
import projectlx.inventory.management.business.logic.api.CrossDockDispatchService;
import projectlx.inventory.management.business.logic.api.InventoryIntegrationCredentialService;
import projectlx.inventory.management.business.validator.api.CrossDockDispatchServiceValidator;
import projectlx.inventory.management.model.CrossDockDispatch;
import projectlx.inventory.management.model.CrossDockDispatchStatus;
import projectlx.inventory.management.model.InventoryIntegrationCredential;
import projectlx.inventory.management.model.IntegrationCredentialStatus;
import projectlx.inventory.management.repository.CrossDockDispatchRepository;
import projectlx.inventory.management.utils.dtos.CrossDockDispatchDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.DispatchIngestRequest;
import projectlx.inventory.management.utils.requests.GrvCallbackRequest;
import projectlx.inventory.management.utils.responses.CrossDockDispatchResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class CrossDockDispatchServiceImpl implements CrossDockDispatchService {

    private final CrossDockDispatchRepository repository;
    private final CrossDockDispatchServiceAuditable auditable;
    private final CrossDockDispatchServiceValidator validator;
    private final InventoryIntegrationCredentialService credentialService;
    private final InventoryIntegrationCredentialServiceAuditable credentialAuditable;
    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String INVENTORY_EXCHANGE    = "inventory.exchange";
    private static final String CROSS_DOCK_CREATED_RK = "cross.dock.dispatch.created";

    // ============================================================
    // INGEST DISPATCH
    // ============================================================

    @Override
    @Transactional
    public CrossDockDispatchResponse ingestDispatch(DispatchIngestRequest request, Locale locale) {

        String message;

        // STEP 1: Validate request
        ValidatorDto validatorDto = validator.isDispatchIngestRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_DISPATCH_INGEST_INVALID_REQUEST.getCode(), locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        // STEP 2: Resolve credential by api_key
        Optional<InventoryIntegrationCredential> credentialOptional =
                credentialService.resolveByApiKey(request.getApiKey());

        if (credentialOptional.isEmpty()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_NOT_FOUND.getCode(), locale);
            return buildResponse(401, false, message, null, null, null);
        }

        InventoryIntegrationCredential credential = credentialOptional.get();

        if (credential.getStatus() != IntegrationCredentialStatus.ACTIVE) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_SUSPENDED.getCode(), locale);
            return buildResponse(403, false, message, null, null, null);
        }

        // STEP 3: Idempotency — reject duplicate external_dispatch_id for same org
        Optional<CrossDockDispatch> existing = repository
                .findByExternalDispatchIdAndOrganizationIdAndEntityStatusNot(
                        request.getExternalDispatchId(), credential.getOrganizationId(),
                        EntityStatus.DELETED);

        if (existing.isPresent()) {
            CrossDockDispatch dup = existing.get();
            message = messageService.getMessage(
                    I18Code.MESSAGE_DISPATCH_INGEST_DUPLICATE.getCode(), locale);
            log.warn("Duplicate dispatch ingest [externalId={}] for org [{}] — returning existing record [id={}]",
                    request.getExternalDispatchId(), credential.getOrganizationId(), dup.getId());
            CrossDockDispatchResponse dupResponse = buildResponse(
                    200, true, message, mapToDto(dup), null, null);
            dupResponse.setDispatchId(dup.getId());
            dupResponse.setShipmentNumber(dup.getShipmentNumber());
            return dupResponse;
        }

        // STEP 4: Serialize en-route depot labels as JSON array
        String enRouteJson = null;
        if (request.getEnRouteDepotLabels() != null && !request.getEnRouteDepotLabels().isEmpty()) {
            try {
                enRouteJson = objectMapper.writeValueAsString(request.getEnRouteDepotLabels());
            } catch (Exception ex) {
                log.warn("Could not serialize enRouteDepotLabels: {}", ex.getMessage());
                enRouteJson = request.getEnRouteDepotLabels().toString();
            }
        }

        // STEP 5: Create cross-dock dispatch record
        CrossDockDispatch dispatch = new CrossDockDispatch();
        dispatch.setOrganizationId(credential.getOrganizationId());
        dispatch.setExternalDispatchId(request.getExternalDispatchId());
        dispatch.setProductCode(request.getProductCode());
        dispatch.setExternalProductId(request.getExternalProductId());
        dispatch.setQuantity(request.getQuantity());
        dispatch.setFromLocationLabel(request.getFromLocationLabel());
        dispatch.setToLocationLabel(request.getToLocationLabel());
        dispatch.setCustomerReference(request.getCustomerReference());
        dispatch.setEnRouteDepotLabels(enRouteJson);
        dispatch.setStatus(CrossDockDispatchStatus.PENDING);
        dispatch.setIntegrationCredentialId(credential.getId());
        dispatch.setEntityStatus(EntityStatus.ACTIVE);
        dispatch.setCreatedBy("api-key:" + credential.getId());

        CrossDockDispatch saved = auditable.create(dispatch, locale, "api-key:" + credential.getId());

        // STEP 6: Update last_used_at on credential
        credential.setLastUsedAt(LocalDateTime.now());
        credentialAuditable.update(credential, locale, "system");

        // STEP 7: Publish cross.dock.dispatch.created event
        try {
            Map<String, Object> event = Map.of(
                    "dispatchId",          saved.getId(),
                    "organizationId",      saved.getOrganizationId(),
                    "externalDispatchId",  saved.getExternalDispatchId(),
                    "productCode",         saved.getProductCode() != null ? saved.getProductCode() : "",
                    "quantity",            saved.getQuantity(),
                    "fromLocationLabel",   saved.getFromLocationLabel() != null ? saved.getFromLocationLabel() : "",
                    "toLocationLabel",     saved.getToLocationLabel() != null ? saved.getToLocationLabel() : ""
            );
            rabbitTemplate.convertAndSend(INVENTORY_EXCHANGE, CROSS_DOCK_CREATED_RK, event);
            log.info("Published {} event for dispatch [id={}]", CROSS_DOCK_CREATED_RK, saved.getId());
        } catch (Exception ex) {
            log.error("Failed to publish cross-dock dispatch event for id [{}]: {}", saved.getId(), ex.getMessage());
        }

        message = messageService.getMessage(
                I18Code.MESSAGE_DISPATCH_INGEST_CREATED_SUCCESSFULLY.getCode(), locale);
        log.info("Cross-dock dispatch created [id={}] for org [{}]",
                saved.getId(), saved.getOrganizationId());

        CrossDockDispatchResponse response = buildResponse(201, true, message, mapToDto(saved), null, null);
        response.setDispatchId(saved.getId());
        response.setShipmentNumber(saved.getShipmentNumber());
        return response;
    }

    // ============================================================
    // GRV CALLBACK
    // ============================================================

    @Override
    @Transactional
    public CrossDockDispatchResponse handleGrvCallback(GrvCallbackRequest request, Locale locale) {

        // STEP 1: Resolve credential
        Optional<InventoryIntegrationCredential> credentialOptional =
                credentialService.resolveByApiKey(request.getApiKey());

        if (credentialOptional.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_NOT_FOUND.getCode(), locale);
            return buildResponse(401, false, message, null, null, null);
        }

        InventoryIntegrationCredential credential = credentialOptional.get();

        // STEP 2: Log GRV callback and update credential last_used_at
        log.info("GRV callback received [grvId={}, grvNumber={}, shipmentId={}, shipmentNumber={}] for org [{}]",
                request.getGrvId(), request.getGrvNumber(),
                request.getShipmentId(), request.getShipmentNumber(),
                credential.getOrganizationId());

        credential.setLastUsedAt(LocalDateTime.now());
        credentialAuditable.update(credential, locale, "system");

        // STEP 3: Push GRV callback to partner webhook (best-effort, non-blocking on failure)
        if (credential.getCallbackGrvUrl() != null && !credential.getCallbackGrvUrl().isBlank()) {
            pushGrvCallbackToPartner(credential, request);
        }

        String message = messageService.getMessage(
                I18Code.MESSAGE_DISPATCH_GRV_CALLBACK_RECEIVED.getCode(), locale);
        return buildResponse(200, true, message, null, null, null);
    }

    // ============================================================
    // PARTNER WEBHOOK
    // ============================================================

    /**
     * HTTP POST the GRV callback payload to the partner's registered callbackGrvUrl.
     * Best-effort — logs failures but does not propagate exceptions.
     */
    public void pushGrvCallbackToPartner(InventoryIntegrationCredential credential,
                                         GrvCallbackRequest callbackRequest) {
        String url = credential.getCallbackGrvUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(callbackRequest);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);
            log.info("GRV callback successfully pushed to partner webhook [{}] for grvId [{}]",
                    url, callbackRequest.getGrvId());
        } catch (Exception ex) {
            log.error("Failed to push GRV callback to partner webhook [{}] for grvId [{}]: {}",
                    url, callbackRequest.getGrvId(), ex.getMessage());
        }
    }

    // ============================================================
    // QUERY
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CrossDockDispatchResponse findById(Long id, Locale locale, String username) {

        Optional<CrossDockDispatch> optional = repository.findByIdAndEntityStatusNot(
                id, EntityStatus.DELETED);

        if (optional.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CROSS_DOCK_DISPATCH_NOT_FOUND.getCode(), locale);
            return buildResponse(404, false, message, null, null, null);
        }

        String message = messageService.getMessage(
                I18Code.MESSAGE_CROSS_DOCK_DISPATCH_RETRIEVED_SUCCESSFULLY.getCode(), locale);
        return buildResponse(200, true, message, mapToDto(optional.get()), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CrossDockDispatchResponse findAllByOrganization(Long organizationId, Locale locale, String username) {

        List<CrossDockDispatchDto> dtoList = repository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        String message = messageService.getMessage(
                I18Code.MESSAGE_CROSS_DOCK_DISPATCH_RETRIEVED_SUCCESSFULLY.getCode(), locale);
        return buildResponse(200, true, message, null, dtoList, null);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private CrossDockDispatchDto mapToDto(CrossDockDispatch dispatch) {
        return modelMapper.map(dispatch, CrossDockDispatchDto.class);
    }

    private CrossDockDispatchResponse buildResponse(
            int statusCode, boolean success, String message,
            CrossDockDispatchDto dto,
            List<CrossDockDispatchDto> dtoList,
            List<String> errorMessages) {

        CrossDockDispatchResponse response = new CrossDockDispatchResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(success);
        response.setMessage(message);
        response.setCrossDockDispatchDto(dto);
        response.setCrossDockDispatchDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }
}
