package projectlx.billing.payments.model;

import jakarta.persistence.Column;
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
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_package")
@Getter
@Setter
@ToString
public class SubscriptionPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "monthly_price_cents", nullable = false)
    private Long monthlyPriceCents = 0L;

    @Column(name = "included_heavy_credits", nullable = false)
    private Integer includedHeavyCredits = 0;

    @Column(name = "included_standard_credits", nullable = false)
    private Integer includedStandardCredits = 0;

    @Column(name = "included_light_credits", nullable = false)
    private Integer includedLightCredits = 0;

    @Column(name = "included_tracking_day_credits", nullable = false)
    private Integer includedTrackingDayCredits = 0;

    /** When false (e.g. Starter), organisations on this package cannot enable fuel consumption. */
    @Column(name = "fuel_consumption_available", nullable = false)
    private Boolean fuelConsumptionAvailable = Boolean.TRUE;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "featured", nullable = false)
    private Boolean featured = Boolean.FALSE;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

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
