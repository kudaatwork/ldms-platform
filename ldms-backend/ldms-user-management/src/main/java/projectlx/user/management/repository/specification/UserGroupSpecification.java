package projectlx.user.management.repository.specification;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.model.UserGroup_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserGroupSpecification {

    public static Specification<UserGroup> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(UserGroup_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<UserGroup> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserGroup_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<UserGroup> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserGroup_.description).as(String.class), description + "%");
            return p;
        };
    }

    public static Specification<UserGroup> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(UserGroup_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserGroup_.description), "%" + search.toUpperCase() + "%")
            );

            return p;
        };
    }

    public static Specification<UserGroup> organizationIdEquals(final Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    /**
     * Groups owned by the organisation workspace, plus platform-wide groups currently assigned to its users.
     */
    public static Specification<UserGroup> organizationWorkspaceVisible(final Long organizationId) {
        return (root, query, cb) -> {
            if (organizationId == null || organizationId <= 0) {
                return cb.conjunction();
            }
            var subquery = query.subquery(Long.class);
            var userRoot = subquery.from(projectlx.user.management.model.User.class);
            subquery.select(userRoot.get("userGroup").get("id"))
                    .where(
                            cb.equal(userRoot.get("organizationId"), organizationId),
                            cb.notEqual(userRoot.get("entityStatus"), EntityStatus.DELETED),
                            cb.isNotNull(userRoot.get("userGroup")));
            return cb.or(
                    cb.equal(root.get("organizationId"), organizationId),
                    cb.and(
                            cb.isNull(root.get("organizationId")),
                            root.get("id").in(subquery)));
        };
    }
}
