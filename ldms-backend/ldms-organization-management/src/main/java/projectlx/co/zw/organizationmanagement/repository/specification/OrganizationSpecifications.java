package projectlx.co.zw.organizationmanagement.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
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

    private static final Set<KycStatus> DEFAULT_QUEUE_STATUSES = EnumSet.of(
            KycStatus.SUBMITTED,
            KycStatus.STAGE_1_REVIEW,
            KycStatus.STAGE_2_REVIEW,
            KycStatus.RESUBMITTED
    );

    public static Specification<Organization> kycQueue(String statusParam, OrganizationClassification classification) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED));
            if (statusParam != null && !statusParam.isBlank()) {
                predicates.add(cb.equal(root.get("kycStatus"), KycStatus.valueOf(statusParam.trim())));
            } else {
                predicates.add(root.get("kycStatus").in(DEFAULT_QUEUE_STATUSES));
            }
            if (classification != null) {
                predicates.add(cb.equal(root.get("organizationClassification"), classification));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
