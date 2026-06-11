package projectlx.billing.payments.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import projectlx.billing.payments.business.auditable.api.DriverExpenseReconciliationServiceAuditable;
import projectlx.billing.payments.business.logic.api.DriverExpenseReconciliationService;
import projectlx.billing.payments.business.logic.support.BillingMapper;
import projectlx.billing.payments.business.logic.support.CallerOrganizationResolver;
import projectlx.billing.payments.business.validator.api.DriverExpenseReconciliationServiceValidator;
import projectlx.billing.payments.model.DriverExpenseReconciliation;
import projectlx.billing.payments.repository.DriverExpenseReconciliationRepository;
import projectlx.billing.payments.utils.enums.DriverExpenseReconciliationStatus;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.requests.ApproveDriverExpenseRequest;
import projectlx.billing.payments.utils.requests.RejectDriverExpenseRequest;
import projectlx.billing.payments.utils.responses.DriverExpenseResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
public class DriverExpenseReconciliationServiceImpl implements DriverExpenseReconciliationService {

    private final DriverExpenseReconciliationRepository driverExpenseReconciliationRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final MessageService messageService;
    private final DriverExpenseReconciliationServiceAuditable driverExpenseReconciliationServiceAuditable;
    private final DriverExpenseReconciliationServiceValidator driverExpenseReconciliationServiceValidator;

    @Override
    @Transactional(readOnly = true)
    public DriverExpenseResponse list(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        DriverExpenseResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_EXPENSE_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setDriverExpenseReconciliationDtoList(driverExpenseReconciliationRepository
                .findByOrganizationIdAndEntityStatusNotOrderByExpenseDateDesc(organizationId, EntityStatus.DELETED)
                .stream()
                .map(BillingMapper::toDto)
                .toList());
        return response;
    }

    @Override
    public DriverExpenseResponse approve(ApproveDriverExpenseRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = driverExpenseReconciliationServiceValidator
                .isApproveDriverExpenseRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_EXPENSE_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        return transition(request.getId(), username, locale,
                DriverExpenseReconciliationStatus.APPROVED, null,
                I18Code.MESSAGE_DRIVER_EXPENSE_APPROVE_SUCCESS);
    }

    @Override
    public DriverExpenseResponse reject(RejectDriverExpenseRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = driverExpenseReconciliationServiceValidator
                .isRejectDriverExpenseRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_EXPENSE_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        return transition(request.getId(), username, locale,
                DriverExpenseReconciliationStatus.REJECTED,
                request.getRejectionReason(),
                I18Code.MESSAGE_DRIVER_EXPENSE_REJECT_SUCCESS);
    }

    private DriverExpenseResponse transition(
            Long id,
            String username,
            Locale locale,
            DriverExpenseReconciliationStatus targetStatus,
            String rejectionReason,
            I18Code successCode) {

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        Optional<DriverExpenseReconciliation> expenseOpt = driverExpenseReconciliationRepository
                .findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED);
        if (expenseOpt.isEmpty()) {
            return error(404,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_EXPENSE_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        DriverExpenseReconciliation expense = expenseOpt.get();
        expense.setStatus(targetStatus);
        expense.setApprovedBy(username);
        expense.setApprovedAt(LocalDateTime.now());
        expense.setRejectionReason(rejectionReason);
        expense.setModifiedAt(LocalDateTime.now());
        expense.setModifiedBy(username);

        DriverExpenseReconciliation saved = driverExpenseReconciliationServiceAuditable.update(expense, locale, username);
        DriverExpenseResponse response = success(200, messageService.getMessage(successCode.getCode(), new String[]{}, locale));
        response.setDriverExpenseReconciliationDto(BillingMapper.toDto(saved));
        return response;
    }

    private DriverExpenseResponse success(int statusCode, String message) {
        DriverExpenseResponse response = new DriverExpenseResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private DriverExpenseResponse error(int statusCode, String message) {
        DriverExpenseResponse response = new DriverExpenseResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
