package projectlx.co.zw.fileuploadservice.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.fileuploadservice.model.FileUpload;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;

import java.util.List;

public final class FileUploadSpecification {

    private FileUploadSpecification() {
    }

    public static Specification<FileUpload> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED);
    }

    public static Specification<FileUpload> originalFileNameLike(final String originalFileName) {
        final String like = "%" + originalFileName.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("originalFileName")), like);
    }

    public static Specification<FileUpload> fileTypeLike(final String fileType) {
        final String like = "%" + fileType.trim().toUpperCase().replace(' ', '_') + "%";
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("fileType").as(String.class)), like);
    }

    public static Specification<FileUpload> entityStatusEquals(final String entityStatus) {
        final EntityStatus status = EntityStatus.valueOf(entityStatus.trim().toUpperCase());
        return (root, query, cb) -> cb.equal(root.get("entityStatus"), status);
    }

    public static Specification<FileUpload> organizationOwnerIdsIn(final List<Long> organizationOwnerIds) {
        return (root, query, cb) -> {
            Predicate orgOwners = cb.and(
                    cb.equal(root.get("ownerType"), OwnerType.ORGANIZATION),
                    root.get("ownerId").in(organizationOwnerIds));
            return orgOwners;
        };
    }

    public static Specification<FileUpload> any(final String search) {
        final String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("originalFileName")), like),
                cb.like(cb.upper(root.get("fileType").as(String.class)), "%" + search.trim().toUpperCase() + "%"),
                cb.like(root.get("ownerId").as(String.class), "%" + search.trim() + "%"));
    }
}
