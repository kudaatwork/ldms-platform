package projectlx.shipment.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.shipment.management.business.auditable.api.BorderClearanceCaseServiceAuditable;
import projectlx.shipment.management.business.logic.api.BorderClearanceCaseService;
import projectlx.shipment.management.business.logic.support.BorderClearanceCaseMapper;
import projectlx.shipment.management.business.validator.api.BorderClearanceCaseServiceValidator;
import projectlx.shipment.management.clients.TripTrackingServiceClient;
import projectlx.shipment.management.model.BorderClearanceCase;
import projectlx.shipment.management.model.BorderClearanceDocument;
import projectlx.shipment.management.repository.BorderClearanceCaseRepository;
import projectlx.shipment.management.repository.BorderClearanceDocumentRepository;
import projectlx.shipment.management.utils.dtos.BorderClearanceCaseDto;
import projectlx.shipment.management.utils.enums.BorderClearanceDocumentType;
import projectlx.shipment.management.utils.enums.BorderClearanceStatus;
import projectlx.shipment.management.utils.enums.I18Code;
import projectlx.shipment.management.utils.requests.AddBorderClearanceDocumentRequest;
import projectlx.shipment.management.utils.requests.BorderClearanceMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.ClearBorderCaseRequest;
import projectlx.shipment.management.utils.requests.RecordTripSystemEventFeignRequest;
import projectlx.shipment.management.utils.requests.RejectBorderCaseRequest;
import projectlx.shipment.management.utils.responses.BorderClearanceCaseResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class BorderClearanceCaseServiceImpl implements BorderClearanceCaseService {

    private final BorderClearanceCaseServiceValidator borderClearanceCaseServiceValidator;
    private final BorderClearanceCaseServiceAuditable borderClearanceCaseServiceAuditable;
    private final BorderClearanceCaseRepository borderClearanceCaseRepository;
    private final BorderClearanceDocumentRepository borderClearanceDocumentRepository;
    private final TripTrackingServiceClient tripTrackingServiceClient;
    private final MessageService messageService;

    // ============================================================
    // QUERIES
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public BorderClearanceCaseResponse findById(Long id, Locale locale, String username) {

        // ============================================================
        // STEP 1: Load the case
        // ============================================================
        BorderClearanceCase bc = borderClearanceCaseRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (bc == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_CASE_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Load associated documents
        // ============================================================
        List<BorderClearanceDocument> documents = borderClearanceDocumentRepository
                .findAllByCaseIdAndEntityStatusNot(id, EntityStatus.DELETED);

        BorderClearanceCaseResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_BORDER_CLEARANCE_CASE_FIND_SUCCESS.getCode(), new String[]{}, locale));
        response.setBorderClearanceCaseDto(BorderClearanceCaseMapper.toCaseDtoWithDocuments(bc, documents));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BorderClearanceCaseResponse findByMultipleFilters(BorderClearanceMultipleFiltersRequest request,
                                                              Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate the request
        // ============================================================
        ValidatorDto validation = borderClearanceCaseServiceValidator.isMultipleFiltersRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale), validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Apply filters and query
        // ============================================================
        List<BorderClearanceCase> cases;

        if (request != null && request.getShipmentId() != null) {
            cases = borderClearanceCaseRepository.findAllByShipmentIdAndEntityStatusNot(
                    request.getShipmentId(), EntityStatus.DELETED);
        } else if (request != null && request.getOrganizationId() != null) {
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                BorderClearanceStatus statusFilter = resolveStatusFilter(request.getStatus());
                if (statusFilter != null) {
                    cases = borderClearanceCaseRepository
                            .findAllByOrganizationIdAndStatusAndEntityStatusNot(
                                    request.getOrganizationId(), statusFilter, EntityStatus.DELETED);
                } else {
                    cases = borderClearanceCaseRepository.findAllByOrganizationIdAndEntityStatusNot(
                            request.getOrganizationId(), EntityStatus.DELETED);
                }
            } else {
                cases = borderClearanceCaseRepository.findAllByOrganizationIdAndEntityStatusNot(
                        request.getOrganizationId(), EntityStatus.DELETED);
            }
        } else {
            cases = new ArrayList<>();
        }

        List<BorderClearanceCaseDto> dtoList = cases.stream()
                .map(bc -> {
                    List<BorderClearanceDocument> docs = borderClearanceDocumentRepository
                            .findAllByCaseIdAndEntityStatusNot(bc.getId(), EntityStatus.DELETED);
                    return BorderClearanceCaseMapper.toCaseDtoWithDocuments(bc, docs);
                })
                .collect(Collectors.toList());

        BorderClearanceCaseResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_BORDER_CLEARANCE_CASE_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setBorderClearanceCaseDtoList(dtoList);
        return response;
    }

    // ============================================================
    // COMMANDS
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BorderClearanceCaseResponse addDocument(AddBorderClearanceDocumentRequest request,
                                                    Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate the request
        // ============================================================
        ValidatorDto validation = borderClearanceCaseServiceValidator.isAddDocumentRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale), validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load the case and verify it is in an editable state
        // ============================================================
        BorderClearanceCase bc = borderClearanceCaseRepository
                .findByIdAndEntityStatusNot(request.getCaseId(), EntityStatus.DELETED).orElse(null);
        if (bc == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_CASE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (bc.getStatus() == BorderClearanceStatus.CLEARED || bc.getStatus() == BorderClearanceStatus.REJECTED) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_CASE_CLOSED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Persist the document
        // ============================================================
        BorderClearanceDocument doc = new BorderClearanceDocument();
        doc.setCaseId(bc.getId());
        doc.setDocumentType(BorderClearanceDocumentType.valueOf(request.getDocumentType().trim().toUpperCase()));
        doc.setFileUploadId(request.getFileUploadId());
        doc.setFileName(request.getFileName().trim());
        doc.setDescription(request.getDescription());
        doc.setEntityStatus(EntityStatus.ACTIVE);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setCreatedBy(username);

        BorderClearanceDocument saved = borderClearanceCaseServiceAuditable.createDocument(doc, locale, username);
        log.info("Border clearance document added: docId={} caseId={} by={}", saved.getId(), bc.getId(), username);

        // ============================================================
        // STEP 4: Return the updated case (with all docs)
        // ============================================================
        List<BorderClearanceDocument> allDocs = borderClearanceDocumentRepository
                .findAllByCaseIdAndEntityStatusNot(bc.getId(), EntityStatus.DELETED);

        BorderClearanceCaseResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_BORDER_CLEARANCE_DOCUMENT_ADD_SUCCESS.getCode(), new String[]{}, locale));
        response.setBorderClearanceCaseDto(BorderClearanceCaseMapper.toCaseDtoWithDocuments(bc, allDocs));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BorderClearanceCaseResponse submit(Long id, Locale locale, String username) {

        // ============================================================
        // STEP 1: Load and validate state transition
        // ============================================================
        BorderClearanceCase bc = borderClearanceCaseRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (bc == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_CASE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (bc.getStatus() != BorderClearanceStatus.AWAITING_DOCUMENTS) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_INVALID_STATUS_TRANSITION.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Transition to SUBMITTED
        // ============================================================
        bc.setStatus(BorderClearanceStatus.SUBMITTED);
        bc.setModifiedAt(LocalDateTime.now());
        bc.setModifiedBy(username);
        BorderClearanceCase saved = borderClearanceCaseServiceAuditable.updateCase(bc, locale, username);
        log.info("Border clearance case submitted: caseId={} by={}", saved.getId(), username);

        return caseDtoResponse(saved, locale, I18Code.MESSAGE_BORDER_CLEARANCE_CASE_SUBMIT_SUCCESS);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BorderClearanceCaseResponse clear(Long id, ClearBorderCaseRequest request,
                                              Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate the request
        // ============================================================
        ValidatorDto validation = borderClearanceCaseServiceValidator.isClearBorderCaseRequestValid(
                id, request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale), validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load and verify the case is not already closed
        // ============================================================
        BorderClearanceCase bc = borderClearanceCaseRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (bc == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_CASE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (bc.getStatus() == BorderClearanceStatus.CLEARED || bc.getStatus() == BorderClearanceStatus.REJECTED) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_CASE_CLOSED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Transition to CLEARED
        // ============================================================
        bc.setStatus(BorderClearanceStatus.CLEARED);
        bc.setClearedAt(LocalDateTime.now());
        bc.setClearedBy(username);
        if (request != null && request.getBorderName() != null && !request.getBorderName().isBlank()) {
            bc.setBorderName(request.getBorderName().trim());
        }
        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            bc.setNotes(request.getNotes());
        }
        bc.setModifiedAt(LocalDateTime.now());
        bc.setModifiedBy(username);
        BorderClearanceCase saved = borderClearanceCaseServiceAuditable.updateCase(bc, locale, username);
        log.info("Border clearance case cleared: caseId={} by={}", saved.getId(), username);

        // ============================================================
        // STEP 4: Notify trip-tracking if a trip is linked
        // ============================================================
        if (saved.getTripId() != null) {
            notifyTripBorderCleared(saved.getTripId(), saved.getCaseNumber(), locale);
        }

        return caseDtoResponse(saved, locale, I18Code.MESSAGE_BORDER_CLEARANCE_CASE_CLEAR_SUCCESS);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BorderClearanceCaseResponse reject(Long id, RejectBorderCaseRequest request,
                                               Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate the request
        // ============================================================
        ValidatorDto validation = borderClearanceCaseServiceValidator.isRejectBorderCaseRequestValid(
                id, request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale), validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load and verify the case is not already closed
        // ============================================================
        BorderClearanceCase bc = borderClearanceCaseRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (bc == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_CASE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (bc.getStatus() == BorderClearanceStatus.CLEARED || bc.getStatus() == BorderClearanceStatus.REJECTED) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_CASE_CLOSED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Transition to REJECTED
        // ============================================================
        bc.setStatus(BorderClearanceStatus.REJECTED);
        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            bc.setNotes(request.getNotes());
        }
        bc.setModifiedAt(LocalDateTime.now());
        bc.setModifiedBy(username);
        BorderClearanceCase saved = borderClearanceCaseServiceAuditable.updateCase(bc, locale, username);
        log.info("Border clearance case rejected: caseId={} by={}", saved.getId(), username);

        return caseDtoResponse(saved, locale, I18Code.MESSAGE_BORDER_CLEARANCE_CASE_REJECT_SUCCESS);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void autoCreateForShipment(Long shipmentId, Long organizationId, Long inventoryTransferId,
                                      Long salesOrderId, Locale locale, String username) {

        // ============================================================
        // STEP 1: Idempotency — skip if case already exists for this shipment
        // ============================================================
        boolean alreadyExists = borderClearanceCaseRepository.existsByShipmentIdAndEntityStatusNot(
                shipmentId, EntityStatus.DELETED);
        if (alreadyExists) {
            log.info("Border clearance case already exists for shipmentId={}; skipping.", shipmentId);
            return;
        }

        // ============================================================
        // STEP 2: Create the case in AWAITING_DOCUMENTS status
        // ============================================================
        BorderClearanceCase bc = new BorderClearanceCase();
        bc.setCaseNumber(generateCaseNumber());
        bc.setOrganizationId(organizationId);
        bc.setShipmentId(shipmentId);
        bc.setInventoryTransferId(inventoryTransferId);
        bc.setSalesOrderId(salesOrderId);
        bc.setStatus(BorderClearanceStatus.AWAITING_DOCUMENTS);
        bc.setEntityStatus(EntityStatus.ACTIVE);
        bc.setCreatedAt(LocalDateTime.now());
        bc.setCreatedBy(username);

        BorderClearanceCase saved = borderClearanceCaseServiceAuditable.createCase(bc, locale, username);
        log.info("Border clearance case auto-created: id={} number={} shipmentId={}",
                saved.getId(), saved.getCaseNumber(), shipmentId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void linkTripId(Long shipmentId, Long tripId, Locale locale, String username) {
        if (shipmentId == null || tripId == null) {
            return;
        }

        borderClearanceCaseRepository.findByShipmentIdAndEntityStatusNot(shipmentId, EntityStatus.DELETED)
                .ifPresentOrElse(bc -> {
                    if (tripId.equals(bc.getTripId())) {
                        return;
                    }
                    bc.setTripId(tripId);
                    bc.setModifiedAt(LocalDateTime.now());
                    bc.setModifiedBy(username);
                    borderClearanceCaseServiceAuditable.updateCase(bc, locale, username);
                    log.info("Linked tripId={} to border clearance case id={} for shipmentId={}",
                            tripId, bc.getId(), shipmentId);
                }, () -> log.debug("No border clearance case found for shipmentId={}; skipping trip link.", shipmentId));
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private void notifyTripBorderCleared(Long tripId, String caseNumber, Locale locale) {
        try {
            RecordTripSystemEventFeignRequest req = new RecordTripSystemEventFeignRequest();
            req.setTripId(tripId);
            req.setEventType("BORDER_CLEARED");
            req.setNotes("Border clearance case " + caseNumber + " cleared.");
            tripTrackingServiceClient.recordSystemEvent(req, locale);
            log.info("Notified trip-tracking of BORDER_CLEARED for tripId={}", tripId);
        } catch (Exception ex) {
            log.error("Failed to notify trip-tracking of BORDER_CLEARED for tripId={}: {}", tripId, ex.getMessage());
        }
    }

    private String generateCaseNumber() {
        return "BC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private BorderClearanceStatus resolveStatusFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return BorderClearanceStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown border clearance status filter: {}", raw);
            return null;
        }
    }

    private BorderClearanceCaseResponse caseDtoResponse(BorderClearanceCase bc, Locale locale, I18Code messageCode) {
        List<BorderClearanceDocument> docs = borderClearanceDocumentRepository
                .findAllByCaseIdAndEntityStatusNot(bc.getId(), EntityStatus.DELETED);
        BorderClearanceCaseResponse response = successResponse(200, messageService.getMessage(
                messageCode.getCode(), new String[]{}, locale));
        response.setBorderClearanceCaseDto(BorderClearanceCaseMapper.toCaseDtoWithDocuments(bc, docs));
        return response;
    }

    private BorderClearanceCaseResponse successResponse(int statusCode, String message) {
        BorderClearanceCaseResponse response = new BorderClearanceCaseResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private BorderClearanceCaseResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private BorderClearanceCaseResponse errorResponse(int statusCode, String message, List<String> errors) {
        BorderClearanceCaseResponse response = new BorderClearanceCaseResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
