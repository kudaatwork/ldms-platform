package projectlx.co.zw.fileuploadservice.service.rest.backend;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.fileuploadservice.service.processor.api.FileUploadProcessor;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/backoffice/file-upload")
@Tag(name = "File Upload Backend Resource", description = "Inter-service/backoffice operations for file uploads")
@RequiredArgsConstructor
public class FileUploadBackendResource {

    private static final String BACKEND_USER = "BACKEND";

    private final FileUploadProcessor fileUploadProcessor;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> upload(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("fileUploadRequest") String fileUploadRequestJson,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        FileUploadResponse response =
                fileUploadProcessor.upload(files, fileUploadRequestJson, locale, currentUsername(authorization));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> update(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("editFileUploadRequest") String editFileUploadRequestJson,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        FileUploadResponse response =
                fileUploadProcessor.update(files, editFileUploadRequestJson, locale, currentUsername(authorization));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<FileUploadResponse> findById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        FileUploadResponse response = fileUploadProcessor.findById(id, locale, currentUsername(authorization));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/find-by-original-file-name")
    public ResponseEntity<FileUploadResponse> findByOriginalFileName(
            @RequestParam String originalFileName,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        FileUploadResponse response =
                fileUploadProcessor.findByOriginalFileName(originalFileName, locale, currentUsername(authorization));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/find-by-owner")
    public ResponseEntity<FileUploadResponse> findByOwner(
            @RequestParam String ownerType,
            @RequestParam Long ownerId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        OwnerType ot = OwnerType.valueOf(ownerType.toUpperCase());
        FileUploadResponse response =
                fileUploadProcessor.findByOwnerTypeAndId(ot, ownerId, locale, currentUsername(authorization));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<FileUploadResponse> delete(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        FileUploadResponse response = fileUploadProcessor.delete(id, locale, currentUsername(authorization));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    private static String currentUsername(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return BACKEND_USER;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return BACKEND_USER;
        }
        return auth.getName();
    }
}
