package projectlx.co.zw.organizationmanagement.business.validation.api;

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
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface OrganizationServiceValidator {

    ValidatorDto validateRegister(RegisterOrganizationRequest request, Locale locale);

    default ValidatorDto validateRegister(RegisterOrganizationRequest request) {
        return validateRegister(request, Locale.getDefault());
    }

    ValidatorDto validateUpdateMy(UpdateMyOrganizationRequest request, Locale locale);

    default ValidatorDto validateUpdateMy(UpdateMyOrganizationRequest request) {
        return validateUpdateMy(request, Locale.getDefault());
    }

    ValidatorDto validateUpdate(UpdateOrganizationRequest request, Locale locale);

    default ValidatorDto validateUpdate(UpdateOrganizationRequest request) {
        return validateUpdate(request, Locale.getDefault());
    }

    ValidatorDto validateAddBranch(AddBranchRequest request, Locale locale);

    default ValidatorDto validateAddBranch(AddBranchRequest request) {
        return validateAddBranch(request, Locale.getDefault());
    }

    ValidatorDto validateRegisterCustomer(RegisterCustomerOrganizationRequest request, Locale locale);

    default ValidatorDto validateRegisterCustomer(RegisterCustomerOrganizationRequest request) {
        return validateRegisterCustomer(request, Locale.getDefault());
    }

    ValidatorDto validateLinkTransporter(LinkTransporterRequest request, Locale locale);

    default ValidatorDto validateLinkTransporter(LinkTransporterRequest request) {
        return validateLinkTransporter(request, Locale.getDefault());
    }

    ValidatorDto validateFindByMultipleFilters(OrganizationMultipleFiltersRequest request, Locale locale);

    default ValidatorDto validateFindByMultipleFilters(OrganizationMultipleFiltersRequest request) {
        return validateFindByMultipleFilters(request, Locale.getDefault());
    }

    ValidatorDto validateFindBranchesByMultipleFilters(BranchMultipleFiltersRequest request, Locale locale);

    default ValidatorDto validateFindBranchesByMultipleFilters(BranchMultipleFiltersRequest request) {
        return validateFindBranchesByMultipleFilters(request, Locale.getDefault());
    }

    ValidatorDto validateFindAgentsByMultipleFilters(AgentMultipleFiltersRequest request, Locale locale);

    default ValidatorDto validateFindAgentsByMultipleFilters(AgentMultipleFiltersRequest request) {
        return validateFindAgentsByMultipleFilters(request, Locale.getDefault());
    }

    ValidatorDto validateFindIndustriesByMultipleFilters(IndustryMultipleFiltersRequest request, Locale locale);

    default ValidatorDto validateFindIndustriesByMultipleFilters(IndustryMultipleFiltersRequest request) {
        return validateFindIndustriesByMultipleFilters(request, Locale.getDefault());
    }

    ValidatorDto validateCreateIndustry(CreateIndustryRequest request, Locale locale);

    default ValidatorDto validateCreateIndustry(CreateIndustryRequest request) {
        return validateCreateIndustry(request, Locale.getDefault());
    }

    ValidatorDto validateUpdateIndustry(UpdateIndustryRequest request, Locale locale);

    default ValidatorDto validateUpdateIndustry(UpdateIndustryRequest request) {
        return validateUpdateIndustry(request, Locale.getDefault());
    }

    ValidatorDto validateIndustryId(Long id, Locale locale);

    default ValidatorDto validateIndustryId(Long id) {
        return validateIndustryId(id, Locale.getDefault());
    }

    ValidatorDto validateOrganizationId(Long id, Locale locale);

    default ValidatorDto validateOrganizationId(Long id) {
        return validateOrganizationId(id, Locale.getDefault());
    }

    ValidatorDto validateCreateBranch(CreateBranchRequest request, Locale locale);

    ValidatorDto validateUpdateBranch(UpdateBranchRequest request, Locale locale);

    ValidatorDto validateBranchId(Long id, Locale locale);

    ValidatorDto validateCreateAgent(CreateAgentRequest request, Locale locale);

    ValidatorDto validateUpdateAgent(UpdateAgentRequest request, Locale locale);

    ValidatorDto validateAgentId(Long id, Locale locale);
}
