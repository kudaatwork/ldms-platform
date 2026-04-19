package projectlx.co.zw.organizationmanagement.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import projectlx.co.zw.organizationmanagement.business.logic.api.OrganizationService;
import projectlx.co.zw.organizationmanagement.service.processor.api.OrganizationServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.requests.AddBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycActionRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.KycRejectRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.LinkTransporterRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterCustomerOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.Locale;

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
    public OrganizationResponse listCustomers(Locale locale, String username) {
        log.info("Incoming request: listCustomers user={}", username);
        OrganizationResponse response = organizationService.listCustomers(locale, username);
        log.info("Outgoing response: listCustomers success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationResponse registerCustomer(RegisterCustomerOrganizationRequest request, Locale locale, String username) {
        log.info("Incoming request: registerCustomer user={}", username);
        OrganizationResponse response = organizationService.registerCustomer(request, locale, username);
        log.info("Outgoing response: registerCustomer success={}", response.isSuccess());
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
    public OrganizationResponse getKycQueue(String status, String classification, Pageable pageable, Locale locale) {
        log.info("Incoming request: getKycQueue");
        OrganizationResponse response = organizationService.getKycQueue(status, classification, pageable, locale);
        log.info("Outgoing response: getKycQueue success={}", response.isSuccess());
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
}
