package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Stores idempotency keys to prevent double-processing of operations.
 */
@Entity
@Table(name = "idempotency_key", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotency_key_value", columnNames = {"key_value"})
})
@Getter
@Setter
@ToString
public class IdempotencyKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Use a non-reserved column name instead of "key"
    @Column(name = "key_value", nullable = false, length = 200)
    private String keyValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", length = 100)
    private IdempotencyOperation operation; // e.g., RECEIVE_GOODS, COMPLETE_TRANSFER

    @Column(name = "reference_type", length = 100)
    private String referenceType; // e.g., GRV, INVENTORY_TRANSFER

    @Column(name = "reference_id")
    private Long referenceId; // ID of created/affected record (optional)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IdempotencyStatus status = IdempotencyStatus.IN_PROGRESS;

    // Cached response for replaying on duplicate requests
    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody; // JSON serialized response object
}
