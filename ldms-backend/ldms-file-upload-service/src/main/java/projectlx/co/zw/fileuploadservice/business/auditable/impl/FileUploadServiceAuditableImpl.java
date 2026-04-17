package projectlx.co.zw.fileuploadservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.fileuploadservice.business.auditable.api.FileUploadServiceAuditable;
import projectlx.co.zw.fileuploadservice.model.FileUpload;
import projectlx.co.zw.fileuploadservice.repository.FileUploadRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.Locale;

@RequiredArgsConstructor
public class FileUploadServiceAuditableImpl implements FileUploadServiceAuditable {

    private final FileUploadRepository fileUploadRepository;

    @Override
    public FileUpload save(FileUpload fileUpload, Locale locale, String username) {
        if (fileUpload.getCreatedBy() == null || fileUpload.getCreatedBy().isBlank()) {
            fileUpload.setCreatedBy(username != null ? username : "SYSTEM");
        }
        return fileUploadRepository.save(fileUpload);
    }

    @Override
    public FileUpload delete(FileUpload fileUpload, Locale locale, String username) {
        fileUpload.setEntityStatus(EntityStatus.DELETED);
        fileUpload.setModifiedAt(LocalDateTime.now());
        fileUpload.setModifiedBy(username != null ? username : "SYSTEM");
        return fileUploadRepository.save(fileUpload);
    }
}
