package projectlx.shipment.management.repository.projection;

import java.time.LocalDateTime;

public interface OrganizationShipmentStatsProjection {

    Long getOrganizationId();

    Long getActiveShipments();

    Long getCompletedThisMonth();

    LocalDateTime getLastActivityAt();
}
