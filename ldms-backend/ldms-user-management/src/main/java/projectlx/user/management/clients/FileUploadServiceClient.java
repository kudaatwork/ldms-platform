package projectlx.user.management.clients;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.util.List;

/**
 * Feign client for File Upload Service system API.
 * Base URL (host:port) is supplied by {@link projectlx.user.management.config.FileUploadServiceFeignConfiguration}.
 */
public interface FileUploadServiceClient {

    String FILE_UPLOAD_SYSTEM_API_BASE = "/ldms-file-upload-service/v1/system/file-upload";

    @PostMapping(value = FILE_UPLOAD_SYSTEM_API_BASE + "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse upload(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("fileUploadRequest") String fileUploadRequestJson);

    @PutMapping(value = FILE_UPLOAD_SYSTEM_API_BASE + "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse update(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("editFileUploadRequest") String editFileUploadRequestJson);
}
