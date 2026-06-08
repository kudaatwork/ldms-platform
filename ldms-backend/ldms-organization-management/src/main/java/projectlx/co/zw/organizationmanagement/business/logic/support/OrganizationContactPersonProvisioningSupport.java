package projectlx.co.zw.organizationmanagement.business.logic.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.clients.ProvisionOrganizationContactPersonRequest;
import projectlx.co.zw.organizationmanagement.clients.UserManagementServiceClient;
import projectlx.co.zw.organizationmanagement.clients.UsersMultipleFiltersFeignRequest;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

import java.util.List;
import java.util.Locale;

/**
 * Provisions the organisation contact person user via user-management (pending email verification).
 */
@Component
@RequiredArgsConstructor
public class OrganizationContactPersonProvisioningSupport {

    private static final Logger log = LoggerFactory.getLogger(OrganizationContactPersonProvisioningSupport.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserManagementServiceClient userManagementServiceClient;

    public Long provisionContactPersonUser(Organization org, boolean viaSignup) {
        return provisionContactPersonUser(org, viaSignup, true);
    }

    public Long provisionContactPersonUser(Organization org, boolean viaSignup, boolean sendVerificationEmail) {
        if (org == null || org.getId() == null || !StringUtils.hasText(org.getContactPersonEmail())) {
            return null;
        }

        String email = org.getContactPersonEmail().trim().toLowerCase(Locale.ROOT);
        Long existingId = resolveLinkedContactUserId(org.getId(), email);
        if (existingId != null) {
            log.info("Organisation {} already has contact person user {} ({})", org.getId(), existingId, email);
            return existingId;
        }

        ProvisionOrganizationContactPersonRequest request = new ProvisionOrganizationContactPersonRequest();
        request.setOrganizationId(org.getId());
        request.setOrganizationName(org.getName());
        request.setEmail(email);
        request.setFirstName(org.getContactPersonFirstName());
        request.setLastName(org.getContactPersonLastName());
        request.setPhoneNumber(resolveProvisionPhone(org));
        if (org.getContactPersonGender() != null) {
            request.setGender(org.getContactPersonGender().name());
        }
        request.setNationalIdNumber(org.getContactPersonNationalIdNumber());
        request.setPassportNumber(org.getContactPersonPassportNumber());
        request.setDateOfBirth(org.getContactPersonDateOfBirth());
        request.setNationalIdUploadId(org.getContactPersonNationalIdUploadId());
        request.setPassportUploadId(org.getContactPersonPassportUploadId());
        request.setViaSignup(viaSignup);
        request.setSendVerificationEmail(sendVerificationEmail);
        if (org.getContactPersonUserId() != null && org.getContactPersonUserId() > 0) {
            request.setContactUserId(org.getContactPersonUserId());
        }
        try {
            UserResponse response = userManagementServiceClient.provisionOrganizationContactPerson(request);
            Long id = extractUserId(response);
            if (id != null) {
                return id;
            }
            log.warn(
                    "Contact person provisioning did not succeed for organisation {}: {}",
                    org.getId(),
                    describeUserResponse(response));
            existingId = resolveLinkedContactUserId(org.getId(), email);
            if (existingId != null) {
                log.info(
                        "Recovered contact person user {} for organisation {} after failed provision response",
                        existingId,
                        org.getId());
                return existingId;
            }
        } catch (FeignException e) {
            log.error(
                    "Failed to provision contact person user for organisation {}: HTTP {} — {}.",
                    org.getId(),
                    e.status(),
                    feignErrorSummary(e),
                    e);
            existingId = resolveLinkedContactUserId(org.getId(), email);
            if (existingId != null) {
                return existingId;
            }
        } catch (Exception e) {
            log.error(
                    "Failed to provision contact person user for organisation {}: {}",
                    org.getId(),
                    e.getMessage(),
                    e);
        }
        return null;
    }

    /**
     * Keeps the linked contact person user aligned when a supplier edits customer contact details.
     */
    public void syncContactPersonFromOrganization(Organization org) {
        if (org == null || org.getId() == null || org.getContactPersonUserId() == null || org.getContactPersonUserId() <= 0) {
            return;
        }
        if (!StringUtils.hasText(org.getContactPersonEmail())) {
            return;
        }
        boolean viaSignup = Boolean.TRUE.equals(org.getCreatedViaSignup());
        provisionContactPersonUser(org, viaSignup, false);
    }

    /**
     * Finds an active user already linked to this organisation with the contact email (avoids duplicate create).
     */
    private Long resolveLinkedContactUserId(Long organizationId, String normalizedEmail) {
        if (organizationId == null || organizationId <= 0 || !StringUtils.hasText(normalizedEmail)) {
            return null;
        }
        Long fromOrganizationLookup = resolveFromOrganizationUserList(organizationId, normalizedEmail);
        if (fromOrganizationLookup != null) {
            return fromOrganizationLookup;
        }
        Long fromMultipleFilters = resolveFromMultipleFilters(organizationId, normalizedEmail);
        if (fromMultipleFilters != null) {
            return fromMultipleFilters;
        }
        return resolveFromEmailLinkedToOrganization(organizationId, normalizedEmail);
    }

    private Long resolveFromOrganizationUserList(Long organizationId, String normalizedEmail) {
        try {
            UserResponse response = userManagementServiceClient.findByOrganizationId(organizationId);
            if (response == null || !response.isSuccess() || response.getUserDtoList() == null) {
                return null;
            }
            return matchContactEmail(response.getUserDtoList(), normalizedEmail);
        } catch (Exception e) {
            log.debug(
                    "Could not resolve existing contact user for organisation {} via organization lookup: {}",
                    organizationId,
                    e.getMessage());
            return null;
        }
    }

    private Long resolveFromMultipleFilters(Long organizationId, String normalizedEmail) {
        try {
            UsersMultipleFiltersFeignRequest filters = new UsersMultipleFiltersFeignRequest();
            filters.setPage(0);
            filters.setSize(20);
            filters.setOrganizationId(organizationId);
            filters.setSearchValue(normalizedEmail);
            UserResponse response = userManagementServiceClient.findUsersByMultipleFilters(filters);
            if (response == null || !response.isSuccess() || response.getUserDtoPage() == null) {
                return null;
            }
            Page<UserDto> page = response.getUserDtoPage();
            List<UserDto> content = page.getContent();
            return matchContactEmail(content, normalizedEmail);
        } catch (Exception e) {
            log.debug(
                    "Could not resolve existing contact user for organisation {} via user lookup: {}",
                    organizationId,
                    e.getMessage());
            return null;
        }
    }

    private Long resolveFromEmailLinkedToOrganization(Long organizationId, String normalizedEmail) {
        try {
            UsersMultipleFiltersFeignRequest filters = new UsersMultipleFiltersFeignRequest();
            filters.setPage(0);
            filters.setSize(5);
            filters.setEmail(normalizedEmail);
            UserResponse response = userManagementServiceClient.findUsersByMultipleFilters(filters);
            if (response == null || !response.isSuccess() || response.getUserDtoPage() == null) {
                return null;
            }
            for (UserDto dto : response.getUserDtoPage().getContent()) {
                if (dto == null || dto.getId() == null || dto.getOrganizationId() == null) {
                    continue;
                }
                String candidate = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase(Locale.ROOT) : "";
                if (normalizedEmail.equals(candidate) && organizationId.equals(dto.getOrganizationId())) {
                    return dto.getId();
                }
            }
        } catch (Exception e) {
            log.debug(
                    "Could not resolve existing contact user for organisation {} via email lookup: {}",
                    organizationId,
                    e.getMessage());
        }
        return null;
    }

    private static Long matchContactEmail(List<UserDto> users, String normalizedEmail) {
        if (users == null || users.isEmpty()) {
            return null;
        }
        for (UserDto dto : users) {
            if (dto == null || dto.getId() == null) {
                continue;
            }
            String candidate = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase(Locale.ROOT) : "";
            if (normalizedEmail.equals(candidate)) {
                return dto.getId();
            }
        }
        return null;
    }

    private static Long extractUserId(UserResponse response) {
        if (response == null || !response.isSuccess()) {
            return null;
        }
        if (response.getUserDto() != null && response.getUserDto().getId() != null) {
            return response.getUserDto().getId();
        }
        return null;
    }

    private static String describeUserResponse(UserResponse response) {
        if (response == null) {
            return "null response";
        }
        if (response.getErrorMessages() != null && !response.getErrorMessages().isEmpty()) {
            return String.join("; ", response.getErrorMessages());
        }
        return response.getMessage() != null ? response.getMessage() : "success=false";
    }

    private static String feignErrorSummary(FeignException e) {
        String body = e.contentUTF8();
        if (!StringUtils.hasText(body)) {
            return e.getMessage();
        }
        try {
            UserResponse parsed = OBJECT_MAPPER.readValue(body, UserResponse.class);
            String described = describeUserResponse(parsed);
            if (StringUtils.hasText(described) && !"null response".equals(described)) {
                return described;
            }
        } catch (Exception ignored) {
            // Fall through to raw body snippet
        }
        return body.length() > 500 ? body.substring(0, 500) + "…" : body;
    }

    private static String resolveProvisionPhone(Organization org) {
        if (org.getContactPersonPhoneNumber() != null && !org.getContactPersonPhoneNumber().isBlank()) {
            return org.getContactPersonPhoneNumber().trim();
        }
        if (org.getPhoneNumber() != null && !org.getPhoneNumber().isBlank()) {
            return org.getPhoneNumber().trim();
        }
        return null;
    }
}
