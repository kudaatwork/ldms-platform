package projectlx.co.zw.shared_library.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.enums.StorageProvider;
import projectlx.co.zw.shared_library.utils.enums.VerificationMethod;
import projectlx.co.zw.shared_library.utils.enums.VerificationSource;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadDto {

    private Long id;
    private String originalFileName;
    private String storedFileName;
    private String fileUrl;
    private String contentType;
    private Long fileSizeInBytes;
    private StorageProvider storageProvider;
    private OwnerType ownerType;
    private Long ownerId;
    private boolean autoVerified;
    private String autoVerificationNotes;
    private LocalDateTime autoVerifiedAt;
    private FileType fileType;
    private VerificationMethod autoVerificationMethod;
    private VerificationSource autoVerificationSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private EntityStatus entityStatus;
    private String fileContent;
}
