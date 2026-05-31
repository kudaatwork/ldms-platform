package projectlx.co.zw.organizationmanagement.business.kyc;

import projectlx.co.zw.organizationmanagement.model.KycReviewStage;
import projectlx.co.zw.organizationmanagement.model.KycStatus;
import projectlx.co.zw.organizationmanagement.model.Organization;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Maps numeric KYC stages (1–5) to domain enums and organisation assignment/review fields.
 */
public final class KycStageSupport {

    public static final int MIN_STAGE = 1;
    public static final int MAX_STAGE = 5;

    private KycStageSupport() {
    }

    public static KycReviewStage toReviewStage(int stage) {
        return switch (stage) {
            case 1 -> KycReviewStage.STAGE_1;
            case 2 -> KycReviewStage.STAGE_2;
            case 3 -> KycReviewStage.STAGE_3;
            case 4 -> KycReviewStage.STAGE_4;
            case 5 -> KycReviewStage.STAGE_5;
            default -> throw new IllegalArgumentException("Invalid KYC stage: " + stage);
        };
    }

    public static int toStageNumber(KycReviewStage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("stage is required");
        }
        return switch (stage) {
            case STAGE_1 -> 1;
            case STAGE_2 -> 2;
            case STAGE_3 -> 3;
            case STAGE_4 -> 4;
            case STAGE_5 -> 5;
        };
    }

    public static KycStatus reviewStatus(int stage) {
        return switch (stage) {
            case 1 -> KycStatus.STAGE_1_REVIEW;
            case 2 -> KycStatus.STAGE_2_REVIEW;
            case 3 -> KycStatus.STAGE_3_REVIEW;
            case 4 -> KycStatus.STAGE_4_REVIEW;
            case 5 -> KycStatus.STAGE_5_REVIEW;
            default -> throw new IllegalArgumentException("Invalid KYC stage: " + stage);
        };
    }

    public static int toStageNumber(KycStatus status) {
        if (status == null) {
            return 0;
        }
        return switch (status) {
            case STAGE_1_REVIEW -> 1;
            case STAGE_2_REVIEW -> 2;
            case STAGE_3_REVIEW -> 3;
            case STAGE_4_REVIEW -> 4;
            case STAGE_5_REVIEW -> 5;
            default -> 0;
        };
    }

    public static Set<KycStatus> workloadStatuses(int stage) {
        if (stage <= 1) {
            return EnumSet.of(
                    KycStatus.DRAFT,
                    KycStatus.SUBMITTED,
                    KycStatus.STAGE_1_REVIEW,
                    KycStatus.RESUBMITTED);
        }
        return EnumSet.of(reviewStatus(stage));
    }

    public static Long getAssignedApproverUserId(Organization org, int stage) {
        return switch (stage) {
            case 1 -> org.getAssignedStage1ApproverUserId();
            case 2 -> org.getAssignedStage2ApproverUserId();
            case 3 -> org.getAssignedStage3ApproverUserId();
            case 4 -> org.getAssignedStage4ApproverUserId();
            case 5 -> org.getAssignedStage5ApproverUserId();
            default -> null;
        };
    }

    public static String getAssignedApproverUsername(Organization org, int stage) {
        return switch (stage) {
            case 1 -> org.getAssignedStage1ApproverUsername();
            case 2 -> org.getAssignedStage2ApproverUsername();
            case 3 -> org.getAssignedStage3ApproverUsername();
            case 4 -> org.getAssignedStage4ApproverUsername();
            case 5 -> org.getAssignedStage5ApproverUsername();
            default -> null;
        };
    }

    public static void setAssignedApprover(Organization org, int stage, Long userId, String username) {
        switch (stage) {
            case 1 -> {
                org.setAssignedStage1ApproverUserId(userId);
                org.setAssignedStage1ApproverUsername(username);
            }
            case 2 -> {
                org.setAssignedStage2ApproverUserId(userId);
                org.setAssignedStage2ApproverUsername(username);
            }
            case 3 -> {
                org.setAssignedStage3ApproverUserId(userId);
                org.setAssignedStage3ApproverUsername(username);
            }
            case 4 -> {
                org.setAssignedStage4ApproverUserId(userId);
                org.setAssignedStage4ApproverUsername(username);
            }
            case 5 -> {
                org.setAssignedStage5ApproverUserId(userId);
                org.setAssignedStage5ApproverUsername(username);
            }
            default -> throw new IllegalArgumentException("Invalid KYC stage: " + stage);
        }
    }

    public static void clearAssignedApprover(Organization org, int stage) {
        setAssignedApprover(org, stage, null, null);
    }

    public static void clearAllAssignedApprovers(Organization org) {
        for (int stage = MIN_STAGE; stage <= MAX_STAGE; stage++) {
            clearAssignedApprover(org, stage);
        }
    }

    public static void setReviewed(Organization org, int stage, String reviewer, LocalDateTime reviewedAt) {
        switch (stage) {
            case 1 -> {
                org.setStage1ReviewedBy(reviewer);
                org.setStage1ReviewedAt(reviewedAt);
            }
            case 2 -> {
                org.setStage2ReviewedBy(reviewer);
                org.setStage2ReviewedAt(reviewedAt);
            }
            case 3 -> {
                org.setStage3ReviewedBy(reviewer);
                org.setStage3ReviewedAt(reviewedAt);
            }
            case 4 -> {
                org.setStage4ReviewedBy(reviewer);
                org.setStage4ReviewedAt(reviewedAt);
            }
            case 5 -> {
                org.setStage5ReviewedBy(reviewer);
                org.setStage5ReviewedAt(reviewedAt);
            }
            default -> throw new IllegalArgumentException("Invalid KYC stage: " + stage);
        }
    }
}
