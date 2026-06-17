package projectlx.trip.tracking.business.logic.support;

import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.repository.projection.OrganizationTripStatsProjection;
import projectlx.trip.tracking.utils.dtos.PlatformOrganizationTripStatsDto;
import projectlx.trip.tracking.utils.dtos.PlatformTripDashboardDto;
import projectlx.trip.tracking.utils.enums.TripStatus;

import java.util.Comparator;
import java.util.List;

public class PlatformDashboardSupport {

    private static final List<TripStatus> ACTIVE_STATUSES = List.of(
            TripStatus.SCHEDULED,
            TripStatus.IN_TRANSIT,
            TripStatus.AT_BORDER_HOLD,
            TripStatus.ROADSIDE_HOLD,
            TripStatus.ARRIVED,
            TripStatus.OTP_PENDING);

    private final TripRepository tripRepository;

    public PlatformDashboardSupport(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    public PlatformTripDashboardDto buildDashboardSnapshot() {
        long activeTrips = tripRepository.countByStatusInAndEntityStatusNot(ACTIVE_STATUSES, EntityStatus.DELETED);
        long deliveredTrips = tripRepository.countByStatusAndEntityStatusNot(TripStatus.DELIVERED, EntityStatus.DELETED);
        long terminalTrips = deliveredTrips
                + tripRepository.countByStatusAndEntityStatusNot(TripStatus.CANCELLED, EntityStatus.DELETED);
        double onTimePct = terminalTrips == 0
                ? 0.0
                : Math.round((deliveredTrips * 1000.0) / terminalTrips) / 10.0;

        List<PlatformOrganizationTripStatsDto> organizationStats = tripRepository.aggregateActiveTripsByOrganization()
                .stream()
                .map(this::toDto)
                .sorted(Comparator.comparingLong(PlatformOrganizationTripStatsDto::getActiveTrips).reversed())
                .toList();

        PlatformTripDashboardDto dto = new PlatformTripDashboardDto();
        dto.setActiveTrips(activeTrips);
        dto.setDeliveredTrips(deliveredTrips);
        dto.setOnTimePct(onTimePct);
        dto.setOrganizationStats(organizationStats);
        return dto;
    }

    private PlatformOrganizationTripStatsDto toDto(OrganizationTripStatsProjection row) {
        PlatformOrganizationTripStatsDto dto = new PlatformOrganizationTripStatsDto();
        dto.setOrganizationId(row.getOrganizationId());
        dto.setActiveTrips(row.getActiveTrips() == null ? 0L : row.getActiveTrips());
        return dto;
    }
}
