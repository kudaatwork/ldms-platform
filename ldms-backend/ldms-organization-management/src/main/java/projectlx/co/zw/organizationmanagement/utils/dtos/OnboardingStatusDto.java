package projectlx.co.zw.organizationmanagement.utils.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Public onboarding tracker payload (limited fields for platform signup applicants).
 */
@Getter
@Setter
public class OnboardingStatusDto {

    private Long id;
    private String name;
    private String kycStatus;
    private boolean verified;
    private int requiredApprovalStages;
    private String lastRejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime modifiedAt;
}
