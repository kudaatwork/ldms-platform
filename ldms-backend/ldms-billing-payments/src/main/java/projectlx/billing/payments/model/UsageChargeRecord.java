package projectlx.billing.payments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.billing.payments.utils.enums.OrganizationBillingMode;
import projectlx.billing.payments.utils.enums.PlatformBillingTier;
import projectlx.billing.payments.utils.enums.PlatformBillingTierConverter;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "usage_charge_record")
@Getter
@Setter
@ToString
public class UsageChargeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false, length = 50)
    private OrganizationBillingMode billingMode;

    @Column(name = "action_code", nullable = false, length = 80)
    private String actionCode;

    @Column(name = "action_display_name", length = 200)
    private String actionDisplayName;

    /** Snapshot of the action's billing tier, used to count subscription quota consumption. */
    @Convert(converter = PlatformBillingTierConverter.class)
    @Column(name = "billing_tier", length = 20)
    private PlatformBillingTier billingTier;

    @Column(name = "charge_cents", nullable = false)
    private Long chargeCents;

    @Column(name = "deducted", nullable = false)
    private Boolean deducted = Boolean.FALSE;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "season_id")
    private Long seasonId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by")
    private String modifiedBy;
}
