package projectlx.co.zw.audittrail.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.audittrail.model.AuditEventType;
import projectlx.co.zw.audittrail.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByServiceName(String serviceName, Pageable pageable);

    Page<AuditLog> findByUsername(String username, Pageable pageable);

    Page<AuditLog> findByTraceId(String traceId, Pageable pageable);

    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    Page<AuditLog> findByHttpStatusCode(Integer code, Pageable pageable);

    Page<AuditLog> findByRequestTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<AuditLog> findByTraceIdOrderByRequestTimestampAsc(String traceId);

    @Query(
            value = "SELECT a FROM AuditLog a WHERE "
                    + "(:serviceName IS NULL OR a.serviceName = :serviceName) AND "
                    + "(:username IS NULL OR a.username = :username) AND "
                    + "(:eventType IS NULL OR a.eventType = :eventType) AND "
                    + "(:httpStatusCode IS NULL OR a.httpStatusCode = :httpStatusCode) AND "
                    + "(:from IS NULL OR a.requestTimestamp >= :from) AND "
                    + "(:to IS NULL OR a.requestTimestamp <= :to)",
            countQuery = "SELECT COUNT(a) FROM AuditLog a WHERE "
                    + "(:serviceName IS NULL OR a.serviceName = :serviceName) AND "
                    + "(:username IS NULL OR a.username = :username) AND "
                    + "(:eventType IS NULL OR a.eventType = :eventType) AND "
                    + "(:httpStatusCode IS NULL OR a.httpStatusCode = :httpStatusCode) AND "
                    + "(:from IS NULL OR a.requestTimestamp >= :from) AND "
                    + "(:to IS NULL OR a.requestTimestamp <= :to)")
    Page<AuditLog> search(
            @Param("serviceName") String serviceName,
            @Param("username") String username,
            @Param("eventType") AuditEventType eventType,
            @Param("httpStatusCode") Integer httpStatusCode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.serviceName = :sn AND a.requestTimestamp >= :from")
    long countInWindow(@Param("sn") String serviceName, @Param("from") LocalDateTime from);

    @Query(
            "SELECT COUNT(a) FROM AuditLog a WHERE a.serviceName = :sn AND a.requestTimestamp >= :from "
                    + "AND a.httpStatusCode IS NOT NULL AND a.httpStatusCode >= 400")
    long countHttpErrorsInWindow(@Param("sn") String serviceName, @Param("from") LocalDateTime from);

    @Query(
            "SELECT AVG(a.responseTimeMs) FROM AuditLog a WHERE a.serviceName = :sn AND a.requestTimestamp >= :from "
                    + "AND a.responseTimeMs IS NOT NULL")
    Double avgResponseTimeMsInWindow(@Param("sn") String serviceName, @Param("from") LocalDateTime from);

    @Query(
            "SELECT a.eventType, COUNT(a) FROM AuditLog a WHERE a.serviceName = :sn AND a.requestTimestamp >= :from "
                    + "GROUP BY a.eventType")
    List<Object[]> countByEventTypeInWindow(@Param("sn") String serviceName, @Param("from") LocalDateTime from);

    @Query(
            "SELECT a.httpStatusCode, COUNT(a) FROM AuditLog a WHERE a.serviceName = :sn AND a.requestTimestamp >= :from "
                    + "GROUP BY a.httpStatusCode")
    List<Object[]> countByHttpStatusInWindow(@Param("sn") String serviceName, @Param("from") LocalDateTime from);
}
