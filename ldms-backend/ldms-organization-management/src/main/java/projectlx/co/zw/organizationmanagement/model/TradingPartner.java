package projectlx.co.zw.organizationmanagement.model;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.TradingPartnerRole;

import java.time.LocalDateTime;

/**
 * A trading partner record belonging to an organisation.
 *
 * <p>A partner can be:</p>
 * <ul>
 *   <li>A bare CRM entry ({@code recordOnly = true}) — name/email/phone captured locally, no LDMS account.</li>
 *   <li>A linked platform organisation ({@code recordOnly = false, linkedOrganizationId} set) — the
 *       counterparty is also registered on LDMS.</li>
 * </ul>
 */
@Entity
@Table(name = "organization_trading_partner", indexes = {
        @Index(name = "idx_otp_organization",  columnList = "organization_id"),
        @Index(name = "idx_otp_partner_role",  columnList = "partner_role"),
        @Index(name = "idx_otp_linked_org",    columnList = "linked_organization_id"),
        @Index(name = "idx_otp_entity_status", columnList = "entity_status")
})
@Getter
@Setter
public class TradingPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === OWNERSHIP ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // === PARTNER DETAILS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_role", nullable = false, length = 50)
    private TradingPartnerRole partnerRole;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // === PLATFORM LINK ===

    /** Platform organisation id when this partner is also registered on LDMS. */
    @Column(name = "linked_organization_id")
    private Long linkedOrganizationId;

    /** True when this is a CRM-only entry; false when linked to a platform org. */
    @Column(name = "record_only", nullable = false)
    private boolean recordOnly = true;

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
}
