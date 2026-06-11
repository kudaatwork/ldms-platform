package projectlx.fleet.management.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

public interface FileUploadServiceClient {

    String FILE_UPLOAD_SYSTEM_API_BASE = "/ldms-file-upload-service/v1/system/file-upload";

    @GetMapping(FILE_UPLOAD_SYSTEM_API_BASE + "/find-by-id/{id}")
    FileUploadResponse findById(@PathVariable("id") Long id);
}
