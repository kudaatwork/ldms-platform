package projectlx.inventory.management.clients;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.util.List;

public interface FileUploadServiceClient {

    @PostMapping(value = "/ldms-file-upload-service/v1/system/file-upload/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse upload(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("fileUploadRequest") String fileUploadRequestJson);

    @PutMapping(value = "/ldms-file-upload-service/v1/system/file-upload/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse update(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("editFileUploadRequest") String editFileUploadRequestJson);

    @GetMapping("/ldms-file-upload-service/v1/system/file-upload/find-by-id/{id}")
    FileUploadResponse findById(@PathVariable("id") final Long id);
}
