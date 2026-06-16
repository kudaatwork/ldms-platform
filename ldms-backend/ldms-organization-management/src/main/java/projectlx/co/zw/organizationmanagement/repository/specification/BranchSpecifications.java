package projectlx.co.zw.organizationmanagement.repository.specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.utils.enums.BranchLevel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.ArrayList;

public final class BranchSpecifications {

    private BranchSpecifications() {
    }

    public static Specification<Branch> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED);
    }

    public static Specification<Branch> branchNameLike(String branchName) {
        String pattern = "%" + branchName.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("branchName")), pattern);
    }

    public static Specification<Branch> organizationIdEquals(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Branch> searchValueLike(String searchValue) {
        String pattern = "%" + searchValue.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            var orgJoin = root.join("organization", JoinType.LEFT);
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.like(cb.lower(root.get("branchName")), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("branchCode"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("email"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("region"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(orgJoin.get("name")), pattern));
            return cb.or(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Branch> branchLevelEquals(BranchLevel branchLevel) {
        return (root, query, cb) -> cb.equal(root.get("branchLevel"), branchLevel);
    }

    public static Specification<Branch> depotEquals(boolean depot) {
        return (root, query, cb) -> cb.equal(root.get("depot"), depot);
    }

    public static Specification<Branch> parentBranchIdEquals(Long parentBranchId) {
        return (root, query, cb) -> cb.equal(root.get("parentBranch").get("id"), parentBranchId);
    }

    public static Specification<Branch> regionLike(String region) {
        String pattern = "%" + region.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(cb.coalesce(root.get("region"), cb.literal(""))), pattern);
    }

    public static Specification<Branch> activeEquals(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
