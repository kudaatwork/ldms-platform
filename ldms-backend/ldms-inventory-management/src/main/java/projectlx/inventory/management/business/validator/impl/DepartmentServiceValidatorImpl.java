package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.inventory.management.business.validator.api.DepartmentServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.utils.requests.CreateDepartmentRequest;
import projectlx.inventory.management.utils.requests.DepartmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.EditDepartmentRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrEmpty;

@RequiredArgsConstructor
public class DepartmentServiceValidatorImpl implements DepartmentServiceValidator {

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateDepartmentRequestValid(CreateDepartmentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_CREATE_DEPARTMENT_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (isNullOrEmpty(request.getName())) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_CREATE_DEPARTMENT_NAME_MISSING.getCode(), new String[]{}, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        if (id == null || id <= 0L) {
            List<String> errors = List.of(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditDepartmentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_UPDATE_DEPARTMENT_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getDepartmentId() == null || request.getDepartmentId() <= 0L) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_UPDATE_DEPARTMENT_ID_INVALID.getCode(), new String[]{}, locale));
        }
        if (isNullOrEmpty(request.getName())) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_UPDATE_DEPARTMENT_NAME_MISSING.getCode(), new String[]{}, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveDepartmentByMultipleFilters(
            DepartmentMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getPage() < 0) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_PAGE_NEGATIVE.getCode(), new String[]{}, locale));
        }
        if (request.getSize() <= 0 || request.getSize() > InventoryExportSupport.MAX_FILTER_PAGE_SIZE) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_DEPARTMENT_SIZE_INVALID.getCode(), new String[]{}, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isStringValid(String value, Locale locale) {
        if (isNullOrEmpty(value)) {
            List<String> errors = List.of(messageService.getMessage(
                    I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, null);
    }
}
