package projectlx.co.zw.organizationmanagement.business.logic.api;

import org.springframework.data.domain.Pageable;
import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycActionRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycRejectRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.Locale;

public interface OrganizationService {

    OrganizationResponse register(RegisterOrganizationRequest request, Locale locale, String createdBy);

    OrganizationResponse submitKyc(Locale locale, String username);

    OrganizationResponse getMy(Locale locale, String username);

    OrganizationResponse updateMy(UpdateMyOrganizationRequest request, Locale locale, String username);

    OrganizationResponse addBranch(AddBranchRequest request, Locale locale, String username);

    OrganizationResponse listBranches(Locale locale, String username);

    OrganizationResponse listCustomers(Locale locale, String username);

    OrganizationResponse registerCustomer(RegisterCustomerOrganizationRequest request, Locale locale, String username);

    OrganizationResponse linkTransporter(LinkTransporterRequest request, Locale locale, String username);

    OrganizationResponse getKycQueue(String status, String classification, Pageable pageable, Locale locale);

    OrganizationResponse getById(Long id, Locale locale);

    OrganizationResponse stage1Approve(Long id, KycActionRequest request, Locale locale, String username);

    OrganizationResponse stage1Reject(Long id, KycRejectRequest request, Locale locale, String username);

    OrganizationResponse stage2Approve(Long id, KycActionRequest request, Locale locale, String username);

    OrganizationResponse stage2Reject(Long id, KycRejectRequest request, Locale locale, String username);

    OrganizationResponse allowResubmission(Long id, KycActionRequest request, Locale locale, String username);

    OrganizationResponse listKycReviews(Long id, Locale locale);
}
