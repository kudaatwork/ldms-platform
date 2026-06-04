package projectlx.co.zw.organizationmanagement.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

/**
 * Feign client for user-management system API (KYC approver pool).
 */
public interface UserManagementServiceClient {

    @GetMapping("/ldms-user-management/v1/system/user/organization-kyc-approvers")
    UserResponse listOrganizationKycApprovers();

    @GetMapping("/ldms-user-management/v1/system/user/find-by-username/{username}")
    UserResponse findByUsername(@org.springframework.web.bind.annotation.PathVariable("username") String username);

    @GetMapping("/ldms-user-management/v1/system/user/session-profile-by-username/{username}")
    UserResponse findSessionProfileByUsername(
            @org.springframework.web.bind.annotation.PathVariable("username") String username);

    @PostMapping("/ldms-user-management/v1/system/user/provision-organization-contact-person")
    UserResponse provisionOrganizationContactPerson(@RequestBody ProvisionOrganizationContactPersonRequest request);

    @PostMapping("/ldms-user-management/v1/system/user/issue-organization-contact-credentials")
    UserResponse issueOrganizationContactCredentials(@RequestBody IssueOrganizationContactCredentialsRequest request);

    @PostMapping("/ldms-user-management/v1/system/user/find-by-multiple-filters")
    UserResponse findUsersByMultipleFilters(@RequestBody UsersMultipleFiltersFeignRequest request);

    @GetMapping("/ldms-user-management/v1/system/user/find-by-organization-id/{organizationId}")
    UserResponse findByOrganizationId(@org.springframework.web.bind.annotation.PathVariable("organizationId") Long organizationId);
}
