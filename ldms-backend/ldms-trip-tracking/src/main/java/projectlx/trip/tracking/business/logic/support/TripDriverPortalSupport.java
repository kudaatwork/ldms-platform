package projectlx.trip.tracking.business.logic.support;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.trip.tracking.clients.FleetManagementServiceClient;
import projectlx.trip.tracking.clients.UserManagementServiceClient;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.utils.dtos.DriverTripMetricsDto;
import projectlx.trip.tracking.utils.dtos.DriverTripSummaryDto;
import projectlx.trip.tracking.utils.dtos.FleetAssetSummaryDto;
import projectlx.trip.tracking.utils.dtos.FleetDriverSummaryDto;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.trip.tracking.utils.responses.FleetAssetFeignResponse;
import projectlx.trip.tracking.utils.responses.FleetDriverFeignResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class TripDriverPortalSupport {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("d MMM HH:mm");

    private static final List<TripStatus> ACTIVE_STATUSES = List.of(
            TripStatus.SCHEDULED, TripStatus.IN_TRANSIT, TripStatus.AT_BORDER_HOLD,
            TripStatus.ROADSIDE_HOLD, TripStatus.ARRIVED, TripStatus.COUNTING_STOCK,
            TripStatus.COUNT_COMPLETE, TripStatus.OTP_PENDING, TripStatus.RETURN_IN_TRANSIT);

    private static final List<TripStatus> DELIVERY_PENDING = List.of(
            TripStatus.ARRIVED, TripStatus.COUNTING_STOCK, TripStatus.COUNT_COMPLETE, TripStatus.OTP_PENDING);

    private final TripRepository tripRepository;
    private final UserManagementServiceClient userManagementServiceClient;
    private final FleetManagementServiceClient fleetManagementServiceClient;

    public Long resolveSessionUserId(String username, Locale locale) {
        try {
            UserResponse userResponse = userManagementServiceClient.findSessionProfileByUsername(username, locale);
            if (userResponse == null || !userResponse.isSuccess() || userResponse.getUserDto() == null) {
                log.warn("Session profile lookup failed for username={}", username);
                return null;
            }
            Long userId = userResponse.getUserDto().getId();
            return userId != null && userId > 0 ? userId : null;
        } catch (Exception ex) {
            log.warn("Could not resolve session userId for username={}: {}", username, ex.getMessage());
            return null;
        }
    }

    public Long resolveFleetDriverId(String username, Locale locale) {
        Long userId = resolveSessionUserId(username, locale);
        if (userId == null) {
            return null;
        }
        try {
            FleetDriverFeignResponse driverResponse =
                    fleetManagementServiceClient.findFleetDriverByUserId(userId, locale);
            if (driverResponse == null || !driverResponse.isSuccess() || driverResponse.getFleetDriverDto() == null) {
                log.warn("Fleet driver profile not found for userId={}", userId);
                return null;
            }
            return driverResponse.getFleetDriverDto().getId();
        } catch (FeignException.NotFound ex) {
            log.warn("Fleet driver profile not found for userId={}", userId);
            return null;
        } catch (Exception ex) {
            log.warn("Could not resolve fleet driver for userId={}: {}", userId, ex.getMessage());
            return null;
        }
    }

    /**
     * True when the trip is assigned to the resolved fleet driver, or the trip's assigned driver
     * shares the same platform user id (handles stale fleet_driver_id on trip rows).
     */
    public boolean isTripAccessibleToDriver(Trip trip, Long fleetDriverId, Long sessionUserId, Locale locale) {
        if (trip == null || fleetDriverId == null) {
            return false;
        }
        if (fleetDriverId.equals(trip.getFleetDriverId())) {
            return true;
        }
        if (sessionUserId == null || trip.getFleetDriverId() == null) {
            return false;
        }
        Long assignedUserId = resolveUserIdForFleetDriver(trip.getFleetDriverId(), locale);
        return sessionUserId.equals(assignedUserId);
    }

    private Long resolveUserIdForFleetDriver(Long fleetDriverId, Locale locale) {
        try {
            FleetDriverFeignResponse response = fleetManagementServiceClient.findFleetDriverById(fleetDriverId, locale);
            if (response == null || !response.isSuccess() || response.getFleetDriverDto() == null) {
                return null;
            }
            return response.getFleetDriverDto().getUserId();
        } catch (Exception ex) {
            log.warn("Could not resolve userId for fleetDriverId={}: {}", fleetDriverId, ex.getMessage());
            return null;
        }
    }

    public List<DriverTripSummaryDto> listTripsForDriver(Long fleetDriverId, Locale locale) {
        List<Trip> trips = tripRepository
                .findByFleetDriverIdAndEntityStatusNotOrderByStartedAtDesc(fleetDriverId, EntityStatus.DELETED);
        return trips.stream().map(trip -> toSummary(trip, locale)).toList();
    }

    public DriverTripSummaryDto toSummary(Trip trip, Locale locale) {
        Locale resolvedLocale = locale != null ? locale : Locale.ENGLISH;
        DriverTripSummaryDto dto = new DriverTripSummaryDto();
        dto.setId(trip.getId());
        dto.setTripNumber(trip.getTripNumber());
        dto.setShipmentNumber(trip.getShipmentNumber());
        dto.setRoute(buildRoute(trip));
        dto.setProductName(trip.getProductName());
        dto.setQuantity(trip.getQuantity());
        dto.setUnitOfMeasure("UNIT");
        dto.setCargoLabel(buildCargoLabel(trip));
        String status = trip.getStatus() != null ? trip.getStatus().name() : "";
        dto.setStatus(status);
        dto.setStatusLabel(statusLabel(trip.getStatus()));
        dto.setStatusTone(statusTone(trip.getStatus()));
        if (trip.getStartedAt() != null) {
            dto.setStartedAtLabel(trip.getStartedAt().format(DISPLAY_TIME));
        }
        dto.setCanTriggerArrival(trip.getStatus() == TripStatus.IN_TRANSIT);
        dto.setCanStartDeliveryWorkflow(isDeliveryWorkflowStatus(trip.getStatus()));
        dto.setCanLiveTrack(isLiveTrackStatus(trip.getStatus()));
        dto.setDeliveryWorkflowPhase(deliveryPhase(trip.getStatus()));
        applyAssignedFleetProfile(trip, dto, resolvedLocale);
        return dto;
    }

    private void applyAssignedFleetProfile(Trip trip, DriverTripSummaryDto dto, Locale locale) {
        if (trip.getFleetDriverId() != null) {
            dto.setFleetDriverId(trip.getFleetDriverId());
            try {
                FleetDriverFeignResponse response =
                        fleetManagementServiceClient.findFleetDriverById(trip.getFleetDriverId(), locale);
                FleetDriverSummaryDto driver = response != null ? response.getFleetDriverDto() : null;
                if (driver != null) {
                    dto.setDriverUserId(driver.getUserId());
                    dto.setDriverPhone(driver.getPhoneNumber());
                    String name = buildDriverName(driver.getFirstName(), driver.getLastName());
                    if (name != null) {
                        dto.setDriverName(name);
                    }
                }
            } catch (Exception ex) {
                log.debug("Fleet driver enrich skipped for trip {}: {}", trip.getId(), ex.getMessage());
            }
        }

        if (trip.getFleetAssetId() == null) {
            return;
        }
        try {
            FleetAssetFeignResponse response =
                    fleetManagementServiceClient.findFleetAssetById(trip.getFleetAssetId(), locale);
            FleetAssetSummaryDto asset = response != null ? response.getFleetAssetDto() : null;
            if (asset == null) {
                return;
            }
            if (asset.getRegistration() != null && !asset.getRegistration().isBlank()) {
                dto.setVehicleRegistration(asset.getRegistration().trim());
            }
            if (dto.getDriverName() == null && asset.getDriverName() != null && !asset.getDriverName().isBlank()) {
                dto.setDriverName(asset.getDriverName().trim());
            }
        } catch (Exception ex) {
            log.debug("Fleet asset enrich skipped for trip {}: {}", trip.getId(), ex.getMessage());
        }
    }

    private static String buildDriverName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        String combined = (f + " " + l).trim();
        return combined.isEmpty() ? null : combined;
    }

    public DriverTripMetricsDto metricsForDriver(Long fleetDriverId) {
        List<Trip> trips = tripRepository
                .findByFleetDriverIdAndEntityStatusNotOrderByStartedAtDesc(fleetDriverId, EntityStatus.DELETED);
        LocalDate today = LocalDate.now();
        DriverTripMetricsDto metrics = new DriverTripMetricsDto();
        metrics.setActiveTrips((int) trips.stream()
                .filter(t -> t.getStatus() != null && ACTIVE_STATUSES.contains(t.getStatus()))
                .count());
        metrics.setPendingDeliveries((int) trips.stream()
                .filter(t -> t.getStatus() != null && DELIVERY_PENDING.contains(t.getStatus()))
                .count());
        metrics.setCompletedToday((int) trips.stream()
                .filter(t -> t.getStatus() == TripStatus.DELIVERED || t.getStatus() == TripStatus.RETURNED)
                .filter(t -> completedToday(t, today))
                .count());
        return metrics;
    }

    private static boolean completedToday(Trip trip, LocalDate today) {
        LocalDateTime at = trip.getCompletedAt() != null ? trip.getCompletedAt() : trip.getArrivedAt();
        return at != null && at.toLocalDate().equals(today);
    }

    private static String buildRoute(Trip trip) {
        String from = trip.getFromWarehouseName() != null ? trip.getFromWarehouseName() : "Origin";
        String to = trip.getToWarehouseName() != null ? trip.getToWarehouseName() : "Destination";
        return from + " → " + to;
    }

    private static String buildCargoLabel(Trip trip) {
        if (trip.getProductName() == null && trip.getQuantity() == null) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        if (trip.getProductName() != null) {
            sb.append(trip.getProductName());
        }
        if (trip.getQuantity() != null) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(trip.getQuantity().stripTrailingZeros().toPlainString());
        }
        return sb.isEmpty() ? "—" : sb.toString();
    }

    private static String statusLabel(TripStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case IN_TRANSIT -> "In transit";
            case ARRIVED -> "At destination";
            case COUNTING_STOCK -> "Counting stock";
            case COUNT_COMPLETE -> "Count complete";
            case OTP_PENDING -> "Awaiting OTP";
            case DELIVERED -> "Delivered";
            case RETURN_IN_TRANSIT -> "Returning";
            case RETURNED -> "Returned";
            case ROADSIDE_HOLD -> "Roadside hold";
            default -> status.name().replace('_', ' ');
        };
    }

    private static String statusTone(TripStatus status) {
        if (status == null) {
            return "muted";
        }
        return switch (status) {
            case IN_TRANSIT, RETURN_IN_TRANSIT -> "info";
            case ARRIVED, COUNTING_STOCK, COUNT_COMPLETE, OTP_PENDING -> "warn";
            case DELIVERED, RETURNED -> "success";
            case ROADSIDE_HOLD, AT_BORDER_HOLD -> "danger";
            default -> "muted";
        };
    }

    private static boolean isDeliveryWorkflowStatus(TripStatus status) {
        return status == TripStatus.ARRIVED
                || status == TripStatus.COUNTING_STOCK
                || status == TripStatus.COUNT_COMPLETE
                || status == TripStatus.OTP_PENDING
                || status == TripStatus.DELIVERED
                || status == TripStatus.RETURN_IN_TRANSIT;
    }

    private static boolean isLiveTrackStatus(TripStatus status) {
        return status == TripStatus.IN_TRANSIT
                || status == TripStatus.ROADSIDE_HOLD
                || status == TripStatus.RETURN_IN_TRANSIT;
    }

    private static String deliveryPhase(TripStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case IN_TRANSIT -> "ARRIVAL";
            case ARRIVED -> "STOCK_COUNTING";
            case COUNTING_STOCK -> "STOCK_COUNTING";
            case COUNT_COMPLETE -> "SEND_OTP";
            case OTP_PENDING -> "OTP_VERIFICATION";
            case DELIVERED -> "START_RETURN";
            case RETURN_IN_TRANSIT -> "RETURNS";
            case RETURNED -> "CONFIRM_RETURN";
            default -> null;
        };
    }
}
