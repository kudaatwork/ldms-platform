package projectlx.trip.tracking.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_delivery_workflow", indexes = {
        @Index(name = "idx_delivery_workflow_trip_status", columnList = "trip_id, entity_status")
})
@Getter
@Setter
@ToString(exclude = {"trip", "returnLines"})
public class TripDeliveryWorkflow implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    // === COUNTING TIMESTAMPS ===

    @Column(name = "driver_counting_started_at")
    private LocalDateTime driverCountingStartedAt;

    @Column(name = "driver_counting_finished_at")
    private LocalDateTime driverCountingFinishedAt;

    @Column(name = "customer_counting_started_at")
    private LocalDateTime customerCountingStartedAt;

    @Column(name = "customer_counting_finished_at")
    private LocalDateTime customerCountingFinishedAt;

    // === QUANTITIES ===

    @Column(name = "expected_quantity", precision = 19, scale = 2)
    private BigDecimal expectedQuantity;

    @Column(name = "counted_quantity", precision = 19, scale = 2)
    private BigDecimal countedQuantity;

    // === OTP CHANNEL ===

    @Column(name = "otp_channel", length = 50)
    private String otpChannel;

    @Column(name = "otp_recipient", length = 320)
    private String otpRecipient;

    // === DELIVERY NOTES ===

    @Column(name = "delivery_notes", length = 2000)
    private String deliveryNotes;

    // === RETURN JOURNEY ===

    @Column(name = "return_initiated_at")
    private LocalDateTime returnInitiatedAt;

    @Column(name = "return_completed_at")
    private LocalDateTime returnCompletedAt;

    // === AUDIT ===

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;

    // === RELATIONSHIPS ===

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripDeliveryReturnLine> returnLines = new ArrayList<>();
}
