package projectlx.co.zw.organizationmanagement.utils.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrganizationKycReviewDto {

    private Long id;
    private String stage;
    private String decision;
    private Long reviewerUserId;
    private String reviewerUsername;
    private String rejectionReason;
    private String notes;
    private int resubmissionCycle;
    private LocalDateTime reviewedAt;
}
