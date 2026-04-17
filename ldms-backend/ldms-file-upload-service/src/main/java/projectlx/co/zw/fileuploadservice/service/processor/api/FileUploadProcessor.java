package projectlx.co.zw.fileuploadservice.service.processor.api;

import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.util.List;
import java.util.Locale;

public interface FileUploadProcessor {

    FileUploadResponse upload(List<MultipartFile> files, String fileUploadRequestJson, Locale locale, String username);

    FileUploadResponse update(List<MultipartFile> files, String editFileUploadRequestJson, Locale locale, String username);

    FileUploadResponse findById(Long id, Locale locale, String username);

    FileUploadResponse findByOriginalFileName(String originalFileName, Locale locale, String username);

    FileUploadResponse findByOwnerTypeAndId(OwnerType ownerType, Long ownerId, Locale locale, String username);

    FileUploadResponse delete(Long id, Locale locale, String username);
}
