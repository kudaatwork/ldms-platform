package projectlx.co.zw.fileuploadservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import projectlx.co.zw.fileuploadservice.model.FileUpload;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    Optional<FileUpload> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<FileUpload> findByOriginalFileNameAndEntityStatusNot(String originalFileName, EntityStatus entityStatus);

    List<FileUpload> findByOwnerTypeAndOwnerIdAndEntityStatusNot(
            OwnerType ownerType, Long ownerId, EntityStatus entityStatus);
}
