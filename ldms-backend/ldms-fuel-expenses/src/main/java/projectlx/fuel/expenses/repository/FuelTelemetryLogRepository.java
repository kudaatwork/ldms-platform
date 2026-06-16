package projectlx.fuel.expenses.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fuel.expenses.model.FuelTelemetryLog;
import projectlx.fuel.expenses.utils.enums.FuelReadingType;
import projectlx.fuel.expenses.utils.enums.FuelTelemetrySource;

import java.util.List;

public interface FuelTelemetryLogRepository extends JpaRepository<FuelTelemetryLog, Long>,
        JpaSpecificationExecutor<FuelTelemetryLog> {

    Page<FuelTelemetryLog> findByTripIdAndEntityStatusNotOrderByRecordedAtDesc(Long tripId, EntityStatus entityStatus,
            Pageable pageable);

    List<FuelTelemetryLog> findByFuelSessionIdAndEntityStatusNotOrderByRecordedAtDesc(Long fuelSessionId,
            EntityStatus entityStatus);

    Page<FuelTelemetryLog> findByTripIdAndSourceAndEntityStatusNotOrderByRecordedAtDesc(Long tripId,
            FuelTelemetrySource source, EntityStatus entityStatus, Pageable pageable);

    Page<FuelTelemetryLog> findByTripIdAndReadingTypeAndEntityStatusNotOrderByRecordedAtDesc(Long tripId,
            FuelReadingType readingType, EntityStatus entityStatus, Pageable pageable);
}
