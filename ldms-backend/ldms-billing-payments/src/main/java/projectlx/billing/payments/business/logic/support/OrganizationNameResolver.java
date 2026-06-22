package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.clients.OrganizationManagementServiceClient;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationNameResolver {

    private static final Set<String> PLACEHOLDER_NAMES = Set.of("organization", "organisation");

    private final OrganizationManagementServiceClient organizationManagementServiceClient;

    public String resolve(Long organizationId) {
        return resolve(organizationId, null);
    }

    public String resolve(Long organizationId, String storedName) {
        if (organizationId == null) {
            return hasRealName(storedName) ? storedName.trim() : "Organisation";
        }
        if (hasRealName(storedName)) {
            return storedName.trim();
        }
        String fetched = fetchFromOrganizationService(organizationId);
        if (hasRealName(fetched)) {
            return fetched.trim();
        }
        return "Organisation #" + organizationId;
    }

    public boolean isPlaceholder(String name) {
        return !hasRealName(name);
    }

    private boolean hasRealName(String name) {
        return StringUtils.hasText(name) && !PLACEHOLDER_NAMES.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    private String fetchFromOrganizationService(Long organizationId) {
        try {
            OrganizationResponse response = organizationManagementServiceClient.findById(organizationId, Locale.getDefault());
            if (response != null && response.isSuccess() && response.getOrganizationDto() != null) {
                return response.getOrganizationDto().getName();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve organisation name for org {}: {}", organizationId, ex.getMessage());
        }
        return null;
    }
}
