package projectlx.co.zw.organizationmanagement.repository.specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.organizationmanagement.model.Agent;
import projectlx.co.zw.organizationmanagement.model.AgentKind;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.ArrayList;

public final class AgentSpecifications {

    private AgentSpecifications() {
    }

    public static Specification<Agent> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED);
    }

    public static Specification<Agent> organizationIdEquals(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Agent> agentKindEquals(AgentKind agentKind) {
        return (root, query, cb) -> cb.equal(root.get("agentKind"), agentKind);
    }

    public static Specification<Agent> searchValueLike(String searchValue) {
        String pattern = "%" + searchValue.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            var orgJoin = root.join("organization", JoinType.LEFT);
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("firstName"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("lastName"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("email"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("role"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(orgJoin.get("name")), pattern));
            return cb.or(predicates.toArray(Predicate[]::new));
        };
    }
}
