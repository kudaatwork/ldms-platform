package projectlx.shipment.management.repository.projection;

import java.time.LocalDate;

public interface DailyShipmentVolumeProjection {

    LocalDate getDay();

    Long getCount();
}
