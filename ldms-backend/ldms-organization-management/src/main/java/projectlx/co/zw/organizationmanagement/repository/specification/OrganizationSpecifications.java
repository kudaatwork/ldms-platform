package projectlx.co.zw.organizationmanagement.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.organizationmanagement.model.KycStatus;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

public final class OrganizationSpecifications {

    private OrganizationSpecifications() {
    }

    /** In-review pipeline (excludes new signups still in {@link KycStatus#DRAFT}). */
    public static final Set<KycStatus> DEFAULT_QUEUE_STATUSES = EnumSet.of(
            KycStatus.SUBMITTED,
            KycStatus.STAGE_1_REVIEW,
            KycStatus.STAGE_2_REVIEW,
            KycStatus.STAGE_3_REVIEW,
            KycStatus.STAGE_4_REVIEW,
            KycStatus.STAGE_5_REVIEW,
            KycStatus.RESUBMITTED
    );

    /**
     * Full platform signup pipeline for the admin KYC applications table — includes organisations that
     * registered but have not submitted KYC yet ({@link KycStatus#DRAFT}) and rejected applications.
     */
    public static final Set<KycStatus> SIGNUP_PIPELINE_QUEUE_STATUSES = EnumSet.of(
            KycStatus.DRAFT,
            KycStatus.SUBMITTED,
            KycStatus.STAGE_1_REVIEW,
            KycStatus.STAGE_2_REVIEW,
            KycStatus.STAGE_3_REVIEW,
            KycStatus.STAGE_4_REVIEW,
            KycStatus.STAGE_5_REVIEW,
            KycStatus.RESUBMITTED,
            KycStatus.REJECTED
    );

    public static Specification<Organization> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED);
    }

    public static Specification<Organization> nameLike(String name) {
        String pattern = name.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Organization> emailLike(String email) {
        String pattern = email.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), pattern);
    }

    public static Specification<Organization> organizationClassificationEquals(OrganizationClassification classification) {
        return (root, query, cb) -> cb.equal(root.get("organizationClassification"), classification);
    }

    public static Specification<Organization> industryEquals(Industry industry) {
        return (root, query, cb) -> cb.equal(root.get("industry"), industry);
    }

    public static Specification<Organization> kycStatusEquals(KycStatus status) {
        return (root, query, cb) -> cb.equal(root.get("kycStatus"), status);
    }

    public static Specification<Organization> kycStatusIn(Set<KycStatus> statuses) {
        return (root, query, cb) -> root.get("kycStatus").in(statuses);
    }

    /**
     * Organisations eligible for the admin directory (all organisations / classification views):
     * admin-registered ({@code createdViaSignup=false}) or platform signup that completed KYC.
     */
    public static Specification<Organization> organizationDirectoryEligible() {
        return (root, query, cb) -> cb.or(
                cb.isFalse(root.get("createdViaSignup")),
                cb.and(
                        cb.isTrue(root.get("createdViaSignup")),
                        cb.equal(root.get("kycStatus"), KycStatus.APPROVED)));
    }

    /** Platform signup organisations only (KYC pipeline). */
    public static Specification<Organization> signupOrganizationsOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("createdViaSignup"));
    }

    public static Specification<Organization> searchValueLike(String searchValue) {
        String pattern = "%" + searchValue.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            predicates.add(cb.like(cb.lower(root.get("email")), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("registrationNumber"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("contactPersonFirstName"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("contactPersonLastName"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("contactPersonEmail"), cb.literal(""))), pattern));
            return cb.or(predicates.toArray(Predicate[]::new));
        };
    }

    public static final Set<KycStatus> STAGE1_REVIEWER_QUEUE_STATUSES = EnumSet.of(
            KycStatus.SUBMITTED,
            KycStatus.STAGE_1_REVIEW,
            KycStatus.RESUBMITTED);

    public static Specification<Organization> kycAssignedToReviewer(String username) {
        String normalized = username.trim().toLowerCase();
        return (root, query, cb) -> {
            var stage1Match = cb.and(
                    root.get("kycStatus").in(STAGE1_REVIEWER_QUEUE_STATUSES),
                    cb.equal(cb.lower(root.get("assignedStage1ApproverUsername")), normalized));
            var stage2Match = cb.and(
                    cb.equal(root.get("kycStatus"), KycStatus.STAGE_2_REVIEW),
                    cb.equal(cb.lower(root.get("assignedStage2ApproverUsername")), normalized));
            var stage3Match = cb.and(
                    cb.equal(root.get("kycStatus"), KycStatus.STAGE_3_REVIEW),
                    cb.equal(cb.lower(root.get("assignedStage3ApproverUsername")), normalized));
            var stage4Match = cb.and(
                    cb.equal(root.get("kycStatus"), KycStatus.STAGE_4_REVIEW),
                    cb.equal(cb.lower(root.get("assignedStage4ApproverUsername")), normalized));
            var stage5Match = cb.and(
                    cb.equal(root.get("kycStatus"), KycStatus.STAGE_5_REVIEW),
                    cb.equal(cb.lower(root.get("assignedStage5ApproverUsername")), normalized));
            return cb.or(stage1Match, stage2Match, stage3Match, stage4Match, stage5Match);
        };
    }

    public static Specification<Organization> kycQueue(String statusParam, OrganizationClassification classification) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED));
            predicates.add(cb.isTrue(root.get("createdViaSignup")));
            if (statusParam != null && !statusParam.isBlank() && !"ALL".equalsIgnoreCase(statusParam.trim())) {
                predicates.add(cb.equal(root.get("kycStatus"), KycStatus.valueOf(statusParam.trim())));
            } else if (statusParam == null || statusParam.isBlank()) {
                predicates.add(root.get("kycStatus").in(SIGNUP_PIPELINE_QUEUE_STATUSES));
            }
            if (classification != null) {
                predicates.add(cb.equal(root.get("organizationClassification"), classification));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
