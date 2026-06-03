package projectlx.co.zw.fileuploadservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import projectlx.co.zw.fileuploadservice.model.FileUpload;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long>, JpaSpecificationExecutor<FileUpload> {

    Optional<FileUpload> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<FileUpload> findByOriginalFileNameAndEntityStatusNot(String originalFileName, EntityStatus entityStatus);

    @Query(
            "SELECT f FROM FileUpload f WHERE f.ownerType = :ownerType AND f.ownerId = :ownerId "
                    + "AND f.entityStatus <> :excludeStatus")
    List<FileUpload> findByOwnerTypeAndOwnerIdAndEntityStatusNot(
            @Param("ownerType") OwnerType ownerType,
            @Param("ownerId") Long ownerId,
            @Param("excludeStatus") EntityStatus entityStatus);

    Page<FileUpload> findByEntityStatusNotOrderByCreatedAtDesc(EntityStatus excludeStatus, Pageable pageable);
}
