package projectlx.fleet.management.clients;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.util.List;

public interface FileUploadServiceClient {

    String FILE_UPLOAD_SYSTEM_API_BASE = "/ldms-file-upload-service/v1/system/file-upload";

    @GetMapping(FILE_UPLOAD_SYSTEM_API_BASE + "/find-by-id/{id}")
    FileUploadResponse findById(@PathVariable("id") Long id);

    @PostMapping(value = FILE_UPLOAD_SYSTEM_API_BASE + "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse upload(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("fileUploadRequest") String fileUploadRequestJson);
}
