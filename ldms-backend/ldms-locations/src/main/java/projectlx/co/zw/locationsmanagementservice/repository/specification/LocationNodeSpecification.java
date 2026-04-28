package projectlx.co.zw.locationsmanagementservice.repository.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public final class LocationNodeSpecification {

    private LocationNodeSpecification() {
    }

    public static Specification<LocationNode> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED);
    }

    public static Specification<LocationNode> hasLocationType(LocationType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("locationType"), type);
    }

    public static Specification<LocationNode> parentIdEquals(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("parent").get("id"), parentId);
    }

    public static Specification<LocationNode> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String term = "%" + name.trim().toUpperCase() + "%";
        return (root, query, cb) -> cb.like(cb.upper(root.get("name")), term);
    }

    public static Specification<LocationNode> codeContains(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String term = "%" + code.trim().toUpperCase() + "%";
        return (root, query, cb) -> cb.like(cb.upper(root.get("code")), term);
    }

    public static Specification<LocationNode> timezoneContains(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return null;
        }
        String term = "%" + timezone.trim().toUpperCase() + "%";
        return (root, query, cb) -> cb.like(cb.upper(root.get("timezone")), term);
    }

    public static Specification<LocationNode> parentNameContains(String parentName) {
        if (parentName == null || parentName.isBlank()) {
            return null;
        }
        String term = "%" + parentName.trim().toUpperCase() + "%";
        return (root, query, cb) -> {
            Join<LocationNode, LocationNode> parent = root.join("parent", JoinType.LEFT);
            return cb.like(cb.upper(parent.get("name")), term);
        };
    }

    public static Specification<LocationNode> searchValueMatches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String term = "%" + search.trim().toUpperCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.upper(root.get("name")), term),
                cb.like(cb.upper(root.get("code")), term),
                cb.like(cb.upper(root.get("timezone")), term)
        );
    }

    public static Specification<LocationNode> hasEntityStatus(EntityStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("entityStatus"), status);
    }
}
