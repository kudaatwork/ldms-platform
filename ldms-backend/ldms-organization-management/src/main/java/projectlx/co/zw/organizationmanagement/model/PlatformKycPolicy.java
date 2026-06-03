package projectlx.co.zw.organizationmanagement.model;

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
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_kyc_policy")
@Getter
@Setter
public class PlatformKycPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "default_required_approval_stages", nullable = false)
    private int defaultRequiredApprovalStages = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;
}
