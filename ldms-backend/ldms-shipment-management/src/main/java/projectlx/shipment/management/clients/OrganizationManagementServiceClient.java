package projectlx.shipment.management.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.shipment.management.utils.requests.ValidateTransporterAssignmentFeignRequest;

import java.util.Locale;

public interface OrganizationManagementServiceClient {

    @GetMapping("/ldms-organization-management/v1/system/organization/{id}")
    OrganizationResponse findById(
            @PathVariable("id") Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/transporter-assignment/validate")
    OrganizationResponse validateTransporterAssignment(
            @RequestBody ValidateTransporterAssignmentFeignRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
