package projectlx.co.zw.organizationmanagement.business.validation.impl;

import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.business.validation.api.OrganizationServiceValidator;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.model.AgentKind;
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
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        if (!StringUtils.hasText(request.getContactPersonEmail())) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                    new String[] { "contactPersonEmail" }, locale));
        }
        if (!StringUtils.hasText(request.getContactPersonFirstName())) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                    new String[] { "contactPersonFirstName" }, locale));
        }
        if (!StringUtils.hasText(request.getContactPersonLastName())) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                    new String[] { "contactPersonLastName" }, locale));
        }
        if (request.getContactPersonGender() == null) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                    new String[] { "contactPersonGender" }, locale));
        }
        if (!StringUtils.hasText(request.getContactPersonPhoneNumber())) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                    new String[] { "contactPersonPhoneNumber" }, locale));
        }
        if (request.getOrganizationClassification() == null) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "classification" }, locale));
        }
        if (request.getOrganizationType() == null) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "organizationType" }, locale));
        }
        if (!StringUtils.hasText(request.getContactPersonDateOfBirth())) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                    new String[] { "contactPersonDateOfBirth" }, locale));
        } else {
            try {
                LocalDate dob = LocalDate.parse(request.getContactPersonDateOfBirth().trim());
                if (!Validators.isAtLeast18YearsOld(dob)) {
                    errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                            new String[] { "contactPersonDateOfBirth" }, locale));
                }
            } catch (DateTimeParseException ex) {
                errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                        new String[] { "contactPersonDateOfBirth" }, locale));
            }
        }
        boolean hasNationalId = StringUtils.hasText(request.getContactPersonNationalIdNumber());
        boolean hasNationalUpload = request.getContactPersonNationalIdUpload() != null
                && !request.getContactPersonNationalIdUpload().isEmpty();
        boolean hasNationalUploadId = request.getContactPersonNationalIdUploadId() != null;
        boolean hasPassport = StringUtils.hasText(request.getContactPersonPassportNumber());
        boolean hasPassportUpload = request.getContactPersonPassportUpload() != null
                && !request.getContactPersonPassportUpload().isEmpty();
        boolean hasPassportUploadId = request.getContactPersonPassportUploadId() != null;
        boolean nationalIdComplete = hasNationalId && (hasNationalUpload || hasNationalUploadId);
        boolean passportComplete = hasPassport && (hasPassportUpload || hasPassportUploadId);
        if (!nationalIdComplete && !passportComplete) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                    new String[] { "contactPersonIdentification" }, locale));
        }
        boolean hasTaxNumber = StringUtils.hasText(request.getTaxNumber());
        boolean hasTaxUpload = request.getTaxClearanceCertificateUpload() != null
                && !request.getTaxClearanceCertificateUpload().isEmpty();
        boolean hasTaxUploadId = request.getTaxClearanceCertificateUploadId() != null;
        boolean taxDocumentProvided = hasTaxUpload || hasTaxUploadId;
        boolean viaSignup = request.getCreatedViaSignup() == null || Boolean.TRUE.equals(request.getCreatedViaSignup());
        if (viaSignup) {
            if (!hasTaxNumber) {
                errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                        new String[] { "taxNumber" }, locale));
            }
            if (!taxDocumentProvided) {
                errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                        new String[] { "taxClearanceCertificate" }, locale));
            }
        } else if (hasTaxNumber && !taxDocumentProvided) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(),
                    new String[] { "taxClearanceCertificate" }, locale));
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
    public ValidatorDto validateUpdate(UpdateOrganizationRequest request, Locale locale) {
        if (request == null) {
            return new ValidatorDto(false, null,
                    List.of(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[]{}, locale)));
        }
        List<String> errors = new ArrayList<>();
        if (request.getEmail() != null && StringUtils.hasText(request.getEmail().trim())
                && !request.getEmail().trim().contains("@")) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "email" }, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
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

    @Override
    public ValidatorDto validateFindByMultipleFilters(OrganizationMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "request" }, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getPage() < 0) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "page" }, locale));
        }
        if (request.getSize() < 1) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "size" }, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateFindBranchesByMultipleFilters(BranchMultipleFiltersRequest request, Locale locale) {
        return validatePagedRequest(request, locale);
    }

    @Override
    public ValidatorDto validateFindAgentsByMultipleFilters(AgentMultipleFiltersRequest request, Locale locale) {
        return validatePagedRequest(request, locale);
    }

    @Override
    public ValidatorDto validateFindIndustriesByMultipleFilters(IndustryMultipleFiltersRequest request, Locale locale) {
        return validatePagedRequest(request, locale);
    }

    private ValidatorDto validatePagedRequest(Object request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "request" }, locale));
            return new ValidatorDto(false, null, errors);
        }
        int page = request instanceof BranchMultipleFiltersRequest b ? b.getPage()
                : request instanceof AgentMultipleFiltersRequest a ? a.getPage()
                : request instanceof IndustryMultipleFiltersRequest i ? i.getPage() : -1;
        int size = request instanceof BranchMultipleFiltersRequest b ? b.getSize()
                : request instanceof AgentMultipleFiltersRequest a ? a.getSize()
                : request instanceof IndustryMultipleFiltersRequest i ? i.getSize() : 0;
        if (page < 0) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "page" }, locale));
        }
        if (size < 1) {
            errors.add(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "size" }, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateCreateIndustry(CreateIndustryRequest request, Locale locale) {
        return validateIndustryRequest(request == null, request != null ? request.getName() : null, locale);
    }

    @Override
    public ValidatorDto validateUpdateIndustry(UpdateIndustryRequest request, Locale locale) {
        return validateIndustryRequest(request == null, request != null ? request.getName() : null, locale);
    }

    @Override
    public ValidatorDto validateIndustryId(Long id, Locale locale) {
        if (id == null || id < 1) {
            return new ValidatorDto(false, null,
                    List.of(messageService.getMessage(I18Code.INDUSTRY_VALIDATION_FAILED.getCode(), new String[] { "id" }, locale)));
        }
        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto validateOrganizationId(Long id, Locale locale) {
        if (id == null || id < 1) {
            return new ValidatorDto(false, null,
                    List.of(messageService.getMessage(I18Code.ORG_VALIDATION_FAILED.getCode(), new String[] { "id" }, locale)));
        }
        return new ValidatorDto(true, null, null);
    }

    private ValidatorDto validateIndustryRequest(boolean missing, String name, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (missing) {
            errors.add(messageService.getMessage(I18Code.INDUSTRY_VALIDATION_FAILED.getCode(), new String[] { "request" }, locale));
        } else if (!StringUtils.hasText(name)) {
            errors.add(messageService.getMessage(I18Code.INDUSTRY_VALIDATION_FAILED.getCode(), new String[] { "name" }, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateCreateBranch(CreateBranchRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null || request.getOrganizationId() == null || !StringUtils.hasText(request.getBranchName())) {
            errors.add(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(), new String[] {}, locale));
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, null) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateUpdateBranch(UpdateBranchRequest request, Locale locale) {
        if (request == null) {
            return new ValidatorDto(false, null,
                    List.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(), new String[] {}, locale)));
        }
        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto validateBranchId(Long id, Locale locale) {
        if (id == null || id < 1) {
            return new ValidatorDto(false, null,
                    List.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(), new String[] { "id" }, locale)));
        }
        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto validateCreateAgent(CreateAgentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null || request.getOrganizationId() == null || !StringUtils.hasText(request.getAgentKind())) {
            errors.add(messageService.getMessage(I18Code.AGENT_VALIDATION_FAILED.getCode(), new String[] {}, locale));
        } else {
            try {
                AgentKind.valueOf(request.getAgentKind().trim());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.AGENT_VALIDATION_FAILED.getCode(), new String[] { "agentKind" }, locale));
            }
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, null) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateUpdateAgent(UpdateAgentRequest request, Locale locale) {
        if (request == null) {
            return new ValidatorDto(false, null,
                    List.of(messageService.getMessage(I18Code.AGENT_VALIDATION_FAILED.getCode(), new String[] {}, locale)));
        }
        if (StringUtils.hasText(request.getAgentKind())) {
            try {
                AgentKind.valueOf(request.getAgentKind().trim());
            } catch (IllegalArgumentException ex) {
                return new ValidatorDto(false, null,
                        List.of(messageService.getMessage(I18Code.AGENT_VALIDATION_FAILED.getCode(), new String[] { "agentKind" }, locale)));
            }
        }
        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto validateAgentId(Long id, Locale locale) {
        if (id == null || id < 1) {
            return new ValidatorDto(false, null,
                    List.of(messageService.getMessage(I18Code.AGENT_VALIDATION_FAILED.getCode(), new String[] { "id" }, locale)));
        }
        return new ValidatorDto(true, null, null);
    }
}
