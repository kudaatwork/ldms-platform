package projectlx.co.zw.organizationmanagement.business.validation.impl;

import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.business.validation.api.OrganizationServiceValidator;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrganizationServiceValidatorImpl implements OrganizationServiceValidator {

    private final MessageService messageService;

    public OrganizationServiceValidatorImpl(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public ValidatorDto validateRegister(RegisterOrganizationRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (!StringUtils.hasText(request.getName())) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "name" }, locale));
        }
        if (!StringUtils.hasText(request.getEmail())) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "email" }, locale));
        }
        if (request.getOrganizationClassification() == null) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "classification" }, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateUpdateMy(UpdateMyOrganizationRequest request, Locale locale) {
        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto validateAddBranch(AddBranchRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null || !StringUtils.hasText(request.getBranchName())) {
            errors.add(messageService.getMessage(I18Code.ORG_BRANCH_INVALID.getCode(), new String[]{}, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateRegisterCustomer(RegisterCustomerOrganizationRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null || !StringUtils.hasText(request.getName()) || !StringUtils.hasText(request.getEmail())) {
            errors.add(messageService.getMessage(I18Code.ORG_CUSTOMER_REGISTER_INVALID.getCode(), new String[]{}, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateLinkTransporter(LinkTransporterRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null || request.getTransporterOrganizationId() == null) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "transporterId" }, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }
}
