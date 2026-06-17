package projectlx.messaging.inbound.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.Locale;

public interface OrganizationManagementServiceClient {

    @GetMapping("/ldms-organization-management/v1/system/organization/find-by-id/{id}")
    OrganizationResponse findById(
            @PathVariable("id") Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    default OrganizationResponse findById(Long id) {
        return findById(id, Locale.ENGLISH);
    }
}
