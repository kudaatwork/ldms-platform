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
import projectlx.billing.payments.utils.enums.OrganizationBillingMode;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_billing_setting")
@Getter
@Setter
@ToString
public class OrganizationBillingSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false, unique = true)
    private Long organizationId;

    @Column(name = "organization_name", nullable = false, length = 200)
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false, length = 50)
    private OrganizationBillingMode billingMode = OrganizationBillingMode.PREPAID_WALLET;

    @Column(name = "subscription_package_id")
    private Long subscriptionPackageId;

    @Column(name = "subscription_started_at")
    private LocalDateTime subscriptionStartedAt;

    @Column(name = "subscription_renews_at")
    private LocalDateTime subscriptionRenewsAt;

    @Column(name = "low_balance_threshold_cents", nullable = false)
    private Long lowBalanceThresholdCents = 500L;

    @Column(name = "required_payment_verification_stages", nullable = false)
    private Integer requiredPaymentVerificationStages = 1;

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
