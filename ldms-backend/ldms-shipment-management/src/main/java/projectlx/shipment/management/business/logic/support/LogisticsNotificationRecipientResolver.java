package projectlx.shipment.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import projectlx.shipment.management.clients.FleetManagementServiceClient;
import projectlx.shipment.management.clients.OrganizationManagementServiceClient;
import projectlx.shipment.management.clients.UserManagementServiceClient;
import projectlx.shipment.management.utils.dtos.FleetDriverSummaryDto;
import projectlx.shipment.management.utils.responses.FleetDriverFeignResponse;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Resolves all recipient contact information needed for logistics lifecycle notifications.
 *
 * <p>All Feign calls are wrapped in try/catch — failures degrade gracefully (empty/null values
 * are passed to the notification support, which skips blank addresses).</p>
 */
@RequiredArgsConstructor
@Slf4j
public class LogisticsNotificationRecipientResolver {

    private final OrganizationManagementServiceClient organizationManagementServiceClient;
    private final UserManagementServiceClient userManagementServiceClient;
    private final FleetManagementServiceClient fleetManagementServiceClient;

    // ============================================================
    // Organisation contact resolution
    // ============================================================

    public OrganizationDto resolveOrganization(Long organizationId, Locale locale) {
        if (organizationId == null) return null;
        try {
            OrganizationResponse response = organizationManagementServiceClient.findById(organizationId, locale);
            if (response != null && response.isSuccess()) {
                return response.getOrganizationDto();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve organization id={}: {}", organizationId, ex.getMessage());
        }
        return null;
    }

    // ============================================================
    // Fleet manager resolution
    // ============================================================

    public List<UserDto> resolveFleetManagers(Long organizationId, Locale locale) {
        if (organizationId == null) return Collections.emptyList();
        try {
            UserResponse response = userManagementServiceClient.findFleetManagersByOrganization(organizationId, locale);
            if (response != null && response.isSuccess() && response.getUserDtoList() != null) {
                return response.getUserDtoList();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve fleet managers for orgId={}: {}", organizationId, ex.getMessage());
        }
        return Collections.emptyList();
    }

    // ============================================================
    // Driver contact resolution
    // ============================================================

    public DriverContact resolveDriverContact(Long fleetDriverId, Locale locale) {
        if (fleetDriverId == null) return DriverContact.empty();
        try {
            FleetDriverFeignResponse driverResponse = fleetManagementServiceClient.findFleetDriverById(fleetDriverId, locale);
            if (driverResponse == null || !driverResponse.isSuccess() || driverResponse.getFleetDriverDto() == null) {
                return DriverContact.empty();
            }
            FleetDriverSummaryDto driverDto = driverResponse.getFleetDriverDto();
            String driverName = buildDriverName(driverDto);
            String driverPhone = driverDto.getPhoneNumber();

            if (driverDto.getUserId() != null) {
                try {
                    UserResponse userResponse = userManagementServiceClient.findById(driverDto.getUserId(), locale);
                    if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null) {
                        UserDto userDto = userResponse.getUserDto();
                        String email = userDto.getEmail();
                        String phone = StringUtils.hasText(userDto.getPhoneNumber())
                                ? userDto.getPhoneNumber()
                                : driverPhone;
                        return new DriverContact(email, phone, driverName);
                    }
                } catch (Exception ex) {
                    log.warn("Could not resolve user profile for driverId={} userId={}: {}",
                            fleetDriverId, driverDto.getUserId(), ex.getMessage());
                }
            }

            return new DriverContact(null, driverPhone, driverName);
        } catch (Exception ex) {
            log.warn("Could not resolve driver contact for fleetDriverId={}: {}", fleetDriverId, ex.getMessage());
            return DriverContact.empty();
        }
    }

    private static String buildDriverName(FleetDriverSummaryDto dto) {
        String first = dto.getFirstName() != null ? dto.getFirstName().trim() : "";
        String last = dto.getLastName() != null ? dto.getLastName().trim() : "";
        String name = (first + " " + last).trim();
        return StringUtils.hasText(name) ? name : "Driver";
    }

    // ============================================================
    // Value object for driver contact info
    // ============================================================

    public record DriverContact(String email, String phone, String name) {
        public static DriverContact empty() {
            return new DriverContact(null, null, "Driver");
        }
    }
}
