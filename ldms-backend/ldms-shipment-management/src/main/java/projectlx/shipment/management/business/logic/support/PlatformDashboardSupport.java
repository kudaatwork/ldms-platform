package projectlx.shipment.management.business.logic.support;

import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.shipment.management.model.Shipment;
import projectlx.shipment.management.repository.ShipmentRepository;
import projectlx.shipment.management.repository.projection.DailyShipmentVolumeProjection;
import projectlx.shipment.management.repository.projection.OrganizationShipmentStatsProjection;
import projectlx.shipment.management.repository.projection.ShipmentStatusCountProjection;
import projectlx.shipment.management.utils.dtos.PlatformOrganizationShipmentStatsDto;
import projectlx.shipment.management.utils.dtos.PlatformShipmentDashboardDto;
import projectlx.shipment.management.utils.dtos.PlatformShipmentStatusCountDto;
import projectlx.shipment.management.utils.dtos.ShipmentDto;
import projectlx.shipment.management.utils.enums.ShipmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlatformDashboardSupport {

    private static final List<ShipmentStatus> ACTIVE_STATUSES = List.of(
            ShipmentStatus.PENDING_ALLOCATION,
            ShipmentStatus.PENDING_FLEET_ALLOCATION,
            ShipmentStatus.ALLOCATED,
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.ARRIVED_PENDING_OTP);

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "PENDING_ALLOCATION", "Pending allocation",
            "PENDING_FLEET_ALLOCATION", "Pending fleet",
            "ALLOCATED", "Allocated",
            "IN_TRANSIT", "In transit",
            "ARRIVED_PENDING_OTP", "Arrived",
            "DELIVERED", "Delivered",
            "CANCELLED", "Cancelled");

    private static final Map<String, String> STATUS_COLORS = Map.of(
            "PENDING_ALLOCATION", "#94a3b8",
            "PENDING_FLEET_ALLOCATION", "#60a5fa",
            "ALLOCATED", "#818cf8",
            "IN_TRANSIT", "#22c55e",
            "ARRIVED_PENDING_OTP", "#f59e0b",
            "DELIVERED", "#10b981",
            "CANCELLED", "#ef4444");

    private final ShipmentRepository shipmentRepository;

    public PlatformDashboardSupport(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    public PlatformShipmentDashboardDto buildDashboardSnapshot() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();

        Map<Long, PlatformOrganizationShipmentStatsDto> merged = new HashMap<>();
        mergeOrganizationStats(merged, shipmentRepository.aggregateStatsByOwnerOrganization(monthStart));
        mergeOrganizationStats(merged, shipmentRepository.aggregateStatsByTransportOrganization(monthStart));

        List<PlatformOrganizationShipmentStatsDto> organizationStats = merged.values().stream()
                .filter(row -> row.getActiveShipments() > 0 || row.getCompletedThisMonth() > 0)
                .sorted(Comparator
                        .comparingLong((PlatformOrganizationShipmentStatsDto row) ->
                                row.getCompletedThisMonth() * 2L + row.getActiveShipments())
                        .reversed()
                        .thenComparing(PlatformOrganizationShipmentStatsDto::getLastActivityAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long activeShipments = shipmentRepository.countByStatusInAndEntityStatusNot(ACTIVE_STATUSES, EntityStatus.DELETED);
        long completedThisMonth = shipmentRepository.countByStatusAndModifiedAtGreaterThanEqualAndEntityStatusNot(
                ShipmentStatus.DELIVERED, monthStart, EntityStatus.DELETED);
        long organizationsWithActivity = organizationStats.stream()
                .filter(row -> row.getActiveShipments() > 0 || row.getCompletedThisMonth() > 0)
                .count();

        PlatformShipmentDashboardDto dto = new PlatformShipmentDashboardDto();
        dto.setActiveShipments(activeShipments);
        dto.setCompletedThisMonth(completedThisMonth);
        dto.setOrganizationsWithActivity(organizationsWithActivity);
        dto.setOrganizationStats(organizationStats);
        dto.setShipmentsByStatus(buildStatusCounts());
        dto.setWeeklyVolume(buildWeeklyVolume(weekStart));
        dto.setLiveShipments(buildLiveShipments());
        return dto;
    }

    private void mergeOrganizationStats(Map<Long, PlatformOrganizationShipmentStatsDto> merged,
                                        List<OrganizationShipmentStatsProjection> rows) {
        for (OrganizationShipmentStatsProjection row : rows) {
            if (row.getOrganizationId() == null) {
                continue;
            }
            PlatformOrganizationShipmentStatsDto existing = merged.computeIfAbsent(
                    row.getOrganizationId(), id -> {
                        PlatformOrganizationShipmentStatsDto created = new PlatformOrganizationShipmentStatsDto();
                        created.setOrganizationId(id);
                        return created;
                    });
            existing.setActiveShipments(existing.getActiveShipments() + nullSafe(row.getActiveShipments()));
            existing.setCompletedThisMonth(existing.getCompletedThisMonth() + nullSafe(row.getCompletedThisMonth()));
            if (row.getLastActivityAt() != null
                    && (existing.getLastActivityAt() == null || row.getLastActivityAt().isAfter(existing.getLastActivityAt()))) {
                existing.setLastActivityAt(row.getLastActivityAt());
            }
        }
    }

    private List<PlatformShipmentStatusCountDto> buildStatusCounts() {
        List<PlatformShipmentStatusCountDto> rows = new ArrayList<>();
        for (ShipmentStatusCountProjection projection : shipmentRepository.countByStatus()) {
            if (projection.getStatus() == null) {
                continue;
            }
            PlatformShipmentStatusCountDto row = new PlatformShipmentStatusCountDto();
            row.setStatus(projection.getStatus());
            row.setLabel(STATUS_LABELS.getOrDefault(projection.getStatus(), projection.getStatus()));
            row.setCount(nullSafe(projection.getCount()));
            row.setColor(STATUS_COLORS.getOrDefault(projection.getStatus(), "#64748b"));
            rows.add(row);
        }
        return rows;
    }

    private List<Long> buildWeeklyVolume(LocalDateTime weekStart) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        LocalDate cursor = weekStart.toLocalDate();
        LocalDate end = LocalDate.now();
        while (!cursor.isAfter(end)) {
            counts.put(cursor, 0L);
            cursor = cursor.plusDays(1);
        }
        for (DailyShipmentVolumeProjection row : shipmentRepository.countCreatedSince(weekStart)) {
            if (row.getDay() != null) {
                counts.put(row.getDay(), nullSafe(row.getCount()));
            }
        }
        return new ArrayList<>(counts.values());
    }

    private List<ShipmentDto> buildLiveShipments() {
        return shipmentRepository
                .findTop20ByStatusInAndEntityStatusNotOrderByModifiedAtDesc(ACTIVE_STATUSES, EntityStatus.DELETED)
                .stream()
                .map(ShipmentMapper::toDto)
                .toList();
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
