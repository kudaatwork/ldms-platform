package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.PurchaseRequisitionServiceAuditable;
import projectlx.inventory.management.business.logic.api.PurchaseRequisitionService;
import projectlx.inventory.management.business.logic.support.OrganizationFunctionalCurrencySupport;
import projectlx.inventory.management.business.logic.support.ProcurementApprovalStageResolver;
import projectlx.inventory.management.business.validator.api.PurchaseRequisitionServiceValidator;
import org.springframework.util.StringUtils;
import projectlx.inventory.management.model.*;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.PurchaseOrderLineRepository;
import projectlx.inventory.management.repository.PurchaseOrderRepository;
import projectlx.inventory.management.repository.PurchaseRequisitionAmendmentRepository;
import projectlx.inventory.management.repository.PurchaseRequisitionLineRepository;
import projectlx.inventory.management.repository.PurchaseRequisitionRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.specification.PurchaseRequisitionSpecification;
import projectlx.inventory.management.utils.dtos.PurchaseRequisitionDto;
import projectlx.inventory.management.utils.dtos.PurchaseRequisitionLineDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.*;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.inventory.management.utils.responses.PurchaseRequisitionResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequisitionServiceImpl implements PurchaseRequisitionService {

    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final PurchaseRequisitionLineRepository purchaseRequisitionLineRepository;
    private final PurchaseRequisitionAmendmentRepository purchaseRequisitionAmendmentRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final PurchaseRequisitionServiceAuditable purchaseRequisitionServiceAuditable;
    private final PurchaseRequisitionServiceValidator validator;
    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final OrganizationFunctionalCurrencySupport organizationFunctionalCurrencySupport;
    private final ProcurementApprovalStageResolver procurementApprovalStageResolver;

    // === CRUD OPERATIONS ===

    @Override
    public PurchaseRequisitionResponse create(CreatePurchaseRequisitionRequest request, Locale locale, String username) {
        log.info("Creating purchase requisition for user: {}", username);

        ValidatorDto validatorDto = validator.isCreatePurchaseRequisitionRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        PurchaseRequisition pr = new PurchaseRequisition();
        pr.setRequisitionNumber(generateRequisitionNumber());
        pr.setOrganizationId(request.getOrganizationId());
        pr.setDepartmentId(request.getDepartmentId());
        pr.setRequestedByUserId(request.getRequestedByUserId() != null ? request.getRequestedByUserId() : request.getCreatedByUserId());
        pr.setPurpose(request.getPurpose());
        pr.setJustification(request.getJustification());
        pr.setPriority(request.getPriority() != null ? request.getPriority() : PriorityLevel.NORMAL);
        pr.setRequisitionDate(request.getRequisitionDate() != null ? request.getRequisitionDate() : LocalDate.now());
        pr.setRequiredByDate(request.getRequiredByDate());
        pr.setExpiryDate(request.getExpiryDate());
        pr.setDefaultFulfillmentMethod(request.getDefaultFulfillmentMethod());
        pr.setTargetWarehouseId(request.getTargetWarehouseId());
        pr.setPreferredSupplierId(request.getPreferredSupplierId());
        if (StringUtils.hasText(request.getCurrency())) {
            pr.setCurrency(request.getCurrency());
        } else if (request.getOrganizationId() != null) {
            pr.setCurrency(organizationFunctionalCurrencySupport.resolveFunctionalCurrency(request.getOrganizationId()));
        }
        pr.setBudgetAvailable(request.getBudgetAvailable());
        pr.setBudgetCode(request.getBudgetCode());
        pr.setCostCenter(request.getCostCenter());
        pr.setProjectCode(request.getProjectCode());
        pr.setNotes(request.getNotes());
        pr.setCreatedByUserId(request.getCreatedByUserId());
        pr.setStatus(PurchaseRequisitionStatus.DRAFT);

        // Create lines
        if (request.getLines() != null) {
            int lineNumber = 1;
            for (var lineRequest : request.getLines()) {
                PurchaseRequisitionLine line = new PurchaseRequisitionLine();
                line.setPurchaseRequisition(pr);
                line.setLineNumber(lineNumber++);
                line.setProduct(productRepository.findById(lineRequest.getProductId()).orElse(null));
                line.setProductDescription(lineRequest.getProductDescription());
                line.setUnitOfMeasure(lineRequest.getUnitOfMeasure());
                line.setRequestedQuantity(lineRequest.getRequestedQuantity());
                line.setEstimatedUnitPrice(lineRequest.getEstimatedUnitPrice());
                line.setFulfillmentMethod(lineRequest.getFulfillmentMethod() != null ? lineRequest.getFulfillmentMethod() : request.getDefaultFulfillmentMethod());
                line.setSpecifications(lineRequest.getSpecifications());
                line.setPreferredBrand(lineRequest.getPreferredBrand());
                line.setIsSubstituteAcceptable(lineRequest.getIsSubstituteAcceptable());
                line.setCreatedByUserId(request.getCreatedByUserId());
                pr.getLines().add(line);
            }
        }

        pr.calculateEstimatedTotal();

        PurchaseRequisition savedPr = purchaseRequisitionServiceAuditable.create(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(201, true, message, mapToDto(savedPr), null, null);
    }

    @Override
    public PurchaseRequisitionResponse findById(Long id, Locale locale, String username) {
        log.info("Finding purchase requisition by id: {} for user: {}", id, username);

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponseWithErrors(400, false, "Invalid ID", null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findById(id)
                .filter(pr -> pr.getEntityStatus() != EntityStatus.DELETED);

        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, mapToDto(prOpt.get()), null, null);
    }

    @Override
    public PurchaseRequisitionResponse findAllAsList(Locale locale, String username) {
        log.info("Finding all purchase requisitions for user: {}", username);

        List<PurchaseRequisition> prs = purchaseRequisitionRepository.findAll()
                .stream()
                .filter(pr -> pr.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        List<PurchaseRequisitionDto> dtos = prs.stream().map(this::mapToDto).collect(Collectors.toList());

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponseList(200, true, message, dtos);
    }

    @Override
    public PurchaseRequisitionResponse update(EditPurchaseRequisitionRequest request, String username, Locale locale) {
        log.info("Updating purchase requisition for user: {}", username);

        ValidatorDto validatorDto = validator.isEditPurchaseRequisitionRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponseWithErrors(400, false, "Invalid request", null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisition pr = prOpt.get();

        ValidatorDto editableDto = validator.isEditableStatus(pr, locale);
        if (!editableDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, editableDto.getErrorMessages());
        }

        // Update fields
        if (request.getDepartmentId() != null) pr.setDepartmentId(request.getDepartmentId());
        if (request.getPurpose() != null) pr.setPurpose(request.getPurpose());
        if (request.getJustification() != null) pr.setJustification(request.getJustification());
        if (request.getPriority() != null) pr.setPriority(request.getPriority());
        if (request.getRequiredByDate() != null) pr.setRequiredByDate(request.getRequiredByDate());
        if (request.getExpiryDate() != null) pr.setExpiryDate(request.getExpiryDate());
        if (request.getDefaultFulfillmentMethod() != null) pr.setDefaultFulfillmentMethod(request.getDefaultFulfillmentMethod());
        if (request.getTargetWarehouseId() != null) pr.setTargetWarehouseId(request.getTargetWarehouseId());
        if (request.getPreferredSupplierId() != null) pr.setPreferredSupplierId(request.getPreferredSupplierId());
        if (request.getCurrency() != null) pr.setCurrency(request.getCurrency());
        if (request.getBudgetAvailable() != null) pr.setBudgetAvailable(request.getBudgetAvailable());
        if (request.getBudgetCode() != null) pr.setBudgetCode(request.getBudgetCode());
        if (request.getCostCenter() != null) pr.setCostCenter(request.getCostCenter());
        if (request.getProjectCode() != null) pr.setProjectCode(request.getProjectCode());
        if (request.getNotes() != null) pr.setNotes(request.getNotes());
        pr.setUpdatedByUserId(request.getUpdatedByUserId());

        pr.calculateEstimatedTotal();

        PurchaseRequisition savedPr = purchaseRequisitionServiceAuditable.update(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, mapToDto(savedPr), null, null);
    }

    @Override
    public PurchaseRequisitionResponse delete(Long id, Locale locale, String username) {
        log.info("Deleting purchase requisition by id: {} for user: {}", id, username);

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponseWithErrors(400, false, "Invalid ID", null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisition pr = prOpt.get();

        ValidatorDto editableDto = validator.isEditableStatus(pr, locale);
        if (!editableDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, editableDto.getErrorMessages());
        }

        pr.setEntityStatus(EntityStatus.DELETED);
        purchaseRequisitionServiceAuditable.delete(pr, locale);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, null, null, null);
    }

    @Override
    public PurchaseRequisitionResponse findByMultipleFilters(PurchaseRequisitionMultipleFiltersRequest request, String username, Locale locale) {
        log.info("Finding purchase requisitions by filters for user: {}", username);

        ValidatorDto validatorDto = validator.isMultipleFiltersRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponseWithErrors(400, false, "Invalid request", null, null, validatorDto.getErrorMessages());
        }

        Specification<PurchaseRequisition> spec = Specification.where(PurchaseRequisitionSpecification.deleted());

        if (request.getOrganizationId() != null) {
            spec = spec.and(PurchaseRequisitionSpecification.organizationIdEquals(request.getOrganizationId()));
        }
        if (request.getDepartmentId() != null) {
            spec = spec.and(PurchaseRequisitionSpecification.departmentIdEquals(request.getDepartmentId()));
        }
        if (request.getRequestedByUserId() != null) {
            spec = spec.and(PurchaseRequisitionSpecification.requestedByUserIdEquals(request.getRequestedByUserId()));
        }
        if (request.getStatus() != null) {
            spec = spec.and(PurchaseRequisitionSpecification.statusEquals(request.getStatus()));
        }
        if (request.getPriority() != null) {
            spec = spec.and(PurchaseRequisitionSpecification.priorityEquals(request.getPriority()));
        }
        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            spec = spec.and(PurchaseRequisitionSpecification.any(request.getSearchTerm()));
        }

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<PurchaseRequisition> prPage = purchaseRequisitionRepository.findAll(spec, pageable);
        Page<PurchaseRequisitionDto> dtoPage = prPage.map(this::mapToDto);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponsePage(200, true, message, dtoPage);
    }

    // === WORKFLOW OPERATIONS ===

    @Override
    public PurchaseRequisitionResponse submit(Long id, Long submittedByUserId, Locale locale, String username) {
        log.info("Submitting purchase requisition id: {} by user: {}", id, username);

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisition pr = prOpt.get();

        ValidatorDto submittableDto = validator.isSubmittable(pr, locale);
        if (!submittableDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_SUBMITTABLE.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, submittableDto.getErrorMessages());
        }

        // Set required approval stages from resolver (org-level or platform default)
        if (pr.getRequiredApprovalStages() == null) {
            pr.setRequiredApprovalStages(
                    procurementApprovalStageResolver.resolveRequiredStages(pr.getOrganizationId()));
        }

        pr.setStatus(PurchaseRequisitionStatus.SUBMITTED);
        pr.setSubmittedAt(LocalDateTime.now());
        pr.setSubmittedByUserId(submittedByUserId);

        PurchaseRequisition savedPr = purchaseRequisitionServiceAuditable.update(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_SUBMITTED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, mapToDto(savedPr), null, null);
    }

    @Override
    public PurchaseRequisitionResponse approve(ApprovePurchaseRequisitionRequest request, Locale locale, String username) {
        log.info("Approving purchase requisition id: {} by user: {}", request.getId(), username);

        ValidatorDto validatorDto = validator.isApprovePurchaseRequisitionRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponseWithErrors(400, false, "Invalid request", null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisition pr = prOpt.get();

        ValidatorDto approvableDto = validator.isApprovable(pr, locale);
        if (!approvableDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_APPROVABLE.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, approvableDto.getErrorMessages());
        }

        pr.setStatus(PurchaseRequisitionStatus.APPROVED);
        pr.setApprovedAt(LocalDateTime.now());
        pr.setApprovedByUserId(request.getApprovedByUserId());
        pr.setApprovalNotes(request.getApprovalNotes());

        // Apply line-level adjustments
        if (request.getLineApprovals() != null) {
            for (var lineApproval : request.getLineApprovals()) {
                pr.getLines().stream()
                        .filter(l -> l.getId().equals(lineApproval.getLineId()))
                        .findFirst()
                        .ifPresent(line -> {
                            line.setApprovedQuantity(lineApproval.getApprovedQuantity() != null ? lineApproval.getApprovedQuantity() : line.getRequestedQuantity());
                            if (lineApproval.getFulfillmentMethod() != null) {
                                line.setFulfillmentMethod(lineApproval.getFulfillmentMethod());
                            }
                            if (lineApproval.getQuantityAdjustmentReason() != null) {
                                line.setQuantityAdjustmentReason(lineApproval.getQuantityAdjustmentReason());
                            }
                            line.calculateRemainingQuantity();
                        });
            }
        } else {
            // Default: approve all quantities as requested
            for (var line : pr.getLines()) {
                if (line.getApprovedQuantity() == null) {
                    line.setApprovedQuantity(line.getRequestedQuantity());
                }
                line.calculateRemainingQuantity();
            }
        }

        PurchaseRequisition savedPr = purchaseRequisitionServiceAuditable.update(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_APPROVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, mapToDto(savedPr), null, null);
    }

    @Override
    public PurchaseRequisitionResponse reject(RejectPurchaseRequisitionRequest request, Locale locale, String username) {
        log.info("Rejecting purchase requisition id: {} by user: {}", request.getId(), username);

        ValidatorDto validatorDto = validator.isRejectPurchaseRequisitionRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponseWithErrors(400, false, "Invalid request", null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisition pr = prOpt.get();

        ValidatorDto rejectableDto = validator.isRejectable(pr, locale);
        if (!rejectableDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_REJECTABLE.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, rejectableDto.getErrorMessages());
        }

        pr.setStatus(PurchaseRequisitionStatus.REJECTED);
        pr.setRejectedAt(LocalDateTime.now());
        pr.setRejectedByUserId(request.getRejectedByUserId());
        pr.setRejectionReason(request.getRejectionReason());

        PurchaseRequisition savedPr = purchaseRequisitionServiceAuditable.update(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_REJECTED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, mapToDto(savedPr), null, null);
    }

    @Override
    public PurchaseRequisitionResponse cancel(CancelPurchaseRequisitionRequest request, Locale locale, String username) {
        log.info("Cancelling purchase requisition id: {} by user: {}", request.getId(), username);

        ValidatorDto validatorDto = validator.isCancelPurchaseRequisitionRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponseWithErrors(400, false, "Invalid request", null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisition pr = prOpt.get();

        ValidatorDto cancellableDto = validator.isCancellable(pr, locale);
        if (!cancellableDto.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_CANCELLABLE.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, cancellableDto.getErrorMessages());
        }

        pr.setStatus(PurchaseRequisitionStatus.CANCELLED);
        pr.setCancelledAt(LocalDateTime.now());
        pr.setCancelledByUserId(request.getCancelledByUserId());
        pr.setCancellationReason(request.getCancellationReason());

        PurchaseRequisition savedPr = purchaseRequisitionServiceAuditable.update(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_CANCELLED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, mapToDto(savedPr), null, null);
    }

    @Override
    public PurchaseRequisitionResponse close(Long id, Long closedByUserId, String reason, Locale locale, String username) {
        log.info("Closing purchase requisition id: {} by user: {}", id, username);

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisition pr = prOpt.get();
        pr.setStatus(PurchaseRequisitionStatus.CLOSED);
        pr.setUpdatedByUserId(closedByUserId);

        // Create amendment for closure
        PurchaseRequisitionAmendment amendment = new PurchaseRequisitionAmendment();
        amendment.setPurchaseRequisition(pr);
        amendment.setAmendmentNumber(purchaseRequisitionAmendmentRepository.getNextAmendmentNumber(id));
        amendment.setAmendmentType("CLOSURE");
        amendment.setDescription("PR administratively closed");
        amendment.setReason(reason);
        amendment.setCreatedByUserId(closedByUserId);
        purchaseRequisitionServiceAuditable.createAmendment(amendment, locale, username);

        PurchaseRequisition savedPr = purchaseRequisitionServiceAuditable.update(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_CLOSED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, mapToDto(savedPr), null, null);
    }

    // === FULFILLMENT OPERATIONS ===

    @Override
    public PurchaseRequisitionResponse fulfillLine(FulfillPurchaseRequisitionLineRequest request, Locale locale, String username) {
        log.info("Fulfilling PR line id: {} for user: {}", request.getLineId(), username);

        ValidatorDto validatorDto = validator.isFulfillPurchaseRequisitionLineRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponseWithErrors(400, false, "Invalid request", null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(request.getPurchaseRequisitionId(), EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisition pr = prOpt.get();

        if (pr.getStatus() != PurchaseRequisitionStatus.APPROVED && pr.getStatus() != PurchaseRequisitionStatus.PARTIALLY_FULFILLED) {
            String message = messageService.getMessage(I18Code.MESSAGE_FULFILL_PURCHASE_REQUISITION_NOT_APPROVED.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        Optional<PurchaseRequisitionLine> lineOpt = pr.getLines().stream()
                .filter(l -> l.getId().equals(request.getLineId()))
                .findFirst();

        if (lineOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_FULFILL_PURCHASE_REQUISITION_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseRequisitionLine line = lineOpt.get();

        if (request.getQuantity().compareTo(line.getRemainingQuantity()) > 0) {
            String message = messageService.getMessage(I18Code.MESSAGE_FULFILL_PURCHASE_REQUISITION_QUANTITY_EXCEEDS_REMAINING.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        // Record fulfillment based on method
        switch (request.getFulfillmentMethod()) {
            case FROM_STOCK:
                line.recordStockFulfillment(request.getQuantity());
                break;
            case TRANSFER:
                line.recordTransferFulfillment(request.getQuantity());
                break;
            case PURCHASE:
                line.recordPurchaseOrderFulfillment(request.getQuantity());
                break;
            default:
                break;
        }

        line.setFulfillmentMethod(request.getFulfillmentMethod());
        if (request.getFulfillmentNotes() != null) {
            line.setFulfillmentNotes(request.getFulfillmentNotes());
        }

        // Update PR status based on fulfillment
        if (pr.isFullyFulfilled()) {
            pr.setStatus(PurchaseRequisitionStatus.FULFILLED);
        } else if (pr.isPartiallyFulfilled()) {
            pr.setStatus(PurchaseRequisitionStatus.PARTIALLY_FULFILLED);
        }

        PurchaseRequisition savedPr = purchaseRequisitionServiceAuditable.update(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_LINE_FULFILLED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, mapToDto(savedPr), null, null);
    }

    // === PO CONVERSION ===

    @Override
    public PurchaseOrderResponse createPurchaseOrderFromPR(CreatePOFromPurchaseRequisitionRequest request, Locale locale, String username) {
        log.info("Creating PO from PR id: {} for user: {}", request.getPurchaseRequisitionId(), username);

        ValidatorDto validatorDto = validator.isCreatePOFromPRRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return buildPOResponseWithErrors(400, false, "Invalid request", validatorDto.getErrorMessages());
        }

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository.findByIdAndEntityStatusNot(request.getPurchaseRequisitionId(), EntityStatus.DELETED);
        if (prOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildPOResponse(404, false, message, null);
        }

        PurchaseRequisition pr = prOpt.get();

        if (pr.getStatus() != PurchaseRequisitionStatus.CUSTOMER_ACKNOWLEDGED
                && pr.getStatus() != PurchaseRequisitionStatus.APPROVED
                && pr.getStatus() != PurchaseRequisitionStatus.PARTIALLY_FULFILLED) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_PO_FROM_PR_NOT_APPROVED.getCode(), new String[]{}, locale);
            return buildPOResponse(400, false, message, null);
        }

        // Get eligible lines
        List<PurchaseRequisitionLine> eligibleLines = purchaseRequisitionLineRepository.findLinesEligibleForPurchaseOrder(pr.getId(), EntityStatus.DELETED);
        if (eligibleLines.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_PO_FROM_PR_NO_ELIGIBLE_LINES.getCode(), new String[]{}, locale);
            return buildPOResponse(400, false, message, null);
        }

        // Create PO
        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseOrderNumber(generatePONumber());
        po.setPurchaseRequisitionId(pr.getId());
        po.setOrganizationId(pr.getOrganizationId());
        po.setExternalId(request.getExternalId());
        po.setSupplierId(request.getSupplierId());
        po.setBuyerContact(request.getBuyerContact());
        po.setSupplierContact(request.getSupplierContact());
        po.setCurrency(request.getCurrency() != null ? request.getCurrency() : pr.getCurrency());
        po.setPaymentTerm(request.getPaymentTerm());
        po.setPaymentDueDate(request.getPaymentDueDate());
        po.setTaxRate(request.getTaxRate());
        po.setShipFromLocationId(request.getShipFromLocationId());
        po.setShipToLocationId(request.getShipToLocationId());
        Long receivingWarehouseId = request.getReceivingWarehouseId() != null
                ? request.getReceivingWarehouseId()
                : pr.getTargetWarehouseId();
        if (receivingWarehouseId == null) {
            String message = messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_RECEIVING_WAREHOUSE_ID_REQUIRED.getCode(), new String[]{}, locale);
            return buildPOResponse(400, false, message, null);
        }
        boolean receivingIsCustomer = warehouseLocationRepository.findById(receivingWarehouseId)
                .filter(location -> location.getEntityStatus() != EntityStatus.DELETED)
                .filter(location -> location.getWarehouseType() == WarehouseLocationType.CUSTOMER)
                .isPresent();
        if (!receivingIsCustomer) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_RECEIVING_WAREHOUSE_INVALID_TYPE.getCode(), new String[]{}, locale);
            return buildPOResponse(400, false, message, null);
        }
        po.setReceivingWarehouseId(receivingWarehouseId);
        po.setFreightTerms(request.getFreightTerms());
        po.setShipMode(request.getShipMode() != null ? request.getShipMode() : ShipMode.ROAD);
        po.setShippingInstructions(request.getShippingInstructions());
        po.setExpectedDate(request.getExpectedDate());
        po.setNotes(request.getNotes());
        po.setCreatedByUserId(request.getCreatedByUserId());
        po.setStatus(PurchaseOrderStatus.DRAFT);

        // Create PO lines from PR lines
        int lineNumber = 1;
        for (var prLine : eligibleLines) {
            BigDecimal qtyToOrder = prLine.getRemainingQuantity();

            // Check if specific line selection
            if (request.getLineSelections() != null) {
                var selection = request.getLineSelections().stream()
                        .filter(s -> s.getPrLineId().equals(prLine.getId()))
                        .findFirst();
                if (selection.isEmpty()) continue;
                qtyToOrder = selection.get().getQuantityToOrder();
            }

            if (qtyToOrder.compareTo(BigDecimal.ZERO) <= 0) continue;

            PurchaseOrderLine poLine = new PurchaseOrderLine();
            poLine.setPurchaseOrder(po);
            poLine.setLineNumber(lineNumber++);
            poLine.setPurchaseRequisitionLineId(prLine.getId());
            poLine.setProduct(prLine.getProduct());
            poLine.setUnitOfMeasure(prLine.getUnitOfMeasure());
            poLine.setQuantity(qtyToOrder);
            poLine.setUnitPrice(prLine.getEstimatedUnitPrice() != null ? prLine.getEstimatedUnitPrice() : BigDecimal.ZERO);
            poLine.setCreatedByUserId(request.getCreatedByUserId());
            po.getPurchaseOrderLines().add(poLine);

            // Update PR line ordered quantity
            prLine.recordPurchaseOrderFulfillment(qtyToOrder);
        }

        po.calculateTotals();
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        // Update PR status
        if (pr.isFullyFulfilled()) {
            pr.setStatus(PurchaseRequisitionStatus.FULFILLED);
        } else if (pr.isPartiallyFulfilled()) {
            pr.setStatus(PurchaseRequisitionStatus.PARTIALLY_FULFILLED);
        }
        purchaseRequisitionServiceAuditable.update(pr, locale, username);

        String message = messageService.getMessage(I18Code.MESSAGE_CREATE_PO_FROM_PR_SUCCESSFUL.getCode(), new String[]{}, locale);
        return buildPOResponse(201, true, message, savedPo);
    }

    // === UTILITY OPERATIONS ===

    @Override
    public PurchaseRequisitionResponse findByDepartment(Long departmentId, Locale locale, String username) {
        List<PurchaseRequisition> prs = purchaseRequisitionRepository.findByDepartmentIdAndStatusAndEntityStatusNot(
                departmentId, null, EntityStatus.DELETED);
        List<PurchaseRequisitionDto> dtos = prs.stream().map(this::mapToDto).collect(Collectors.toList());
        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponseList(200, true, message, dtos);
    }

    @Override
    public PurchaseRequisitionResponse findPendingApprovals(Long organizationId, Locale locale, String username) {
        List<PurchaseRequisition> prs = purchaseRequisitionRepository.findPendingApprovalsByOrganization(organizationId, EntityStatus.DELETED);
        List<PurchaseRequisitionDto> dtos = prs.stream().map(this::mapToDto).collect(Collectors.toList());
        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponseList(200, true, message, dtos);
    }

    @Override
    public PurchaseRequisitionResponse findApprovedPendingFulfillment(Long organizationId, Locale locale, String username) {
        List<PurchaseRequisition> prs = purchaseRequisitionRepository.findApprovedPendingFulfillment(organizationId, EntityStatus.DELETED);
        List<PurchaseRequisitionDto> dtos = prs.stream().map(this::mapToDto).collect(Collectors.toList());
        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponseList(200, true, message, dtos);
    }

    @Override
    public void expireOverdueRequisitions() {
        log.info("Expiring overdue purchase requisitions");
        List<PurchaseRequisition> expiredPrs = purchaseRequisitionRepository.findExpired(LocalDate.now(), EntityStatus.DELETED);
        for (PurchaseRequisition pr : expiredPrs) {
            pr.setStatus(PurchaseRequisitionStatus.EXPIRED);
            purchaseRequisitionRepository.save(pr);
        }
        log.info("Expired {} purchase requisitions", expiredPrs.size());
    }

    // === HELPER METHODS ===

    private String generateRequisitionNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PR-" + year + "-" + uuid;
    }

    private String generatePONumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PO-" + year + "-" + uuid;
    }

    private PurchaseRequisitionDto mapToDto(PurchaseRequisition pr) {
        TypeMap<PurchaseRequisition, PurchaseRequisitionDto> typeMap =
                modelMapper.getTypeMap(PurchaseRequisition.class, PurchaseRequisitionDto.class);
        if (typeMap == null) {
            typeMap = modelMapper.createTypeMap(PurchaseRequisition.class, PurchaseRequisitionDto.class);
            typeMap.addMappings(mapper -> mapper.skip(PurchaseRequisitionDto::setLines));
        }
        PurchaseRequisitionDto dto = typeMap.map(pr);

        if (pr.getLines() != null) {
            List<PurchaseRequisitionLineDto> lineDtos = pr.getLines().stream()
                    .filter(l -> l.getEntityStatus() != EntityStatus.DELETED)
                    .map(this::mapLineToDto)
                    .collect(Collectors.toList());
            dto.setLines(lineDtos);

            // Compute summary
            dto.setTotalLines(lineDtos.size());
            dto.setFulfilledLines((int) lineDtos.stream().filter(l -> l.getRemainingQuantity() != null && l.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0).count());
            dto.setPartiallyFulfilledLines((int) lineDtos.stream().filter(l -> {
                if (l.getRemainingQuantity() == null || l.getApprovedQuantity() == null) return false;
                BigDecimal fulfilled = l.getApprovedQuantity().subtract(l.getRemainingQuantity());
                return fulfilled.compareTo(BigDecimal.ZERO) > 0 && l.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0;
            }).count());
            dto.setPendingLines(dto.getTotalLines() - dto.getFulfilledLines() - dto.getPartiallyFulfilledLines());
        }

        return dto;
    }

    private PurchaseRequisitionLineDto mapLineToDto(PurchaseRequisitionLine line) {
        PurchaseRequisitionLineDto dto = new PurchaseRequisitionLineDto();
        dto.setId(line.getId());
        if (line.getPurchaseRequisition() != null) {
            dto.setPurchaseRequisitionId(line.getPurchaseRequisition().getId());
        }
        dto.setLineNumber(line.getLineNumber());
        if (line.getProduct() != null) {
            dto.setProductId(line.getProduct().getId());
            dto.setProductName(line.getProduct().getName());
            dto.setProductCode(line.getProduct().getProductCode());
        }
        dto.setProductDescription(line.getProductDescription());
        dto.setUnitOfMeasure(line.getUnitOfMeasure());
        dto.setRequestedQuantity(line.getRequestedQuantity());
        dto.setApprovedQuantity(line.getApprovedQuantity());
        dto.setOrderedQuantity(line.getOrderedQuantity());
        dto.setFulfilledFromStockQuantity(line.getFulfilledFromStockQuantity());
        dto.setFulfilledFromTransferQuantity(line.getFulfilledFromTransferQuantity());
        dto.setRemainingQuantity(line.getRemainingQuantity());
        dto.setEstimatedUnitPrice(line.getEstimatedUnitPrice());
        dto.setEstimatedTotalPrice(line.getEstimatedTotalPrice());
        dto.setFulfillmentMethod(line.getFulfillmentMethod());
        dto.setFulfillmentNotes(line.getFulfillmentNotes());
        dto.setSpecifications(line.getSpecifications());
        dto.setPreferredBrand(line.getPreferredBrand());
        dto.setIsSubstituteAcceptable(line.getIsSubstituteAcceptable());
        dto.setQuantityAdjustmentReason(line.getQuantityAdjustmentReason());
        dto.setCreatedByUserId(line.getCreatedByUserId());
        dto.setUpdatedByUserId(line.getUpdatedByUserId());
        dto.setCreatedAt(line.getCreatedAt());
        dto.setUpdatedAt(line.getUpdatedAt());
        dto.setEntityStatus(line.getEntityStatus());
        return dto;
    }

    private PurchaseRequisitionResponse buildResponse(int status, boolean success, String message, PurchaseRequisitionDto dto, Page<PurchaseRequisitionDto> page, List<String> errors) {
        PurchaseRequisitionResponse response = new PurchaseRequisitionResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setPurchaseRequisitionDto(dto);
        response.setPurchaseRequisitionDtoPage(page);
        response.setErrorMessages(errors);
        return response;
    }

    private PurchaseRequisitionResponse buildResponseList(int status, boolean success, String message, List<PurchaseRequisitionDto> dtos) {
        PurchaseRequisitionResponse response = new PurchaseRequisitionResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setPurchaseRequisitionDtoList(dtos);
        return response;
    }

    private PurchaseRequisitionResponse buildResponsePage(int status, boolean success, String message, Page<PurchaseRequisitionDto> page) {
        PurchaseRequisitionResponse response = new PurchaseRequisitionResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setPurchaseRequisitionDtoPage(page);
        return response;
    }

    private PurchaseRequisitionResponse buildResponseWithErrors(int status, boolean success, String message, PurchaseRequisitionDto dto, Page<PurchaseRequisitionDto> page, List<String> errors) {
        PurchaseRequisitionResponse response = new PurchaseRequisitionResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setPurchaseRequisitionDto(dto);
        response.setPurchaseRequisitionDtoPage(page);
        response.setErrorMessages(errors);
        return response;
    }

    private PurchaseOrderResponse buildPOResponse(int status, boolean success, String message, PurchaseOrder po) {
        PurchaseOrderResponse response = new PurchaseOrderResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        return response;
    }

    private PurchaseOrderResponse buildPOResponseWithErrors(int status, boolean success, String message, List<String> errors) {
        PurchaseOrderResponse response = new PurchaseOrderResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
