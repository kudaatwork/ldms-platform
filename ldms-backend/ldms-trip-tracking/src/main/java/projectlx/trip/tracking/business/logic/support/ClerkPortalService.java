package projectlx.trip.tracking.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.trip.tracking.clients.FleetManagementServiceClient;
import projectlx.trip.tracking.clients.UserManagementServiceClient;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.utils.dtos.IncomingDeliveryDto;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.trip.tracking.utils.responses.ClerkProfileResponse;
import projectlx.trip.tracking.utils.responses.FleetDriverFeignResponse;
import projectlx.trip.tracking.utils.responses.IncomingDeliveriesResponse;
import projectlx.trip.tracking.utils.responses.IncomingDeliveryResponse;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Backs the clerk portal: the clerk's own profile and the inbound deliveries
 * heading to their organisation that are awaiting receipt.
 */
@Service
@RequiredArgsConstructor
public class ClerkPortalService {

    private static final Logger log = LoggerFactory.getLogger(ClerkPortalService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Statuses that represent a delivery still inbound / being received. */
    private static final Set<TripStatus> INBOUND_STATUSES = Set.of(
            TripStatus.IN_TRANSIT,
            TripStatus.AT_BORDER_HOLD,
            TripStatus.ROADSIDE_HOLD,
            TripStatus.ARRIVED,
            TripStatus.COUNTING_STOCK,
            TripStatus.COUNT_COMPLETE,
            TripStatus.OTP_PENDING,
            TripStatus.DELIVERED);

    private static final int MAX_DELIVERIES = 100;

    private final UserManagementServiceClient userManagementServiceClient;
    private final FleetManagementServiceClient fleetManagementServiceClient;
    private final TripRepository tripRepository;
    private final LogisticsNotificationRecipientResolver recipientResolver;

    // ============================================================
    // Clerk profile
    // ============================================================
    public ClerkProfileResponse getMyProfile(String username, Locale locale) {
        ClerkProfileResponse response = new ClerkProfileResponse();
        UserDto user = resolveUser(username, locale);
        if (user == null) {
            response.setSuccess(false);
            response.setStatusCode(404);
            response.setMessage("No clerk profile found for your account.");
            return response;
        }
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setBranchId(user.getBranchId());
        if (user.getOrganizationId() != null) {
            OrganizationDto org = safeResolveOrganization(user.getOrganizationId(), locale);
            if (org != null) {
                response.setOrganizationName(org.getName());
            }
        }
        return response;
    }

    // ============================================================
    // Incoming deliveries
    // ============================================================
    @Transactional(readOnly = true)
    public IncomingDeliveriesResponse getIncomingDeliveries(String username, Locale locale) {
        IncomingDeliveriesResponse response = new IncomingDeliveriesResponse();
        UserDto user = resolveUser(username, locale);
        if (user == null || user.getOrganizationId() == null) {
            response.setSuccess(false);
            response.setStatusCode(404);
            response.setMessage("No clerk profile found for your account.");
            response.setData(new ArrayList<>());
            return response;
        }

        List<Trip> trips = tripRepository
                .findByOrganizationIdAndEntityStatusNot(
                        user.getOrganizationId(), EntityStatus.DELETED,
                        PageRequest.of(0, MAX_DELIVERIES))
                .getContent();

        Map<Long, String> driverNameCache = new HashMap<>();
        List<IncomingDeliveryDto> rows = new ArrayList<>();
        for (Trip trip : trips) {
            if (!INBOUND_STATUSES.contains(trip.getStatus())) {
                continue;
            }
            rows.add(toDeliveryDto(trip, driverNameCache, locale));
        }

        response.setSuccess(true);
        response.setStatusCode(200);
        response.setData(rows);
        return response;
    }

    @Transactional(readOnly = true)
    public IncomingDeliveryResponse getIncomingDelivery(String username, Long tripId, Locale locale) {
        IncomingDeliveryResponse response = new IncomingDeliveryResponse();
        UserDto user = resolveUser(username, locale);
        if (user == null || user.getOrganizationId() == null) {
            response.setSuccess(false);
            response.setStatusCode(404);
            response.setMessage("No clerk profile found for your account.");
            return response;
        }

        // Read-only view: use the no-lock lookup. The locking variant issues SELECT ... FOR UPDATE,
        // which fails inside this readOnly transaction and surfaced as a 500 on the clerk detail screen.
        Trip trip = tripRepository.findByIdAndEntityStatusNotNoLock(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null || !user.getOrganizationId().equals(trip.getOrganizationId())) {
            response.setSuccess(false);
            response.setStatusCode(404);
            response.setMessage("Delivery not found.");
            return response;
        }

        response.setSuccess(true);
        response.setStatusCode(200);
        response.setData(toDeliveryDto(trip, new HashMap<>(), locale));
        return response;
    }

    // ============================================================
    // Helpers
    // ============================================================
    private IncomingDeliveryDto toDeliveryDto(Trip trip, Map<Long, String> driverNameCache, Locale locale) {
        IncomingDeliveryDto dto = new IncomingDeliveryDto();
        dto.setTripId(trip.getId());
        dto.setTripNumber(trip.getTripNumber());
        dto.setStatus(trip.getStatus() != null ? trip.getStatus().name() : null);
        dto.setProductName(trip.getProductName());
        dto.setQuantity(trip.getQuantity());
        dto.setOriginBranch(trip.getFromWarehouseName());
        dto.setInventoryTransferId(trip.getInventoryTransferId());
        if (trip.getArrivedAt() != null) {
            dto.setArrivedAt(trip.getArrivedAt().format(ISO));
        }
        if (trip.getStartedAt() != null && trip.getArrivedAt() == null) {
            dto.setEta(trip.getStartedAt().format(ISO));
        }
        if (trip.getFleetDriverId() != null) {
            dto.setDriverName(driverNameCache.computeIfAbsent(
                    trip.getFleetDriverId(), id -> resolveDriverName(id, locale)));
        }
        return dto;
    }

    private String resolveDriverName(Long fleetDriverId, Locale locale) {
        try {
            FleetDriverFeignResponse response = fleetManagementServiceClient.findFleetDriverById(fleetDriverId, locale);
            if (response != null && response.isSuccess() && response.getFleetDriverDto() != null) {
                String name = buildName(
                        response.getFleetDriverDto().getFirstName(),
                        response.getFleetDriverDto().getLastName());
                return StringUtils.hasText(name) ? name : "";
            }
        } catch (Exception ex) {
            log.warn("Could not resolve driver name for fleetDriverId={}: {}", fleetDriverId, ex.getMessage());
        }
        return "";
    }

    private UserDto resolveUser(String username, Locale locale) {
        try {
            UserResponse response = userManagementServiceClient.findSessionProfileByUsername(username, locale);
            if (response != null && response.isSuccess() && response.getUserDto() != null) {
                return response.getUserDto();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve clerk user for username={}: {}", username, ex.getMessage());
        }
        return null;
    }

    private OrganizationDto safeResolveOrganization(Long organizationId, Locale locale) {
        try {
            return recipientResolver.resolveOrganization(organizationId, locale);
        } catch (Exception ex) {
            log.warn("Could not resolve organisation {} for clerk profile: {}", organizationId, ex.getMessage());
            return null;
        }
    }

    private static String buildName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        return (f + " " + l).trim();
    }
}
