package projectlx.co.zw.organizationmanagement.business.logic.api;

import org.springframework.data.domain.Pageable;
import projectlx.co.zw.organizationmanagement.utils.dtos.ImportSummary;
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
import projectlx.co.zw.organizationmanagement.utils.requests.KycActionRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycRejectRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationRequest;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;

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

    OrganizationResponse findByMultipleFilters(OrganizationMultipleFiltersRequest request, String username, Locale locale);

    List<projectlx.co.zw.shared_library.utils.dtos.OrganizationDto> listOrganizationsForExport(
            OrganizationMultipleFiltersRequest request, String username, Locale locale);

    OrganizationResponse getById(Long id, Locale locale);

    OrganizationResponse getByIdForSystem(Long id, Locale locale);

    OrganizationResponse delete(Long id, Locale locale, String modifiedBy);

    OrganizationResponse update(Long id, UpdateOrganizationRequest request, Locale locale, String modifiedBy);

    OrganizationResponse provisionContactPerson(Long id, Locale locale, String modifiedBy);

    OrganizationResponse stage1Approve(Long id, KycActionRequest request, Locale locale, String username);

    OrganizationResponse stage1Reject(Long id, KycRejectRequest request, Locale locale, String username);

    OrganizationResponse stage2Approve(Long id, KycActionRequest request, Locale locale, String username);

    OrganizationResponse stage2Reject(Long id, KycRejectRequest request, Locale locale, String username);

    OrganizationResponse allowResubmission(Long id, KycActionRequest request, Locale locale, String username);

    OrganizationResponse listKycReviews(Long id, Locale locale);

    OrganizationResponse listIndustriesWithUsage(Locale locale);

    OrganizationResponse findIndustriesByMultipleFilters(IndustryMultipleFiltersRequest request, Locale locale);

    OrganizationResponse getIndustryById(Long id, Locale locale);

    OrganizationResponse createIndustry(CreateIndustryRequest request, Locale locale, String username);

    OrganizationResponse updateIndustry(Long id, UpdateIndustryRequest request, Locale locale, String username);

    OrganizationResponse deleteIndustry(Long id, Locale locale, String username);

    OrganizationResponse findBranchesByMultipleFilters(BranchMultipleFiltersRequest request, Locale locale);

    OrganizationResponse findAgentsByMultipleFilters(AgentMultipleFiltersRequest request, Locale locale);

    OrganizationResponse createBranch(CreateBranchRequest request, Locale locale, String username);

    OrganizationResponse updateBranch(Long id, UpdateBranchRequest request, Locale locale, String username);

    OrganizationResponse getBranchById(Long id, Locale locale);

    OrganizationResponse deleteBranch(Long id, Locale locale, String username);

    OrganizationResponse createAgent(CreateAgentRequest request, Locale locale, String username);

    OrganizationResponse updateAgent(Long id, UpdateAgentRequest request, Locale locale, String username);

    OrganizationResponse getAgentById(Long id, Locale locale);

    OrganizationResponse deleteAgent(Long id, Locale locale, String username);

    List<BranchDto> listBranchesForExport(BranchMultipleFiltersRequest request, Locale locale);

    List<AgentDto> listAgentsForExport(AgentMultipleFiltersRequest request, Locale locale);

    List<IndustryDto> listIndustriesForExport(IndustryMultipleFiltersRequest request, Locale locale);

    ImportSummary importBranchesFromCsv(InputStream inputStream, Locale locale, String username) throws IOException;

    ImportSummary importAgentsFromCsv(InputStream inputStream, Locale locale, String username) throws IOException;

    ImportSummary importIndustriesFromCsv(InputStream inputStream, Locale locale, String username) throws IOException;
}
