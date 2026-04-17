package projectlx.co.zw.fileuploadservice.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.fileuploadservice.business.logic.api.FileUploadService;
import projectlx.co.zw.fileuploadservice.service.processor.api.FileUploadProcessor;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FileUploadProcessorImpl implements FileUploadProcessor {

    private final FileUploadService fileUploadService;

    @Override
    public FileUploadResponse upload(
            List<MultipartFile> files, String fileUploadRequestJson, Locale locale, String username) {
        return fileUploadService.upload(files, fileUploadRequestJson, locale, username);
    }

    @Override
    public FileUploadResponse update(
            List<MultipartFile> files, String editFileUploadRequestJson, Locale locale, String username) {
        return fileUploadService.update(files, editFileUploadRequestJson, locale, username);
    }

    @Override
    public FileUploadResponse findById(Long id, Locale locale, String username) {
        return fileUploadService.findById(id, locale, username);
    }

    @Override
    public FileUploadResponse findByOriginalFileName(String originalFileName, Locale locale, String username) {
        return fileUploadService.findByOriginalFileName(originalFileName, locale, username);
    }

    @Override
    public FileUploadResponse findByOwnerTypeAndId(OwnerType ownerType, Long ownerId, Locale locale, String username) {
        return fileUploadService.findByOwnerTypeAndId(ownerType, ownerId, locale, username);
    }

    @Override
    public FileUploadResponse delete(Long id, Locale locale, String username) {
        return fileUploadService.delete(id, locale, username);
    }
}
