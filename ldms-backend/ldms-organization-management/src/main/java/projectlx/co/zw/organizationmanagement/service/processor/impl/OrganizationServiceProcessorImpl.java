package projectlx.co.zw.organizationmanagement.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import projectlx.co.zw.organizationmanagement.business.logic.api.OrganizationService;
import projectlx.co.zw.organizationmanagement.service.processor.api.OrganizationServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.dtos.ImportSummary;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateFleetVehicleRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.EditFleetVehicleRequest;
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
import projectlx.co.zw.organizationmanagement.utils.requests.LinkClearingAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkCustomerRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.OrganizationMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationKycStagesRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateKycApprovalPolicyRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;

@RequiredArgsConstructor
public class OrganizationServiceProcessorImpl implements OrganizationServiceProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceProcessorImpl.class);
    private final OrganizationService organizationService;

    @Override
    public OrganizationResponse register(RegisterOrganizationRequest request, Locale locale, String createdBy) {
        log.info("Incoming request: register organization");
        OrganizationResponse response = organizationService.register(request, locale, createdBy);
        log.info("Outgoing response: register organization success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getOnboardingStatus(Long organizationId, Locale locale) {
        log.info("Incoming request: getOnboardingStatus orgId={}", organizationId);
        OrganizationResponse response = organizationService.getOnboardingStatus(organizationId, locale);
        log.info("Outgoing response: getOnboardingStatus success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse submitKyc(Locale locale, String username) {
        log.info("Incoming request: submitKyc user={}", username);
        OrganizationResponse response = organizationService.submitKyc(locale, username);
        log.info("Outgoing response: submitKyc success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getMy(Locale locale, String username) {
        log.info("Incoming request: getMy user={}", username);
        OrganizationResponse response = organizationService.getMy(locale, username);
        log.info("Outgoing response: getMy success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateMy(UpdateMyOrganizationRequest request, Locale locale, String username) {
        log.info("Incoming request: updateMy user={}", username);
        OrganizationResponse response = organizationService.updateMy(request, locale, username);
        log.info("Outgoing response: updateMy success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse addBranch(AddBranchRequest request, Locale locale, String username) {
        log.info("Incoming request: addBranch user={}", username);
        OrganizationResponse response = organizationService.addBranch(request, locale, username);
        log.info("Outgoing response: addBranch success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse listBranches(Locale locale, String username) {
        log.info("Incoming request: listBranches user={}", username);
        OrganizationResponse response = organizationService.listBranches(locale, username);
        log.info("Outgoing response: listBranches success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getBranchByIdForUser(Long branchId, Locale locale, String username) {
        log.info("Incoming request: getBranchByIdForUser branchId={} user={}", branchId, username);
        OrganizationResponse response = organizationService.getBranchByIdForUser(branchId, locale, username);
        log.info("Outgoing response: getBranchByIdForUser success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateBranchForUser(
            Long branchId, UpdateBranchRequest request, Locale locale, String username) {
        log.info("Incoming request: updateBranchForUser branchId={} user={}", branchId, username);
        OrganizationResponse response = organizationService.updateBranchForUser(branchId, request, locale, username);
        log.info("Outgoing response: updateBranchForUser success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse deleteBranchForUser(Long branchId, Locale locale, String username) {
        log.info("Incoming request: deleteBranchForUser branchId={} user={}", branchId, username);
        OrganizationResponse response = organizationService.deleteBranchForUser(branchId, locale, username);
        log.info("Outgoing response: deleteBranchForUser success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse findBranchesByMultipleFiltersForUser(
            BranchMultipleFiltersRequest request, Locale locale, String username) {
        log.info("Incoming request: findBranchesByMultipleFiltersForUser user={}", username);
        OrganizationResponse response = organizationService.findBranchesByMultipleFiltersForUser(request, locale, username);
        log.info("Outgoing response: findBranchesByMultipleFiltersForUser success={}", response.isSuccess());
        return response;
    }

    @Override
    public List<BranchDto> listBranchesForExportForUser(
            BranchMultipleFiltersRequest request, Locale locale, String username) {
        return organizationService.listBranchesForExportForUser(request, locale, username);
    }

    @Override
    public ImportSummary importBranchesFromCsvForUser(InputStream inputStream, Locale locale, String username)
            throws IOException {
        return organizationService.importBranchesFromCsvForUser(inputStream, locale, username);
    }

    @Override
    public OrganizationResponse listAgents(Locale locale, String username) {
        log.info("Incoming request: listAgents user={}", username);
        OrganizationResponse response = organizationService.listAgents(locale, username);
        log.info("Outgoing response: listAgents success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse createAgentForUser(CreateAgentRequest request, Locale locale, String username) {
        log.info("Incoming request: createAgentForUser user={}", username);
        return organizationService.createAgentForUser(request, locale, username);
    }

    @Override
    public OrganizationResponse updateAgentForUser(Long agentId, UpdateAgentRequest request, Locale locale, String username) {
        log.info("Incoming request: updateAgentForUser agentId={} user={}", agentId, username);
        return organizationService.updateAgentForUser(agentId, request, locale, username);
    }

    @Override
    public OrganizationResponse deleteAgentForUser(Long agentId, Locale locale, String username) {
        log.info("Incoming request: deleteAgentForUser agentId={} user={}", agentId, username);
        return organizationService.deleteAgentForUser(agentId, locale, username);
    }

    @Override
    public ImportSummary importAgentsFromCsvForUser(InputStream inputStream, Locale locale, String username)
            throws IOException {
        return organizationService.importAgentsFromCsvForUser(inputStream, locale, username);
    }

    @Override
    public OrganizationResponse listCustomers(Locale locale, String username) {
        log.info("Incoming request: listCustomers user={}", username);
        OrganizationResponse response = organizationService.listCustomers(locale, username);
        log.info("Outgoing response: listCustomers success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse listTransporters(Locale locale, String username) {
        log.info("Incoming request: listTransporters user={}", username);
        OrganizationResponse response = organizationService.listTransporters(locale, username);
        log.info("Outgoing response: listTransporters success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse searchTransportCompanyCandidates(String search, Locale locale, String username) {
        log.info("Incoming request: searchTransportCompanyCandidates user={}", username);
        OrganizationResponse response = organizationService.searchTransportCompanyCandidates(search, locale, username);
        log.info("Outgoing response: searchTransportCompanyCandidates success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse registerCustomer(RegisterOrganizationRequest request, Locale locale, String username) {
        log.info("Incoming request: registerCustomer user={}", username);
        OrganizationResponse response = organizationService.registerCustomer(request, locale, username);
        log.info("Outgoing response: registerCustomer success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse checkCustomerRegistrationEmail(String email, Locale locale, String username) {
        log.info("Incoming request: checkCustomerRegistrationEmail user={}", username);
        OrganizationResponse response = organizationService.checkCustomerRegistrationEmail(email, locale, username);
        log.info("Outgoing response: checkCustomerRegistrationEmail success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse linkExistingOrganizationAsCustomer(
            projectlx.co.zw.organizationmanagement.utils.requests.LinkExistingOrganizationAsCustomerRequest request,
            Locale locale,
            String username) {
        log.info("Incoming request: linkExistingOrganizationAsCustomer user={}", username);
        OrganizationResponse response = organizationService.linkExistingOrganizationAsCustomer(request, locale, username);
        log.info("Outgoing response: linkExistingOrganizationAsCustomer success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse registerTransporter(RegisterOrganizationRequest request, Locale locale, String username) {
        log.info("Incoming request: registerTransporter user={}", username);
        OrganizationResponse response = organizationService.registerTransporter(request, locale, username);
        log.info("Outgoing response: registerTransporter success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse verifyOrganizationEmail(String email, String token, Locale locale) {
        log.info("Incoming request: verifyOrganizationEmail email={}", email);
        OrganizationResponse response = organizationService.verifyOrganizationEmail(email, token, locale);
        log.info("Outgoing response: verifyOrganizationEmail success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse completeSupplierRegisteredOnboarding(Long organizationId, Locale locale) {
        log.info("Incoming request: completeSupplierRegisteredOnboarding orgId={}", organizationId);
        OrganizationResponse response = organizationService.completeSupplierRegisteredOnboarding(organizationId, locale);
        log.info("Outgoing response: completeSupplierRegisteredOnboarding success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse retryCustomerOnboarding(Long customerId, Locale locale, String username) {
        log.info("Incoming request: retryCustomerOnboarding customerId={} user={}", customerId, username);
        OrganizationResponse response = organizationService.retryCustomerOnboarding(customerId, locale, username);
        log.info("Outgoing response: retryCustomerOnboarding success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getCustomer(Long customerId, Locale locale, String username) {
        log.info("Incoming request: getCustomer id={} user={}", customerId, username);
        OrganizationResponse response = organizationService.getCustomer(customerId, locale, username);
        log.info("Outgoing response: getCustomer success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getTransporter(Long transporterId, Locale locale, String username) {
        log.info("Incoming request: getTransporter id={} user={}", transporterId, username);
        OrganizationResponse response = organizationService.getTransporter(transporterId, locale, username);
        log.info("Outgoing response: getTransporter success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateCustomer(
            Long customerId, RegisterOrganizationRequest request, Locale locale, String username) {
        log.info("Incoming request: updateCustomer id={} user={}", customerId, username);
        OrganizationResponse response = organizationService.updateCustomer(customerId, request, locale, username);
        log.info("Outgoing response: updateCustomer success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse deleteCustomer(Long customerId, Locale locale, String username) {
        log.info("Incoming request: deleteCustomer id={} user={}", customerId, username);
        OrganizationResponse response = organizationService.deleteCustomer(customerId, locale, username);
        log.info("Outgoing response: deleteCustomer success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateTransporter(
            Long transporterId, RegisterOrganizationRequest request, Locale locale, String username) {
        log.info("Incoming request: updateTransporter id={} user={}", transporterId, username);
        OrganizationResponse response = organizationService.updateTransporter(transporterId, request, locale, username);
        log.info("Outgoing response: updateTransporter success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse deleteTransporter(Long transporterId, Locale locale, String username) {
        log.info("Incoming request: deleteTransporter id={} user={}", transporterId, username);
        OrganizationResponse response = organizationService.deleteTransporter(transporterId, locale, username);
        log.info("Outgoing response: deleteTransporter success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse linkTransporter(LinkTransporterRequest request, Locale locale, String username) {
        log.info("Incoming request: linkTransporter user={}", username);
        OrganizationResponse response = organizationService.linkTransporter(request, locale, username);
        log.info("Outgoing response: linkTransporter success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse linkCustomerForOrganization(
            Long supplierId, LinkCustomerRequest request, Locale locale, String username) {
        log.info("Incoming request: linkCustomerForOrganization supplierId={} user={}", supplierId, username);
        OrganizationResponse response = organizationService.linkCustomerForOrganization(supplierId, request, locale, username);
        log.info("Outgoing response: linkCustomerForOrganization success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse linkTransporterForOrganization(
            Long organizationId, LinkTransporterRequest request, Locale locale, String username) {
        log.info("Incoming request: linkTransporterForOrganization organizationId={} user={}", organizationId, username);
        OrganizationResponse response = organizationService.linkTransporterForOrganization(organizationId, request, locale, username);
        log.info("Outgoing response: linkTransporterForOrganization success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse linkClearingAgentForOrganization(
            Long supplierId, LinkClearingAgentRequest request, Locale locale, String username) {
        log.info("Incoming request: linkClearingAgentForOrganization supplierId={} user={}", supplierId, username);
        OrganizationResponse response = organizationService.linkClearingAgentForOrganization(supplierId, request, locale, username);
        log.info("Outgoing response: linkClearingAgentForOrganization success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getKycQueue(String status, String classification, Pageable pageable, Locale locale) {
        log.info("Incoming request: getKycQueue");
        OrganizationResponse response = organizationService.getKycQueue(status, classification, pageable, locale);
        log.info("Outgoing response: getKycQueue success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse findByMultipleFilters(
            OrganizationMultipleFiltersRequest request, String username, Locale locale) {
        log.info("Incoming request: findByMultipleFilters user={}", username);
        OrganizationResponse response = organizationService.findByMultipleFilters(request, username, locale);
        log.info("Outgoing response: findByMultipleFilters success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getById(Long id, Locale locale) {
        log.info("Incoming request: getById id={}", id);
        OrganizationResponse response = organizationService.getById(id, locale);
        log.info("Outgoing response: getById success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getByIdForFrontend(Long id, Locale locale, String username) {
        log.info("Incoming request: getByIdForFrontend id={} user={}", id, username);
        OrganizationResponse response = organizationService.getByIdForFrontend(id, username, locale);
        log.info("Outgoing response: getByIdForFrontend success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getByIdForSystem(Long id, Locale locale) {
        log.info("Incoming request: getByIdForSystem id={}", id);
        OrganizationResponse response = organizationService.getByIdForSystem(id, locale);
        log.info("Outgoing response: getByIdForSystem success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse delete(Long id, Locale locale, String modifiedBy) {
        log.info("Incoming request: delete organization id={}", id);
        OrganizationResponse response = organizationService.delete(id, locale, modifiedBy);
        log.info("Outgoing response: delete organization success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse update(Long id, UpdateOrganizationRequest request, Locale locale, String modifiedBy) {
        log.info("Incoming request: update organization id={}", id);
        OrganizationResponse response = organizationService.update(id, request, locale, modifiedBy);
        log.info("Outgoing response: update organization success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse provisionContactPerson(Long id, Locale locale, String modifiedBy) {
        log.info("Incoming request: provision contact person for organisation id={}", id);
        OrganizationResponse response = organizationService.provisionContactPerson(id, locale, modifiedBy);
        log.info("Outgoing response: provision contact person success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage1Approve(Long id, KycActionRequest request, Locale locale, String username) {
        log.info("Incoming request: stage1Approve id={}", id);
        OrganizationResponse response = organizationService.stage1Approve(id, request, locale, username);
        log.info("Outgoing response: stage1Approve success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage1Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        log.info("Incoming request: stage1Reject id={}", id);
        OrganizationResponse response = organizationService.stage1Reject(id, request, locale, username);
        log.info("Outgoing response: stage1Reject success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage2Approve(Long id, KycActionRequest request, Locale locale, String username) {
        log.info("Incoming request: stage2Approve id={}", id);
        OrganizationResponse response = organizationService.stage2Approve(id, request, locale, username);
        log.info("Outgoing response: stage2Approve success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage2Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        log.info("Incoming request: stage2Reject id={}", id);
        OrganizationResponse response = organizationService.stage2Reject(id, request, locale, username);
        log.info("Outgoing response: stage2Reject success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage3Approve(Long id, KycActionRequest request, Locale locale, String username) {
        log.info("Incoming request: stage3Approve id={}", id);
        OrganizationResponse response = organizationService.stage3Approve(id, request, locale, username);
        log.info("Outgoing response: stage3Approve success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage3Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        log.info("Incoming request: stage3Reject id={}", id);
        OrganizationResponse response = organizationService.stage3Reject(id, request, locale, username);
        log.info("Outgoing response: stage3Reject success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage4Approve(Long id, KycActionRequest request, Locale locale, String username) {
        log.info("Incoming request: stage4Approve id={}", id);
        OrganizationResponse response = organizationService.stage4Approve(id, request, locale, username);
        log.info("Outgoing response: stage4Approve success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage4Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        log.info("Incoming request: stage4Reject id={}", id);
        OrganizationResponse response = organizationService.stage4Reject(id, request, locale, username);
        log.info("Outgoing response: stage4Reject success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage5Approve(Long id, KycActionRequest request, Locale locale, String username) {
        log.info("Incoming request: stage5Approve id={}", id);
        OrganizationResponse response = organizationService.stage5Approve(id, request, locale, username);
        log.info("Outgoing response: stage5Approve success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse stage5Reject(Long id, KycRejectRequest request, Locale locale, String username) {
        log.info("Incoming request: stage5Reject id={}", id);
        OrganizationResponse response = organizationService.stage5Reject(id, request, locale, username);
        log.info("Outgoing response: stage5Reject success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse allowResubmission(Long id, KycActionRequest request, Locale locale, String username) {
        log.info("Incoming request: allowResubmission id={}", id);
        OrganizationResponse response = organizationService.allowResubmission(id, request, locale, username);
        log.info("Outgoing response: allowResubmission success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse listKycReviews(Long id, Locale locale) {
        log.info("Incoming request: listKycReviews id={}", id);
        OrganizationResponse response = organizationService.listKycReviews(id, locale);
        log.info("Outgoing response: listKycReviews success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse listIndustriesWithUsage(Locale locale) {
        log.info("Incoming request: listIndustriesWithUsage");
        OrganizationResponse response = organizationService.listIndustriesWithUsage(locale);
        log.info("Outgoing response: listIndustriesWithUsage success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse listActiveIndustriesForPlatform(Locale locale) {
        log.info("Incoming request: listActiveIndustriesForPlatform");
        OrganizationResponse response = organizationService.listActiveIndustriesForPlatform(locale);
        log.info("Outgoing response: listActiveIndustriesForPlatform success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse getIndustryById(Long id, Locale locale) {
        log.info("Incoming request: getIndustryById id={}", id);
        OrganizationResponse response = organizationService.getIndustryById(id, locale);
        log.info("Outgoing response: getIndustryById success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse createIndustry(CreateIndustryRequest request, Locale locale, String username) {
        log.info("Incoming request: createIndustry name={}", request.getName());
        OrganizationResponse response = organizationService.createIndustry(request, locale, username);
        log.info("Outgoing response: createIndustry success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateIndustry(Long id, UpdateIndustryRequest request, Locale locale, String username) {
        log.info("Incoming request: updateIndustry id={}", id);
        OrganizationResponse response = organizationService.updateIndustry(id, request, locale, username);
        log.info("Outgoing response: updateIndustry success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse deleteIndustry(Long id, Locale locale, String username) {
        log.info("Incoming request: deleteIndustry id={}", id);
        OrganizationResponse response = organizationService.deleteIndustry(id, locale, username);
        log.info("Outgoing response: deleteIndustry success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse findBranchesByMultipleFilters(BranchMultipleFiltersRequest request, Locale locale) {
        log.info("Incoming request: findBranchesByMultipleFilters page={}", request.getPage());
        OrganizationResponse response = organizationService.findBranchesByMultipleFilters(request, locale);
        log.info("Outgoing response: findBranchesByMultipleFilters success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse findAgentsByMultipleFilters(AgentMultipleFiltersRequest request, Locale locale) {
        log.info("Incoming request: findAgentsByMultipleFilters page={}", request.getPage());
        OrganizationResponse response = organizationService.findAgentsByMultipleFilters(request, locale);
        log.info("Outgoing response: findAgentsByMultipleFilters success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse findIndustriesByMultipleFilters(IndustryMultipleFiltersRequest request, Locale locale) {
        log.info("Incoming request: findIndustriesByMultipleFilters page={}", request.getPage());
        OrganizationResponse response = organizationService.findIndustriesByMultipleFilters(request, locale);
        log.info("Outgoing response: findIndustriesByMultipleFilters success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse createBranch(CreateBranchRequest request, Locale locale, String username) {
        return organizationService.createBranch(request, locale, username);
    }

    @Override
    public OrganizationResponse updateBranch(Long id, UpdateBranchRequest request, Locale locale, String username) {
        return organizationService.updateBranch(id, request, locale, username);
    }

    @Override
    public OrganizationResponse getBranchById(Long id, Locale locale) {
        return organizationService.getBranchById(id, locale);
    }

    @Override
    public OrganizationResponse getHeadOfficeBranch(Long organizationId, Locale locale) {
        return organizationService.getHeadOfficeBranch(organizationId, locale);
    }

    @Override
    public OrganizationResponse deleteBranch(Long id, Locale locale, String username) {
        return organizationService.deleteBranch(id, locale, username);
    }

    @Override
    public OrganizationResponse createAgent(CreateAgentRequest request, Locale locale, String username) {
        return organizationService.createAgent(request, locale, username);
    }

    @Override
    public OrganizationResponse updateAgent(Long id, UpdateAgentRequest request, Locale locale, String username) {
        return organizationService.updateAgent(id, request, locale, username);
    }

    @Override
    public OrganizationResponse getAgentById(Long id, Locale locale) {
        return organizationService.getAgentById(id, locale);
    }

    @Override
    public OrganizationResponse deleteAgent(Long id, Locale locale, String username) {
        return organizationService.deleteAgent(id, locale, username);
    }

    @Override
    public List<projectlx.co.zw.shared_library.utils.dtos.OrganizationDto> listOrganizationsForExport(
            OrganizationMultipleFiltersRequest request, String username, Locale locale) {
        return organizationService.listOrganizationsForExport(request, username, locale);
    }

    @Override
    public List<BranchDto> listBranchesForExport(BranchMultipleFiltersRequest request, Locale locale) {
        return organizationService.listBranchesForExport(request, locale);
    }

    @Override
    public List<AgentDto> listAgentsForExport(AgentMultipleFiltersRequest request, Locale locale) {
        return organizationService.listAgentsForExport(request, locale);
    }

    @Override
    public List<IndustryDto> listIndustriesForExport(IndustryMultipleFiltersRequest request, Locale locale) {
        return organizationService.listIndustriesForExport(request, locale);
    }

    @Override
    public ImportSummary importBranchesFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        return organizationService.importBranchesFromCsv(inputStream, locale, username);
    }

    @Override
    public ImportSummary importAgentsFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        return organizationService.importAgentsFromCsv(inputStream, locale, username);
    }

    @Override
    public ImportSummary importIndustriesFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        return organizationService.importIndustriesFromCsv(inputStream, locale, username);
    }

    @Override
    public OrganizationManagementResponse getKycApprovalPolicy(Locale locale) {
        log.info("Incoming request: getKycApprovalPolicy");
        OrganizationManagementResponse response = organizationService.getKycApprovalPolicy(locale);
        log.info("Outgoing response: getKycApprovalPolicy success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationManagementResponse updateKycApprovalPolicy(
            UpdateKycApprovalPolicyRequest request, Locale locale, String modifiedBy) {
        log.info("Incoming request: updateKycApprovalPolicy");
        OrganizationManagementResponse response = organizationService.updateKycApprovalPolicy(request, locale, modifiedBy);
        log.info("Outgoing response: updateKycApprovalPolicy success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateOrganizationKycStages(
            Long id, UpdateOrganizationKycStagesRequest request, Locale locale, String modifiedBy) {
        log.info("Incoming request: updateOrganizationKycStages id={}", id);
        OrganizationResponse response = organizationService.updateOrganizationKycStages(id, request, locale, modifiedBy);
        log.info("Outgoing response: updateOrganizationKycStages success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse listFleetVehicles(Locale locale, String username) {
        log.info("Incoming request: listFleetVehicles user={}", username);
        OrganizationResponse response = organizationService.listFleetVehicles(locale, username);
        log.info("Outgoing response: listFleetVehicles success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse listTransporterFleetVehicles(Long transporterId, Locale locale, String username) {
        log.info("Incoming request: listTransporterFleetVehicles transporterId={} user={}", transporterId, username);
        OrganizationResponse response = organizationService.listTransporterFleetVehicles(transporterId, locale, username);
        log.info("Outgoing response: listTransporterFleetVehicles success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse createFleetVehicle(CreateFleetVehicleRequest request, Locale locale, String username) {
        log.info("Incoming request: createFleetVehicle user={}", username);
        OrganizationResponse response = organizationService.createFleetVehicle(request, locale, username);
        log.info("Outgoing response: createFleetVehicle success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse notifyFleetRegistered(
            projectlx.co.zw.organizationmanagement.utils.requests.FleetRegisteredNotificationRequest request,
            Locale locale) {
        log.info(
                "Incoming request: notifyFleetRegistered registeringOrgId={}",
                request != null ? request.getRegisteringOrganizationId() : null);
        OrganizationResponse response = organizationService.notifyFleetRegistered(request, locale);
        log.info("Outgoing response: notifyFleetRegistered success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateFleetVehicle(Long id, EditFleetVehicleRequest request, Locale locale, String username) {
        log.info("Incoming request: updateFleetVehicle id={} user={}", id, username);
        OrganizationResponse response = organizationService.updateFleetVehicle(id, request, locale, username);
        log.info("Outgoing response: updateFleetVehicle success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse deleteFleetVehicle(Long id, Locale locale, String username) {
        log.info("Incoming request: deleteFleetVehicle id={} user={}", id, username);
        OrganizationResponse response = organizationService.deleteFleetVehicle(id, locale, username);
        log.info("Outgoing response: deleteFleetVehicle success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse validateFleetOwnership(
            projectlx.co.zw.organizationmanagement.utils.requests.ValidateFleetOwnershipRequest request,
            Locale locale) {
        log.info("Incoming request: validateFleetOwnership orgId={} ownershipType={}",
                request != null ? request.getRegisteringOrganizationId() : null,
                request != null ? request.getOwnershipType() : null);
        OrganizationResponse response = organizationService.validateFleetOwnership(request, locale);
        log.info("Outgoing response: validateFleetOwnership success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse validateTransporterAssignment(
            projectlx.co.zw.organizationmanagement.utils.requests.ValidateTransporterAssignmentRequest request,
            Locale locale) {
        log.info("Incoming request: validateTransporterAssignment shipperOrgId={} transportCompanyOrgId={}",
                request != null ? request.getShipperOrganizationId() : null,
                request != null ? request.getTransportCompanyOrganizationId() : null);
        OrganizationResponse response = organizationService.validateTransporterAssignment(request, locale);
        log.info("Outgoing response: validateTransporterAssignment success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateOperationalSettings(
            projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationOperationalSettingsRequest request,
            Locale locale, String username) {
        log.info("Incoming request: updateOperationalSettings user={}", username);
        OrganizationResponse response = organizationService.updateOperationalSettings(request, locale, username);
        log.info("Outgoing response: updateOperationalSettings success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse updateOperationalSettingsForOrganization(
            Long organizationId,
            projectlx.co.zw.organizationmanagement.utils.requests.UpdateOrganizationOperationalSettingsRequest request,
            Locale locale, String username) {
        log.info("Incoming request: updateOperationalSettingsForOrganization orgId={} user={}", organizationId, username);
        OrganizationResponse response = organizationService
                .updateOperationalSettingsForOrganization(organizationId, request, locale, username);
        log.info("Outgoing response: updateOperationalSettingsForOrganization success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse disableFuelConsumptionForOrganization(
            Long organizationId, Locale locale, String username) {
        log.info("Incoming request: disableFuelConsumptionForOrganization orgId={} user={}", organizationId, username);
        OrganizationResponse response =
                organizationService.disableFuelConsumptionForOrganization(organizationId, locale, username);
        log.info("Outgoing response: disableFuelConsumptionForOrganization success={}", response.isSuccess());
        return response;
    }
}
