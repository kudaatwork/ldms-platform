package projectlx.co.zw.organizationmanagement.business.validation.api;

import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
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
}
