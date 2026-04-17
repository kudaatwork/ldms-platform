package projectlx.user.management.service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import projectlx.user.management.service.utils.config.FeignConfig;
import java.util.List;

@FeignClient(name = "file-upload-service", url = "${clients.base-url.fileUploadService}", configuration = FeignConfig.class)
public interface FileUploadServiceClient {

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse upload(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("fileUploadRequest") String fileUploadRequestJson);

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse update(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("editFileUploadRequest") String editFileUploadRequestJson);
}
